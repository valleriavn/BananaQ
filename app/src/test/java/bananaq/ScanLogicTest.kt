package bananaq

import bananaq.ml.ImagePreprocessor
import bananaq.ml.PredictionScores
import bananaq.ml.ScanImageLoader
import bananaq.model.ConfidenceLevel
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

    @Test fun thumbnailsBoundBothPortraitAndLandscapeImages() {
        assertEquals(32, ScanImageLoader.sampleSize(8000, 6000, 256))
        assertEquals(32, ScanImageLoader.sampleSize(6000, 8000, 256))
        assertEquals(1, ScanImageLoader.sampleSize(100, 200, 256))
    }

    @Test(expected = IllegalArgumentException::class)
    fun imageSamplingRejectsZeroDimensionLimit() {
        ScanImageLoader.sampleSize(8000, 6000, 0)
    }

    @Test fun fourClassOutputIncludesHealthyAndPanama() {
        assertEquals(2, PredictionScores.winner(floatArrayOf(.1f, .1f, .7f, .1f)))
        assertEquals(3, PredictionScores.winner(floatArrayOf(.1f, .1f, .1f, .7f)))
    }

    @Test fun acceptanceIncludesEveryClassAndLowConfidenceResults() {
        for (label in 0..3) {
            val scores = FloatArray(4) { 0.05f }
            scores[label] = 0.85f
            assertEquals(label, PredictionScores.winner(scores))
            assertTrue(PredictionScores.isAccepted(scores))
        }
        assertTrue(PredictionScores.isAccepted(floatArrayOf(.8f, .2f, 0f, 0f)))
        assertTrue(PredictionScores.isAccepted(floatArrayOf(.7999f, .2001f, 0f, 0f)))
        assertTrue(PredictionScores.isAccepted(floatArrayOf(.6f, .2f, .1f, .1f)))
        assertTrue(PredictionScores.isAccepted(floatArrayOf(.5f, .5f, 0f, 0f)))
        assertTrue(PredictionScores.isAccepted(floatArrayOf(.25f, .25f, .25f, .25f)))
        assertEquals(0, PredictionScores.winner(floatArrayOf(.25f, .25f, .25f, .25f)))
    }

    @Test fun previouslyRejectedPredictionReturnsHighestScoringClass() {
        val scores = floatArrayOf(.0203f, .1571f, .2230f, .5996f)
        assertEquals(3, PredictionScores.winner(scores))
        assertTrue(PredictionScores.isAccepted(scores))
    }

    @Test(expected = IllegalArgumentException::class)
    fun acceptanceRejectsWrongClassCount() {
        PredictionScores.isAccepted(floatArrayOf(1f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun acceptanceRejectsInvalidDistribution() {
        PredictionScores.isAccepted(floatArrayOf(.9f, .9f, 0f, 0f))
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
