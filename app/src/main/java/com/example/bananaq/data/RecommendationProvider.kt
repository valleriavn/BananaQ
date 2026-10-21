package com.example.bananaq.data

import android.content.Context
import android.util.Log
import com.example.bananaq.model.DiseaseInfo
import com.example.bananaq.model.DiseaseTip
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/** Local bilingual snapshots; no schema or Supabase changes are needed. */
class RecommendationProvider(private val context: Context) {
    private val repository = DiseaseRepository(context)
    private val preferences = context.applicationContext.getSharedPreferences("scan_recommendations_v1", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun forScan(disease: String, scanId: String, historical: Boolean = false): DiseaseInfo? = synchronized(lock) {
        require(scanId.isNotBlank())
        val key = "scan:$scanId"
        val saved = preferences.getString(key, null)?.let { json ->
            runCatching { gson.fromJson(json, Snapshot::class.java).takeIf { snapshot ->
                snapshot.english.diseaseName == disease && snapshot.tagalog.diseaseName == disease &&
                    listOf(snapshot.english, snapshot.tagalog).all { info ->
                        listOf(info.symptoms, info.treatment, info.prevention).all { tips ->
                            tips.isNotEmpty() && tips.all { it.id.isNotBlank() }
                        }
                    }
            } }
                .onFailure { Log.w("Recommendations", "Unable to read recommendation snapshot", it) }.getOrNull()
        }
        val snapshot = saved?.takeIf { it.english.diseaseName == disease } ?: run {
            val english = repository.getDiseaseInfo(disease, "en") ?: return@synchronized null
            val tagalog = repository.getDiseaseInfo(disease, "tl") ?: return@synchronized null
            // Legacy history showed the complete lists. Preserve that behavior on first open.
            val selected = if (historical) english else selector.select(english)
            val created = Snapshot(selected, translate(selected, tagalog))
            val editor = preferences.edit().putString(key, gson.toJson(created))
            // Preserve every retained history record, regardless of when it was opened.
            // Include the current result even if saving the photo/history failed.
            val retained = BananaQDatabase.get(context).scans().mapTo(hashSetOf()) { "scan:${it.scanId}" }
            retained.add(key)
            preferences.all.keys.filter { it.startsWith("scan:") && it !in retained }
                .forEach { editor.remove(it) }
            editor.apply()
            created
        }
        val language = context.resources.configuration.locales[0].language
        if (language == "tl" || language == "fil") snapshot.tagalog else snapshot.english
    }

    private fun translate(selection: DiseaseInfo, localized: DiseaseInfo): DiseaseInfo {
        fun section(selected: List<DiseaseTip>, pool: List<DiseaseTip>): List<DiseaseTip> {
            val byId = pool.associateBy { it.id }
            return selected.map { requireNotNull(byId[it.id]) { "Missing translated recommendation: ${it.id}" } }
        }
        return localized.copy(
            symptoms = section(selection.symptoms, localized.symptoms),
            treatment = section(selection.treatment, localized.treatment),
            prevention = section(selection.prevention, localized.prevention)
        )
    }

    private data class Snapshot(
        @SerializedName("english") val english: DiseaseInfo,
        @SerializedName("tagalog") val tagalog: DiseaseInfo
    )

    private companion object {
        val lock = Any()
        val selector = RecommendationSelector()
    }
}
