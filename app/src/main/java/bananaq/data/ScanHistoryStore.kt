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

    fun items(): List<ScanHistoryAdapter.HistoryItem> = synchronized(lock) {
        val pattern = if (LocaleHelper.selectedLanguage(appContext) == "tl")
            "MMM dd, yyyy 'nang' h:mm a" else "MMM dd, yyyy 'at' h:mm a"
        val format = SimpleDateFormat(pattern, Locale.getDefault())
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
                records.map { record ->
                    val disease = record.predictedDisease ?: "Unknown"
                    val accuracyVal = record.confidenceScore?.let {
                        String.format(Locale.US, "%.2f", it * 100)
                    }
                    ScanHistoryAdapter.HistoryItem(
                        isHeader = false,
                        diseaseName = disease,
                        dateTime = format.format(Date(record.scannedAt)),
                        accuracy = accuracyVal,
                        confidenceScore = record.confidenceScore?.toFloat(),
                        isHealthy = record.isBananaLeaf && disease == "Healthy",
                        isValid = record.isBananaLeaf,
                        scanId = record.scanId,
                        imageUri = record.imagePath
                    )
                }
        }
    }

    companion object { private val lock = Any() }
}
