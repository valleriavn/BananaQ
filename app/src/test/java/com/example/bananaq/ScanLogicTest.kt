package com.example.bananaq

import ml.ImagePreprocessor
import ml.PredictionScores
import ml.ScanImageLoader
import model.ConfidenceLevel
import org.junit.Assert.*
import org.junit.Test

class ScanLogicTest {
    @Test fun confidenceRejectsInvalidNumbers() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 1.1f)) {
            assertEquals(ConfidenceLevel.VERY_LOW, ConfidenceLevel.fromConfidence(value))
        }
        assertEquals(ConfidenceLevel.LOW, ConfidenceLevel.fromConfidence(0.3f))
        assertEquals(ConfidenceLevel.MODERATE, ConfidenceLevel.fromConfidence(0.6f))
        assertEquals(ConfidenceLevel.HIGH, ConfidenceLevel.fromConfidence(0.8f))
        assertFalse(ConfidenceLevel.LOW.isReliable)
        assertTrue(ConfidenceLevel.MODERATE.isReliable)
        assertTrue(ConfidenceLevel.HIGH.isReliable)
    }

    @Test fun quantizationSaturatesInsteadOfWrapping() {
        assertEquals(255, ImagePreprocessor.quantize(1f, 0.001f, 0, false).toInt() and 255)
        assertEquals(0, ImagePreprocessor.quantize(0f, 0.01f, -10, false).toInt())
        assertEquals(-128, ImagePreprocessor.quantize(0f, 1f / 255, -128, true).toInt())
        assertEquals(127, ImagePreprocessor.quantize(1f, 1f / 255, -128, true).toInt())
    }

    @Test fun largeImagesAreSampledBeforeDecoding() {
        assertEquals(8, ScanImageLoader.sampleSize(8000, 6000))
        assertEquals(8, ScanImageLoader.sampleSize(6000, 8000))
        assertEquals(1, ScanImageLoader.sampleSize(224, 224))
    }

    @Test fun fourClassOutputIncludesHealthyAndPanama() {
        assertEquals(2, PredictionScores.winner(floatArrayOf(.1f, .1f, .7f, .1f)))
        assertEquals(3, PredictionScores.winner(floatArrayOf(.1f, .1f, .1f, .7f)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidOutputDoesNotBecomeConfidentDiagnosis() {
        PredictionScores.winner(floatArrayOf(Float.NaN, 0f, 0f, 0f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun logitsAreNotSilentlyClampedTo100Percent() {
        PredictionScores.winner(floatArrayOf(-1f, 2f, 5f, 1f))
    }
}
