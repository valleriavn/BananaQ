package com.example.bananaq.data.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.bananaq.BuildConfig
import com.example.bananaq.data.BananaQDatabase
import com.example.bananaq.data.PendingSyncBatch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Queues durable, authenticated, incremental synchronization with Supabase. */
object SupabaseSyncManager {
    private const val TAG = "SupabaseSync"
    private const val UNIQUE_WORK = "bananaq-supabase-sync"
    private const val STORAGE_BUCKET = "bananaq-scans"

    fun request(context: Context) {
        if (!isConfigured() || !hasSyncConsent(context)) return
        val work = OneTimeWorkRequest.Builder(SupabaseSyncWorker::class.java)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, work)
    }

    internal fun syncPending(context: Context): Boolean {
        if (!isConfigured() || !hasSyncConsent(context)) return true
        val auth = SupabaseAuthManager.getSession(context) ?: return false
        val database = BananaQDatabase.get(context)
        repeat(20) {
            val batch = database.pendingSyncBatch()
            if (batch.isEmpty) return true
            val remotePaths = mutableMapOf<String, String?>()
            for (scan in batch.scans) {
                val localFile = scan.imagePath?.let(::localImageFile)
                if (localFile == null) {
                    remotePaths[scan.scanId] = null
                    continue
                }
                val remotePath = "${auth.userId}/${scan.scanId}.jpg"
                if (!uploadImage(localFile, remotePath, auth.accessToken)) return false
                remotePaths[scan.scanId] = remotePath
            }
            if (!uploadBatch(batch, remotePaths, auth.accessToken)) return false
            database.markSynced(batch)
        }
        request(context)
        return true
    }

    internal fun isConfigured(): Boolean =
        BuildConfig.SUPABASE_URL.startsWith("https://") &&
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    private fun hasSyncConsent(context: Context): Boolean =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("onboarded", false)

    private fun uploadBatch(
        batch: PendingSyncBatch,
        remotePaths: Map<String, String?>,
        accessToken: String
    ): Boolean {
        val payload = JSONObject().apply {
            put("p_sessions", JSONArray().apply {
                batch.sessions.forEach { session -> put(JSONObject().apply {
                    put("session_id", session.sessionId)
                    put("started_at", timestamp(session.startedAt))
                    put("last_active_at", timestamp(session.lastActiveAt))
                    put("ended_at", session.endedAt?.let(::timestamp) ?: JSONObject.NULL)
                    put("is_active", session.isActive)
                }) }
            })
            put("p_scans", JSONArray().apply {
                batch.scans.forEach { scan -> put(JSONObject().apply {
                    put("scan_id", scan.scanId)
                    put("session_id", scan.sessionId)
                    put("image_path", remotePaths[scan.scanId] ?: JSONObject.NULL)
                    put("predicted_disease", scan.predictedDisease ?: JSONObject.NULL)
                    put("confidence_score", scan.confidenceScore ?: JSONObject.NULL)
                    put("is_banana_leaf", scan.isBananaLeaf)
                    put("scanned_at", timestamp(scan.scannedAt))
                }) }
            })
            put("p_feedback", JSONArray().apply {
                batch.feedback.forEach { entry -> put(JSONObject().apply {
                    put("feedback_id", entry.feedbackId)
                    put("scan_id", entry.scanId)
                    put("accuracy_rating", entry.accuracyRating)
                    put("comments", entry.comments)
                    put("submitted_at", timestamp(entry.submittedAt))
                }) }
            })
        }
        return postJson("/rest/v1/rpc/sync_bananaq_records", payload, accessToken)
    }

    private fun localImageFile(localUri: String): File? {
        val uri = Uri.parse(localUri)
        if (uri.scheme != "file") return null
        return uri.path?.let(::File)?.takeIf(File::isFile)
    }

    private fun uploadImage(file: File, remotePath: String, accessToken: String): Boolean {
        val connection = (URL(
            "${BuildConfig.SUPABASE_URL.trimEnd('/')}/storage/v1/object/$STORAGE_BUCKET/$remotePath"
        ).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setFixedLengthStreamingMode(file.length())
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "image/jpeg")
            setRequestProperty("x-upsert", "true")
        }
        return execute(connection) { output -> file.inputStream().use { it.copyTo(output) } }
    }

    private fun postJson(path: String, body: JSONObject, accessToken: String): Boolean {
        val bytes = body.toString().toByteArray(Charsets.UTF_8)
        val connection = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path)
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 20_000
            doOutput = true
            setFixedLengthStreamingMode(bytes.size)
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        return execute(connection) { it.write(bytes) }
    }

    private fun execute(
        connection: HttpURLConnection,
        write: (java.io.OutputStream) -> Unit
    ): Boolean = try {
        connection.outputStream.use(write)
        val status = connection.responseCode
        if (status in 200..299) true else {
            val message = connection.errorStream?.bufferedReader()?.use { it.readText() }
                ?.take(500).orEmpty()
            Log.w(TAG, "Supabase request failed ($status): $message")
            false
        }
    } catch (error: Exception) {
        Log.w(TAG, "Supabase sync is temporarily unavailable", error)
        false
    } finally {
        connection.disconnect()
    }

    private fun timestamp(value: Long): String = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US
    ).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(value))
}
