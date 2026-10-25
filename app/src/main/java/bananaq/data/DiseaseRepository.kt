package bananaq.data

import android.content.Context
import android.util.Log
import bananaq.model.DefaultPrevention
import bananaq.model.DiseaseInfo
import com.google.gson.Gson

/** Loads immutable, bundled content once per file for this repository's lifetime. */
class DiseaseRepository(private val context: Context) {
    private val gson = Gson()
    private val diseaseCache = mutableMapOf<String, DiseaseInfo>()
    private val preventionCache = mutableMapOf<String, DefaultPrevention>()

    @Synchronized
    fun getDefaultPrevention(language: String = currentLanguage()): DefaultPrevention {
        val path = "${folder(language)}/default_prevention.json"
        return preventionCache.getOrPut(path) { readAsset(path, DefaultPrevention::class.java) }
    }

    @Synchronized
    fun getDiseaseInfo(diseaseName: String, language: String = currentLanguage()): DiseaseInfo? {
        val baseName = diseaseFiles[diseaseName] ?: return null
        val path = "${folder(language)}/$baseName"
        diseaseCache[path]?.let { return it }
        return try {
            readAsset(path, DiseaseInfo::class.java).also { diseaseCache[path] = it }
        } catch (error: Exception) {
            Log.w(TAG, "Unable to load disease information for $diseaseName", error)
            null
        }
    }

    private fun <T> readAsset(path: String, type: Class<T>): T = context.assets.open(path)
        .bufferedReader(Charsets.UTF_8).use { gson.fromJson(it, type) }

    private fun currentLanguage() = context.resources.configuration.locales[0].language
    private fun folder(language: String) = if (language == "tl" || language == "fil") "diseases_tl" else "diseases"

    private companion object {
        const val TAG = "DiseaseRepository"
        val diseaseFiles = mapOf(
            "Healthy" to "healthy.json",
            "Black Sigatoka" to "black_sigatoka.json",
            "Panama Disease" to "panama.json",
            "Cordana Leaf Spot" to "cordana.json"
        )
    }
}
