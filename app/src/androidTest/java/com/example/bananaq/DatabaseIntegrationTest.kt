package com.example.bananaq

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.bananaq.data.BananaQDatabase
import com.example.bananaq.data.FeedbackRecord
import com.example.bananaq.data.ScanRecord
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {
    private lateinit var context: Context
    private lateinit var database: BananaQDatabase

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        BananaQDatabase.resetForTests()
        context.deleteDatabase("bananaq.db")
        database = BananaQDatabase.get(context)
    }

    @After
    fun tearDown() {
        BananaQDatabase.resetForTests()
        context.deleteDatabase("bananaq.db")
    }

    @Test
    fun scanRetentionKeepsNewestTwoHundredAndQueuesIncrementalBatches() {
        val sessionId = database.startSession(1_000L)
        repeat(205) { index ->
            assertTrue(database.insertScan(ScanRecord(
                scanId = "00000000-0000-0000-0000-${index.toString().padStart(12, '0')}",
                sessionId = sessionId,
                imagePath = null,
                predictedDisease = "Healthy",
                confidenceScore = 0.9,
                isBananaLeaf = true,
                scannedAt = 2_000L + index
            )))
        }
        assertEquals(200, database.scans().size)
        val batch = database.pendingSyncBatch(limit = 25, now = Long.MAX_VALUE)
        assertFalse(batch.isEmpty)
        assertTrue(batch.scans.size <= 25)
        assertTrue(batch.sessions.any { it.sessionId == sessionId })
    }

    @Test
    fun feedbackIsUniquePerScan() {
        val sessionId = database.startSession(1_000L)
        val scanId = "10000000-0000-0000-0000-000000000000"
        database.insertScan(ScanRecord(
            scanId, sessionId, null, "Healthy", 0.9, true, 2_000L
        ))
        assertTrue(database.insertFeedback(FeedbackRecord(
            "20000000-0000-0000-0000-000000000000", scanId,
            "Accurate", "", 3_000L
        )))
        assertFalse(database.insertFeedback(FeedbackRecord(
            "30000000-0000-0000-0000-000000000000", scanId,
            "Inaccurate", "duplicate", 4_000L
        )))
        assertEquals("Accurate", database.feedbackForScan(scanId)?.accuracyRating)
    }

    @Test
    fun updateDuringSyncRemainsQueuedForTheNextBatch() {
        val sessionId = database.startSession(1_000L)
        val firstBatch = database.pendingSyncBatch(now = 1_500L)
        database.touchSession(sessionId, 2_000L)
        database.markSynced(firstBatch)

        val nextBatch = database.pendingSyncBatch(now = 2_500L)
        assertTrue(nextBatch.sessions.any {
            it.sessionId == sessionId && it.lastActiveAt == 2_000L
        })
    }
}
