package com.example.bananaq.data

import android.content.Context
import com.example.bananaq.BananaQApplication
import com.example.bananaq.LocaleHelper
import com.example.bananaq.R
import com.example.bananaq.ScanHistoryAdapter
import com.example.bananaq.data.sync.SupabaseSyncManager
import com.example.bananaq.model.ClassificationResult
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
                        String.format(Locale.US, "%.1f", it * 100)
                    }
                    ScanHistoryAdapter.HistoryItem(
                        isHeader = false,
                        diseaseName = disease,
                        dateTime = format.format(Date(record.scannedAt)),
                        accuracy = accuracyVal,
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
