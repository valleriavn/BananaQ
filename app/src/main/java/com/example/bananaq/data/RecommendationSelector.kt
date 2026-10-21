package com.example.bananaq.data

import com.example.bananaq.model.DiseaseInfo
import com.example.bananaq.model.DiseaseTip
import kotlin.random.Random

/** Session-only rotation, isolated by disease and section. Never mixes advice pools. */
class RecommendationSelector(private val random: Random = Random.Default) {
    private val queues = mutableMapOf<Pair<String, String>, MutableList<String>>()

    @Synchronized
    fun select(info: DiseaseInfo): DiseaseInfo = info.copy(
        symptoms = selectSection(info.diseaseName, "symptoms", info.symptoms),
        treatment = selectSection(info.diseaseName, "treatment", info.treatment),
        prevention = selectSection(info.diseaseName, "prevention", info.prevention)
    )

    private fun selectSection(disease: String, section: String, pool: List<DiseaseTip>): List<DiseaseTip> {
        require(pool.isNotEmpty()) { "Empty recommendation pool: $disease/$section" }
        require(pool.all { it.id.isNotBlank() } && pool.map { it.id }.distinct().size == pool.size) {
            "Recommendation IDs must be nonempty and unique: $disease/$section"
        }
        val unique = pool.distinctBy { it.title.trim() to it.description.trim() }
        val core = unique.filter { it.essential }
        val variable = unique.filterNot { it.essential }
        if (variable.isEmpty()) return core
        // Show up to three tips, but always retain every essential tip.
        // Leave at least one optional tip for a different combination next scan.
        val count = minOf(maxOf(1, 3 - core.size), maxOf(1, variable.size - 1))
        val ids = variable.map { it.id }
        val queue = queues.getOrPut(disease to section) { ids.shuffled(random).toMutableList() }
        queue.retainAll(ids.toSet())
        queue.addAll(ids.filterNot { it in queue }.shuffled(random))
        val selected = queue.take(count).toSet()
        repeat(count) { queue.add(queue.removeAt(0)) }
        // Keep the author's logical ordering; variety comes from membership, not reordering.
        return core + variable.filter { it.id in selected }
    }
}
