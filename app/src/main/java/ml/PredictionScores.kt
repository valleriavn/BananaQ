package ml

object PredictionScores {
    // Must match MIN_CONFIDENCE and MIN_MARGIN in BananaQ.ipynb.
    const val MIN_CONFIDENCE = 0.80f
    const val MIN_MARGIN = 0.20f

    fun isAccepted(probabilities: FloatArray): Boolean {
        require(probabilities.size == 4) { "Expected four disease probabilities" }
        val best = winner(probabilities)
        val runnerUp = probabilities.indices.filter { it != best }.maxOf { probabilities[it] }
        return probabilities[best] >= MIN_CONFIDENCE &&
            probabilities[best] - runnerUp >= MIN_MARGIN
    }

    /** This exported classifier returns probabilities, not arbitrary logits. */
    fun winner(probabilities: FloatArray): Int {
        require(probabilities.isNotEmpty()) { "Empty model output" }
        require(probabilities.all { it.isFinite() && it in 0f..1f }) { "Invalid model probabilities" }
        require(kotlin.math.abs(probabilities.sum() - 1f) <= 0.05f) { "Model output is not a probability distribution" }
        return probabilities.indices.maxByOrNull { probabilities[it] }!!
    }
}

