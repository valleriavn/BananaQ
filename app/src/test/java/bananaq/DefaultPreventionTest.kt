package bananaq

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import bananaq.data.DiseaseRepository
import bananaq.data.RecommendationProvider
import bananaq.data.RecommendationSelector
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = BananaQApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DefaultPreventionTest {
    private val diseases = listOf("Healthy", "Black Sigatoka", "Cordana Leaf Spot", "Panama Disease")

    @Test fun repositoryCachesEachLanguageWithoutMixingTranslations() {
        val repository = DiseaseRepository(RuntimeEnvironment.getApplication())
        val english = repository.getDefaultPrevention("en")
        val filipino = repository.getDefaultPrevention("tl")
        assertSame(english, repository.getDefaultPrevention("en"))
        assertSame(filipino, repository.getDefaultPrevention("fil"))
        assertEquals(english.practices.map { it.id to it.essential }, filipino.practices.map { it.id to it.essential })
        assertEquals(english.healthyPractice.id, filipino.healthyPractice.id)
        assertNotEquals(english.healthyPractice.description, filipino.healthyPractice.description)
        assertTrue(english.practices.zip(filipino.practices).all { (en, tl) ->
            en.title != tl.title && en.description != tl.description && tl.description.isNotBlank()
        })
        assertTrue(filipino.contact.contains("(043) 784-8022 local 307"))
        for (disease in diseases) {
            val en = requireNotNull(repository.getDiseaseInfo(disease, "en"))
            val tl = requireNotNull(repository.getDiseaseInfo(disease, "tl"))
            assertSame(en, repository.getDiseaseInfo(disease, "en"))
            assertSame(tl, repository.getDiseaseInfo(disease, "fil"))
            assertEquals(en.prevention.map { it.id }, tl.prevention.map { it.id })
            assertNotEquals(en.prevention.first().description, tl.prevention.first().description)
        }
    }

    @Test fun tabReopensReuseOnlyTheCurrentSavedSnapshot() {
        val context = RuntimeEnvironment.getApplication()
        val provider = RecommendationProvider(context)
        val first = requireNotNull(provider.forScan("Healthy", "cached-result-one"))
        assertSame(first, provider.forScan("Healthy", "cached-result-one", true))
        val next = requireNotNull(provider.forScan("Panama Disease", "cached-result-two"))
        assertSame(next, provider.forScan("Panama Disease", "cached-result-two", true))
        assertEquals(next, RecommendationProvider(context).forScan("Panama Disease", "cached-result-two", true))
    }

    @Test fun fixedEnglishPracticesAreAvailableOfflineForEveryDiseaseAndSavedScan() {
        val repository = DiseaseRepository(RuntimeEnvironment.getApplication())
        val defaults = repository.getDefaultPrevention()
        assertEquals(3, defaults.practices.size)
        assertTrue(defaults.practices.all { it.essential })
        assertTrue(defaults.verificationNote.contains("not confirmed RCPC-issued"))
        assertTrue(defaults.contact.contains("(043) 784-8022 local 307"))
        val selector = RecommendationSelector(Random(10))
        for (disease in diseases) {
            val pool = requireNotNull(repository.getDiseaseInfo(disease, "en"))
            assertTrue("Full prevention list was capped: $disease", defaults.prevention(pool).size > 5)
            repeat(8) {
                val selected = selector.select(pool)
                val rendered = defaults.prevention(selected)
                val fixed = defaults.fixedPracticesFor(selected)
                assertEquals(fixed, rendered.takeLast(fixed.size))
                assertTrue(rendered.containsAll(defaults.practices))
                if (disease == "Healthy") assertTrue(rendered.contains(defaults.healthyPractice))
                assertEquals(rendered.size, rendered.map { it.id }.distinct().size)
                assertTrue(defaults.diseaseSpecificTreatment(selected).none {
                    it.id == "panama.treatment.v2.4" || it.id == "panama.treatment.v2.5"
                })
            }
            val id = "default-practices-$disease"
            val saved = requireNotNull(RecommendationProvider(RuntimeEnvironment.getApplication()).forScan(disease, id))
            defaults.diseaseSpecificPrevention(saved)
            assertEquals(saved, RecommendationProvider(RuntimeEnvironment.getApplication()).forScan(disease, id, true))
        }
    }

    @Test fun actualResultTabsKeepAllPreventionOnCompactEnlargedAndFilipinoScreens() {
        for (language in listOf("en", "tl")) for (disease in diseases) {
            RuntimeEnvironment.setQualifiers("$language-w320dp-h480dp-320dpi")
            RuntimeEnvironment.setFontScale(2f)
            val app = RuntimeEnvironment.getApplication() as BananaQApplication
            LocaleHelper.saveLanguage(app, language)
            val intent = Intent(app, ScannerActivity::class.java).apply {
                putExtra("DISEASE_NAME", disease)
                putExtra("LIBRARY", true)
                putExtra("CONFIDENCE_SCORE", 0.9f)
            }
            val controller = Robolectric.buildActivity(ScannerActivity::class.java, intent).setup()
            try {
                val activity = controller.get()
                val defaults = DiseaseRepository(activity).getDefaultPrevention()
                activity.findViewById<View>(R.id.tabPrevention).performClick()
                val content = activity.findViewById<ViewGroup>(R.id.resultContentContainer)
                val texts = texts(content)
                val info = requireNotNull(DiseaseRepository(activity).getDiseaseInfo(disease))
                val fixed = defaults.fixedPracticesFor(info)
                val expected = defaults.prevention(info)
                val titles = fixed.map { it.title }
                assertEquals(titles, texts.filter { it in titles })
                assertFalse(texts.contains("Additional Recommended Practices"))
                assertEquals((1..expected.size).map { it.toString() }, texts.filter { it.matches(Regex("[0-9]+")) })
                assertEquals(fixed.last().description, texts.last())
                assertFalse(texts.contains(defaults.verificationNote))
                assertFalse(texts.contains(defaults.contact))
                assertEquals(expected.size, (0 until content.childCount).count { content.getChildAt(it).tag == "recommendation-item" })
                assertTrue("Prevention was capped", expected.size > 5)

                activity.findViewById<View>(R.id.fullResultCard).visibility = View.VISIBLE
                activity.findViewById<View>(R.id.extraDetailsLayout).visibility = View.VISIBLE
                val root = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
                root.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(960, View.MeasureSpec.EXACTLY))
                root.layout(0, 0, 640, 960)
                val scroll = activity.findViewById<NestedScrollView>(R.id.largeTextResultScroll)
                scroll.fullScroll(View.FOCUS_DOWN)
                assertTrue("Recommendations cannot scroll", scroll.getChildAt(0).height > scroll.height)
                assertFalse("Cannot reach the last practice", scroll.canScrollVertically(1))
                val lastItem = content.getChildAt(content.childCount - 1) as ViewGroup
                val description = (lastItem.getChildAt(1) as ViewGroup).getChildAt(1) as TextView
                assertTrue(description.layout.lineCount > 0)
                assertEquals("Last practice is clipped", 0, description.layout.getEllipsisCount(description.layout.lineCount - 1))

                activity.findViewById<View>(R.id.tabTreatment).performClick()
                assertTrue(texts(content).none { it in titles })
                if (disease == "Healthy") {
                    assertTrue(texts(content).contains(activity.getString(R.string.healthy_result_care_note)))
                    if (language == "tl") assertTrue(texts(content).first().startsWith("Hindi ipinapakita"))
                }
                activity.findViewById<View>(R.id.tabPrevention).performClick()
                assertEquals(titles, texts(content).filter { it in titles })
            } finally {
                controller.pause().stop().destroy()
            }
        }
    }

    private fun texts(view: View): List<String> = when (view) {
        is TextView -> listOf(view.text.toString())
        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
        else -> emptyList()
    }
}
