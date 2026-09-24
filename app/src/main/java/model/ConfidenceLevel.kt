package model

enum class ConfidenceLevel {

    VERY_LOW,
    LOW,
    MODERATE,
    HIGH;

    val isReliable: Boolean
        get() = this == HIGH

    companion object {

        const val MINIMUM_THRESHOLD = 0.30f
        const val MODERATE_THRESHOLD = 0.60f
        const val HIGH_THRESHOLD = 0.80f

        fun fromConfidence(
            confidence: Float
        ): ConfidenceLevel {

            return when {
                !confidence.isFinite() || confidence !in 0f..1f -> VERY_LOW
                confidence < MINIMUM_THRESHOLD ->
                    VERY_LOW

                confidence < MODERATE_THRESHOLD ->
                    LOW

                confidence < HIGH_THRESHOLD ->
                    MODERATE

                else ->
                    HIGH
            }
        }
    }
}
