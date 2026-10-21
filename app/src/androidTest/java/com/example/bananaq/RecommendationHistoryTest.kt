package com.example.bananaq

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.bananaq.data.DiseaseRepository
import com.example.bananaq.data.RecommendationProvider
import com.example.bananaq.model.DiseaseTip
import com.google.gson.Gson
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecommendationHistoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun localized(language: String): Context {
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        return context.createConfigurationContext(config)
    }

    @Test fun snapshotSurvivesProviderRecreationTabReopenAndLanguageChange() {
        val id = "test-recommendations-${java.util.UUID.randomUUID()}"
        try {
            val english = localized("en")
            val first = requireNotNull(RecommendationProvider(english).forScan("Black Sigatoka", id))
            assertEquals(first, RecommendationProvider(english).forScan("Black Sigatoka", id, historical = true))
            val tagalog = requireNotNull(RecommendationProvider(localized("tl")).forScan("Black Sigatoka", id, true))
            assertEquals(first.symptoms.map { it.id }, tagalog.symptoms.map { it.id })
            assertEquals(first.treatment.map { it.id }, tagalog.treatment.map { it.id })
            assertEquals(first.prevention.map { it.id }, tagalog.prevention.map { it.id })
            assertNotEquals(first.symptoms.first().title, tagalog.symptoms.first().title)
        } finally { remove(id) }
    }

    @Test fun legacyHistoryKeepsTheFullListsAndRemainsStable() {
        val id = "test-legacy-${java.util.UUID.randomUUID()}"
        try {
            val english = localized("en")
            val expected = DiseaseRepository(english).getDiseaseInfo("Panama Disease")
            val actual = RecommendationProvider(english).forScan("Panama Disease", id, true)
            assertEquals(expected, actual)
            assertEquals(actual, RecommendationProvider(english).forScan("Panama Disease", id))
        } finally { remove(id) }
    }

    @Test fun everyDiseaseDisplaysOnlyTheRequestedLanguagesCategoryText() {
        for (disease in listOf("Healthy", "Black Sigatoka", "Panama Disease", "Cordana Leaf Spot")) {
            val id = "test-bilingual-${java.util.UUID.randomUUID()}"
            try {
                val english = requireNotNull(RecommendationProvider(localized("en")).forScan(disease, id))
                val tagalog = requireNotNull(RecommendationProvider(localized("tl")).forScan(disease, id, true))
                for ((language, result) in listOf("en" to english, "tl" to tagalog)) {
                    val pool = requireNotNull(DiseaseRepository(localized(language)).getDiseaseInfo(disease))
                    assertEquals(disease, result.diseaseName)
                    assertEquals(3, result.treatment.size)
                    assertEquals(3, result.prevention.size)
                    assertTrue(pool.treatment.containsAll(result.treatment))
                    assertTrue(pool.prevention.containsAll(result.prevention))
                }
                assertEquals(english.treatment.map { it.id }, tagalog.treatment.map { it.id })
                assertEquals(english.prevention.map { it.id }, tagalog.prevention.map { it.id })
                assertNotEquals(english.treatment.map { it.description }, tagalog.treatment.map { it.description })
                assertNotEquals(english.prevention.map { it.description }, tagalog.prevention.map { it.description })
            } finally { remove(id) }
        }
    }

    @Test fun snapshotFromBeforePoolUpdateRetainsItsOriginalWordingAndIds() {
        val id = "test-old-pool-${java.util.UUID.randomUUID()}"
        try {
            val english = requireNotNull(DiseaseRepository(localized("en")).getDiseaseInfo("Panama Disease"))
                .copy(treatment = listOf(DiseaseTip("Do not disturb the suspected plant",
                    "Do not uproot, cut, or move the plant, suckers, or surrounding soil.", "panama.treatment.1", true)))
            val tagalog = requireNotNull(DiseaseRepository(localized("tl")).getDiseaseInfo("Panama Disease"))
                .copy(treatment = listOf(DiseaseTip("Huwag galawin ang hinihinalang halaman",
                    "Huwag bunutin, hiwain, o ilipat ang halaman, suhi, o nakapaligid na lupa.", "panama.treatment.1", true)))
            context.getSharedPreferences("scan_recommendations_v1", Context.MODE_PRIVATE).edit()
                .putString("scan:$id", Gson().toJson(mapOf("english" to english, "tagalog" to tagalog))).commit()
            assertEquals(english, RecommendationProvider(localized("en")).forScan("Panama Disease", id, true))
            assertEquals(tagalog, RecommendationProvider(localized("tl")).forScan("Panama Disease", id, true))
        } finally { remove(id) }
    }

    private fun remove(id: String) {
        context.getSharedPreferences("scan_recommendations_v1", Context.MODE_PRIVATE)
            .edit().remove("scan:$id").commit()
    }
}
