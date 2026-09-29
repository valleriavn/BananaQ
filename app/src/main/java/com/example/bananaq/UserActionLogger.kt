package com.example.bananaq

import android.util.Log

object UserActionLogger {

    private const val TAG = "UserAction"

    fun log(actionName: String, details: Map<String, Any> = emptyMap()) {
        if (!BuildConfig.DEBUG) return
        val detailsStr = if (details.isNotEmpty()) " | $details" else ""
        Log.d(TAG, "⚡ USER ACTION: [$actionName]$detailsStr")
    }

    fun logScanEvent(
        scanId: String,
        diseaseName: String?,
        confidenceScore: Float?,
        isValidLeaf: Boolean
    ) {
        val params = mapOf(
            "scan_id" to scanId,
            "disease_name" to (diseaseName ?: "None"),
            "confidence" to (confidenceScore ?: 0f),
            "is_valid_leaf" to isValidLeaf
        )
        log("scan_completed", params)
    }

    fun logFeedbackEvent(
        scanId: String,
        rating: String,
        comments: String
    ) {
        val params = mapOf(
            "scan_id" to scanId,
            "rating" to rating,
            "has_comments" to comments.isNotBlank()
        )
        log("feedback_submitted", params)
    }
}
