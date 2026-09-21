package ml

object PredictionScores {
    /** This exported classifier returns probabilities, not arbitrary logits. */
    fun winner(probabilities: FloatArray): Int {
        require(probabilities.isNotEmpty()) { "Empty model output" }
        require(probabilities.all { it.isFinite() && it in 0f..1f }) { "Invalid model probabilities" }
        require(kotlin.math.abs(probabilities.sum() - 1f) <= 0.05f) { "Model output is not a probability distribution" }
        return probabilities.indices.maxByOrNull { probabilities[it] }!!
    }
}

