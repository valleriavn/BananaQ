package com.example.bananaq.data.sync

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.bananaq.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class SupabaseAuthSession(val accessToken: String, val userId: String)

/** Maintains a device-local anonymous Supabase Auth session without a login screen. */
internal object SupabaseAuthManager {
    private const val TAG = "SupabaseAuth"
    private const val PREFS = "supabase_auth"
    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"
    private const val EXPIRES_AT = "expires_at"
    private val lock = Any()

    fun getSession(context: Context): SupabaseAuthSession? = synchronized(lock) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val accessToken = preferences.getString(ACCESS_TOKEN, null)
        val expiresAt = preferences.getLong(EXPIRES_AT, 0L)
        if (!accessToken.isNullOrBlank() && expiresAt > System.currentTimeMillis() + 60_000L) {
            return@synchronized sessionFromToken(accessToken)
        }
        val refreshToken = preferences.getString(REFRESH_TOKEN, null)
        val response = if (!refreshToken.isNullOrBlank()) refresh(refreshToken) else null
        saveResponse(context, response ?: signInAnonymously())
    }

    private fun signInAnonymously(): JSONObject? = authRequest("/auth/v1/signup", JSONObject())

    private fun refresh(refreshToken: String): JSONObject? = authRequest(
        "/auth/v1/token?grant_type=refresh_token",
        JSONObject().put("refresh_token", refreshToken)
    )

    private fun authRequest(path: String, body: JSONObject): JSONObject? {
        val connection = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path)
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_PUBLISHABLE_KEY}")
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) {
                val message = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?.take(300).orEmpty()
                Log.w(TAG, "Anonymous authentication failed (${connection.responseCode}): $message")
                null
            } else {
                connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            }
        } catch (error: Exception) {
            Log.w(TAG, "Anonymous authentication is temporarily unavailable", error)
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun saveResponse(context: Context, response: JSONObject?): SupabaseAuthSession? {
        response ?: return null
        val accessToken = response.optString("access_token")
        val refreshToken = response.optString("refresh_token")
        if (accessToken.isBlank() || refreshToken.isBlank()) return null
        val expiresAtSeconds = response.optLong("expires_at").takeIf { it > 0L }
            ?: (System.currentTimeMillis() / 1000L + response.optLong("expires_in", 3600L))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ACCESS_TOKEN, accessToken)
            .putString(REFRESH_TOKEN, refreshToken)
            .putLong(EXPIRES_AT, expiresAtSeconds * 1000L)
            .commit()
        return sessionFromToken(accessToken)
    }

    private fun sessionFromToken(accessToken: String): SupabaseAuthSession? = runCatching {
        val payload = accessToken.split('.')[1]
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        SupabaseAuthSession(accessToken, JSONObject(String(decoded, Charsets.UTF_8)).getString("sub"))
    }.onFailure { Log.w(TAG, "Supabase returned an unreadable access token", it) }.getOrNull()
}
