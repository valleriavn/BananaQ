package data

import android.content.Context
import com.example.bananaq.ScanHistoryAdapter
import model.ClassificationResult
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanHistoryStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("scan_history", Context.MODE_PRIVATE)

    fun add(result: ClassificationResult, source: String, imageUri: String? = null) = synchronized(lock) {
        val previous = records()
        // Recreating the scanner may finish the same image again; do not duplicate it.
        if ((0 until previous.length()).any { previous.getJSONObject(it).optString("source") == source }) return@synchronized
        val next = JSONArray().put(JSONObject().apply {
            put("source", source)
            put("imageUri", imageUri)
            put("disease", result.diseaseName)
            put("confidence", result.confidence.toDouble())
            put("valid", result.isValid)
            put("time", System.currentTimeMillis())
        })
        for (i in 0 until minOf(previous.length(), 199)) next.put(previous.getJSONObject(i))
        check(prefs.edit().putString("records", next.toString()).commit()) { "Cannot save scan history" }
    }

    fun items(): List<ScanHistoryAdapter.HistoryItem> = synchronized(lock) {
        val records = records()
        val pattern = if (com.example.bananaq.LocaleHelper.selectedLanguage(appContext) == "tl")
            "MMM dd, yyyy 'nang' h:mm a" else "MMM dd, yyyy 'at' h:mm a"
        val format = SimpleDateFormat(pattern, Locale.getDefault())
        (0 until records.length()).map { index ->
            val record = records.getJSONObject(index)
            val disease = record.getString("disease")
            ScanHistoryAdapter.HistoryItem(false, diseaseName = disease,
                dateTime = format.format(Date(record.getLong("time"))),
                accuracy = String.format(Locale.US, "%.1f", record.getDouble("confidence") * 100),
                isHealthy = disease == "Healthy", isValid = record.optBoolean("valid", false),
                scanId = record.getString("source"), imageUri = record.optString("imageUri").takeIf { it.isNotBlank() })
        }
    }

    private fun records(): JSONArray = try {
        JSONArray(prefs.getString("records", "[]"))
    } catch (_: org.json.JSONException) { JSONArray() }

    companion object { private val lock = Any() }
}
