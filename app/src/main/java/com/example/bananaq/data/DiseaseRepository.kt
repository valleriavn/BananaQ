package com.example.bananaq.data

import android.content.Context
import android.util.Log
import com.example.bananaq.model.DiseaseInfo
import com.google.gson.Gson

class DiseaseRepository(
    private val context: Context
) {

    private companion object {
        const val TAG = "DiseaseRepository"
    }

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
        val activeLanguage = context.resources.configuration.locales[0].language
        val folder = if (activeLanguage == "tl" || activeLanguage == "fil")
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

        } catch (error: Exception) {
            Log.w(TAG, "Unable to load disease information for $diseaseName", error)
            null
        }
    }
}
