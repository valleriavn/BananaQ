package com.example.bananaq.data

data class UserSessionRecord(
    val sessionId: String,
    val startedAt: Long,
    val lastActiveAt: Long,
    val endedAt: Long?,
    val isActive: Boolean
)

data class ScanRecord(
    val scanId: String,
    val sessionId: String,
    val imagePath: String?,
    val predictedDisease: String?,
    val confidenceScore: Double?,
    val isBananaLeaf: Boolean,
    val scannedAt: Long
)

data class FeedbackRecord(
    val feedbackId: String,
    val scanId: String,
    val accuracyRating: String,
    val comments: String,
    val submittedAt: Long
)

data class PendingSyncBatch(
    val sessions: List<UserSessionRecord>,
    val scans: List<ScanRecord>,
    val feedback: List<FeedbackRecord>,
    val cutoffAt: Long
) {
    val isEmpty: Boolean
        get() = sessions.isEmpty() && scans.isEmpty() && feedback.isEmpty()
}
