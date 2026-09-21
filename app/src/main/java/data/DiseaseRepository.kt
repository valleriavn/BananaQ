package data

import android.content.Context
import model.DiseaseInfo
import com.google.gson.Gson

class DiseaseRepository(
    private val context: Context
) {

    private val gson = Gson()

    private val diseaseFiles = mapOf(
        "Healthy" to "diseases/healthy.json",
        "Black Sigatoka" to "diseases/black_sigatoka.json",
        "Panama Disease" to "diseases/panama.json",
        "Cordana Leaf Spot" to "diseases/cordana.json"
    )

    fun getDiseaseInfo(
        diseaseName: String
    ): DiseaseInfo? {

        val fileName =
            diseaseFiles[diseaseName]
                ?: return null

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
