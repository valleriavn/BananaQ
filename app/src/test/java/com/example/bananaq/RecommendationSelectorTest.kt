package com.example.bananaq

import com.example.bananaq.data.RecommendationSelector
import com.example.bananaq.model.DiseaseInfo
import com.example.bananaq.model.DiseaseTip
import com.google.gson.Gson
import java.io.File
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class RecommendationSelectorTest {
    private val files = listOf("black_sigatoka", "panama", "cordana", "healthy")
    private fun load(name: String, folder: String = "diseases"): DiseaseInfo =
        Gson().fromJson(File("src/main/assets/$folder/$name.json").readText(), DiseaseInfo::class.java)
    private fun sections(info: DiseaseInfo) = listOf(info.symptoms, info.treatment, info.prevention)

    @Test fun everyDiseaseUsesOnlyItsOwnCategoryAndRetainsEveryEssentialTip() {
        val selector = RecommendationSelector(Random(7))
        repeat(12) {
            for (file in files) {
                val pool = load(file)
                val selected = selector.select(pool)
                assertEquals(pool.diseaseName, selected.diseaseName)
                sections(pool).zip(sections(selected)).forEach { (all, shown) ->
                    assertTrue(shown.isNotEmpty())
                    assertTrue(all.containsAll(shown))
                    assertTrue(shown.containsAll(all.filter { it.essential }))
                    assertEquals(shown.size, shown.map { it.id }.distinct().size)
                    assertEquals(shown.size, shown.map { it.title to it.description }.distinct().size)
                }
            }
        }
    }

    @Test fun consecutiveScansChangeMembershipWhereOptionalPoolAllowsIt() {
        val selector = RecommendationSelector(Random(12))
        for (file in files) {
            val pool = load(file)
            var previous = selector.select(pool)
            repeat(10) {
                val current = selector.select(pool)
                sections(pool).indices.forEach { section ->
                    val optional = sections(pool)[section].count { !it.essential }
                    if (optional > 1) {
                        assertNotEquals(sections(previous)[section].map { it.id }.toSet(),
                            sections(current)[section].map { it.id }.toSet())
                    }
                }
                previous = current
            }
        }
    }

    @Test fun switchingDiseasesDoesNotConsumeAnotherDiseasesRotation() {
        val uninterrupted = RecommendationSelector(Random(4))
        val interrupted = RecommendationSelector(Random(4))
        val black = load("black_sigatoka")
        assertEquals(uninterrupted.select(black), interrupted.select(black))
        interrupted.select(load("panama"))
        interrupted.select(load("cordana"))
        interrupted.select(load("healthy"))
        assertEquals(uninterrupted.select(black), interrupted.select(black))
    }

    @Test fun panamaContainmentAndFungicideCautionAreNeverHidden() {
        val pool = load("panama")
        val selector = RecommendationSelector(Random(5))
        val essentialIds = setOf("panama.treatment.v2.1", "panama.treatment.v2.7")
        repeat(12) {
            val selected = selector.select(pool).treatment
            assertTrue(selected.map { it.id }.containsAll(essentialIds))
            assertEquals(3, selected.size)
        }
    }

    @Test fun suppliedTreatmentAndPreventionCountsAreCompleteInBothLanguages() {
        val counts = mapOf("healthy" to (8 to 7), "black_sigatoka" to (7 to 6),
            "panama" to (8 to 8), "cordana" to (7 to 7))
        for (folder in listOf("diseases", "diseases_tl")) {
            var total = 0
            counts.forEach { (file, expected) ->
                val pool = load(file, folder)
                assertEquals(expected.first, pool.treatment.size)
                assertEquals(expected.second, pool.prevention.size)
                assertTrue(pool.treatment.all { it.id.startsWith("$file.treatment.v2.") })
                assertTrue(pool.prevention.all { it.id.startsWith("$file.prevention.v2.") })
                assertTrue(pool.symptoms.none { ".v2." in it.id })
                total += pool.treatment.size + pool.prevention.size
            }
            assertEquals(58, total)
        }
    }

    @Test fun allSixteenDiseaseCategoryLanguageCombinationsVaryWithoutLeaking() {
        for (folder in listOf("diseases", "diseases_tl")) {
            val selector = RecommendationSelector(Random(42))
            files.forEach { file ->
                val pool = load(file, folder)
                var previous = selector.select(pool)
                repeat(16) {
                    val current = selector.select(pool)
                    listOf(pool.treatment to current.treatment, pool.prevention to current.prevention)
                        .forEach { (all, shown) ->
                            assertEquals(3, shown.size)
                            assertTrue(all.containsAll(shown))
                            assertTrue(shown.containsAll(all.filter { it.essential }))
                            assertEquals(3, shown.map { it.id }.distinct().size)
                        }
                    assertNotEquals(previous.treatment.map { it.id }.toSet(), current.treatment.map { it.id }.toSet())
                    assertNotEquals(previous.prevention.map { it.id }.toSet(), current.prevention.map { it.id }.toSet())
                    previous = current
                }
            }
        }
    }

    @Test fun conditionalAdviceRetainsItsCautionInBothLanguages() {
        assertTrue(load("panama").treatment.single { it.id.endsWith(".v2.3") }
            .description.contains("where recommended"))
        assertTrue(load("panama", "diseases_tl").treatment.single { it.id.endsWith(".v2.3") }
            .description.contains("Kung inirerekomenda"))
        assertTrue(load("cordana").treatment.single { it.id.endsWith(".v2.6") }
            .description.contains("not be presented as an established treatment protocol"))
        assertTrue(load("cordana", "diseases_tl").treatment.single { it.id.endsWith(".v2.6") }
            .description.contains("hindi dapat ituring"))
        assertTrue(load("cordana").prevention.single { it.id.endsWith(".v2.7") }
            .description.contains("subject to further validation"))
        assertTrue(load("cordana", "diseases_tl").prevention.single { it.id.endsWith(".v2.7") }
            .description.contains("karagdagang pagpapatunay"))
    }

    @Test fun bothLanguagesHaveMatchingIdsCategoriesAndEssentialFlags() {
        files.forEach { file ->
            val english = load(file)
            val tagalog = load(file, "diseases_tl")
            assertEquals(english.diseaseName, tagalog.diseaseName)
            sections(english).zip(sections(tagalog)).forEach { (en, tl) ->
                assertTrue(en.isNotEmpty())
                assertEquals(en.map { it.id to it.essential }, tl.map { it.id to it.essential })
                assertTrue((en + tl).all { it.title.isNotBlank() && it.description.isNotBlank() })
            }
        }
    }

    @Test fun smallPoolsAndAllEssentialPoolsRemainUsable() {
        val tip = DiseaseTip("Title", "Description", "one")
        val info = DiseaseInfo("Test", "Test", listOf(tip), listOf(tip), listOf(tip))
        assertEquals(info, RecommendationSelector().select(info))
        val core = tip.copy(essential = true)
        val essentials = info.copy(symptoms = listOf(core), treatment = listOf(core), prevention = listOf(core))
        assertEquals(essentials, RecommendationSelector().select(essentials))
    }
}
