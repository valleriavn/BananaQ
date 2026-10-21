package com.example.bananaq

import com.example.bananaq.ml.PredictionScores
import com.example.bananaq.model.ConfidenceLevel
import org.junit.Assert.*
import org.junit.Test

class ClassificationRulesTest {
    @Test fun everyExportedClassCanWin() {
        for (index in 0..3) {
            val probabilities = FloatArray(4) { 0.05f }.apply { this[index] = 0.85f }
            assertEquals(index, PredictionScores.winner(probabilities))
            assertTrue(PredictionScores.isAccepted(probabilities))
        }
    }
    @Test fun invalidConfidenceIsNeverHigh() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 2f)) {
            assertEquals(ConfidenceLevel.VERY_LOW, ConfidenceLevel.fromConfidence(value))
        }
    }
    @Test(expected = IllegalArgumentException::class)
    fun malformedModelOutputIsRejected() {
        PredictionScores.winner(floatArrayOf(0.9f, 0.9f, 0f, 0f))
    }
}
