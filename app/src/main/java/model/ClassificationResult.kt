package model

data class ClassificationResult(
    val diseaseName: String,
    val confidence: Float,
    val confidenceLevel: ConfidenceLevel,
    val isValid: Boolean
)