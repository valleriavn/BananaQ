package bananaq.data

import android.content.Context
import bananaq.BananaQApplication
import bananaq.LocaleHelper
import bananaq.R
import bananaq.ScanHistoryAdapter
import bananaq.data.sync.SupabaseSyncManager
import bananaq.model.ClassificationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanHistoryStore(context: Context) {
    private val appContext = context.applicationContext
    private val database = BananaQDatabase.get(appContext)

    fun add(result: ClassificationResult, source: String, imageUri: String? = null): Boolean = synchronized(lock) {
        val application = appContext as BananaQApplication
        val sessionId = application.currentSessionId()
        val isValid = result.isValid

        val inserted = database.insertScan(ScanRecord(
            scanId = source,
            sessionId = sessionId,
            imagePath = imageUri,
            predictedDisease = result.diseaseName.takeIf { isValid },
            confidenceScore = result.confidence.toDouble().takeIf { isValid },
            isBananaLeaf = isValid,
            scannedAt = System.currentTimeMillis()
        ))
        if (inserted) SupabaseSyncManager.request(appContext)
        inserted
    }

    fun itemForScan(scanId: String): ScanHistoryAdapter.HistoryItem? =
        database.scan(scanId)?.let { it.toHistoryItem(dateFormat()) }

    private fun dateFormat(): SimpleDateFormat {
        val pattern = if (LocaleHelper.selectedLanguage(appContext) == "tl")
            "MMM dd, yyyy 'nang' h:mm a" else "MMM dd, yyyy 'at' h:mm a"
        return SimpleDateFormat(pattern, Locale.getDefault())
    }

    fun items(): List<ScanHistoryAdapter.HistoryItem> = synchronized(lock) {
        val format = dateFormat()
        val localizedContext = LocaleHelper.wrap(appContext)
        ScanDateGrouping.group(database.scans()).flatMap { (period, records) ->
            val title = localizedContext.getString(when (period) {
                ScanDateGrouping.Period.TODAY -> R.string.history_group_today
                ScanDateGrouping.Period.YESTERDAY -> R.string.history_group_yesterday
                ScanDateGrouping.Period.THIS_WEEK -> R.string.history_group_this_week
                ScanDateGrouping.Period.LAST_WEEK -> R.string.history_group_last_week
                ScanDateGrouping.Period.OLDER -> R.string.history_group_older
            })
            listOf(ScanHistoryAdapter.HistoryItem(isHeader = true, headerTitle = title)) +
                records.map { it.toHistoryItem(format) }
        }
    }

    private fun ScanRecord.toHistoryItem(format: SimpleDateFormat): ScanHistoryAdapter.HistoryItem {
        val disease = predictedDisease ?: "Unknown"
        return ScanHistoryAdapter.HistoryItem(
            isHeader = false,
            diseaseName = disease,
            dateTime = format.format(Date(scannedAt)),
            accuracy = confidenceScore?.let { String.format(Locale.US, "%.2f", it * 100) },
            confidenceScore = confidenceScore?.toFloat(),
            isHealthy = isBananaLeaf && disease == "Healthy",
            isValid = isBananaLeaf,
            scanId = scanId,
            imageUri = imagePath
        )
    }

    companion object { private val lock = Any() }
}
