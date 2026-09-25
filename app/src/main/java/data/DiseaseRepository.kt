package data

import android.content.Context
import model.DiseaseInfo
import com.google.gson.Gson

class DiseaseRepository(
    private val context: Context
) {

    private val gson = Gson()

    private val diseaseFiles = mapOf(
        "Healthy" to "healthy.json",
        "Black Sigatoka" to "black_sigatoka.json",
        "Panama Disease" to "panama.json",
        "Cordana Leaf Spot" to "cordana.json"
    )

    fun getDiseaseInfo(
        diseaseName: String
    ): DiseaseInfo? {

        val baseName =
            diseaseFiles[diseaseName]
                ?: return null
        val folder = if (com.example.bananaq.LocaleHelper.selectedLanguage(context) == "tl")
            "diseases_tl" else "diseases"
        val fileName = "$folder/$baseName"

        return try {

            val json = context.assets
                .open(fileName)
                .bufferedReader()
                .use { it.readText() }

            gson.fromJson(
                json,
                DiseaseInfo::class.java
            )

        } catch (e: Exception) {

            e.printStackTrace()
            null
        }
    }
}
