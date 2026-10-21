package bananaq

import android.content.res.Configuration
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScalingConfigurationTest {
    @Test fun scannerAnchorsStayStableWhenResultSummaryAppears() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (densityDpi in listOf(320, 420, 480)) {
                for (scale in listOf(1f, 1.5f, 2f)) {
                    val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                        fontScale = scale
                        this.densityDpi = densityDpi
                        screenWidthDp = 360
                        screenHeightDp = 640
                        smallestScreenWidthDp = 360
                        orientation = Configuration.ORIENTATION_PORTRAIT
                    }
                    val context = ContextThemeWrapper(
                        instrumentation.targetContext.createConfigurationContext(config), R.style.Theme_BananaQ)
                    val root = LayoutInflater.from(context).inflate(R.layout.activity_scanner, null)
                    val density = context.resources.displayMetrics.density
                    val width = (360 * density).toInt()
                    val height = (640 * density).toInt()
                    fun layout() {
                        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                        root.layout(0, 0, width, height)
                    }
                    fun bounds(id: Int): android.graphics.Rect {
                        val view = root.findViewById<View>(id)
                        return android.graphics.Rect(view.left, view.top, view.right, view.bottom)
                    }
                    root.findViewById<View>(R.id.fullResultCard).visibility = View.GONE
                    layout()
                    val frame = bounds(R.id.scanFrame)
                    val controls = bounds(R.id.controlsLayout)
                    val card = bounds(R.id.resultCoordinator)
                    root.findViewById<TextView>(R.id.predictionSummary).text =
                        "Black Sigatoka: 90.00%\n${context.getString(R.string.tap_for_details)}"
                    root.findViewById<View>(R.id.compactResultPanel).visibility = View.VISIBLE
                    root.findViewById<View>(R.id.scannerHeader).visibility = View.INVISIBLE
                    root.findViewById<View>(R.id.scanResultTitle).visibility = View.VISIBLE
                    layout()
                    val label = "density=$densityDpi font=$scale"
                    assertEquals("Camera frame moved: $label", frame, bounds(R.id.scanFrame))
                    assertEquals("Camera controls moved: $label", controls, bounds(R.id.controlsLayout))
                    assertEquals("Details anchor moved: $label", card, bounds(R.id.resultCoordinator))
                    assertTrue("Preview area lost: $label", card.top >= height * 0.29f)
                    assertTrue("Header overlaps details: $label",
                        root.findViewById<View>(R.id.scanResultTitle).bottom <= card.top)
                }
            }
        }
    }

    @Test fun pageTextFitsAtCompactLandscapeAndTabletSizes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val layouts = listOf(R.layout.activity_main, R.layout.activity_language,
            R.layout.activity_get_started, R.layout.activity_user_agreement,
            R.layout.activity_history, R.layout.activity_feedback, R.layout.activity_feedback_detail,
            R.layout.activity_account, R.layout.activity_disease_details,
            R.layout.activity_user_manual, R.layout.activity_manual_topic, R.layout.activity_scanner)
        instrumentation.runOnMainSync {
            for ((width, height) in listOf(320 to 480, 640 to 360, 600 to 960)) {
                for (scale in listOf(1f, 2f)) {
                    for (language in listOf("en", "tl")) {
                        val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                            fontScale = scale
                            densityDpi = 320
                            screenWidthDp = width
                            screenHeightDp = height
                            smallestScreenWidthDp = minOf(width, height)
                            orientation = if (width > height) Configuration.ORIENTATION_LANDSCAPE
                                else Configuration.ORIENTATION_PORTRAIT
                            setLocale(java.util.Locale(language))
                        }
                        val context = ContextThemeWrapper(
                            instrumentation.targetContext.createConfigurationContext(config), R.style.Theme_BananaQ)
                        for (layout in layouts) {
                            val root = LayoutInflater.from(context).inflate(layout, null)
                            val density = context.resources.displayMetrics.density
                            val w = (width * density).toInt()
                            val h = (height * density).toInt()
                            root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                            root.layout(0, 0, w, h)
                            val label = "${context.resources.getResourceEntryName(layout)} $width x $height font=$scale $language"
                            assertTextFits(root, label)
                        }
                    }
                }
            }
        }
    }

    private fun assertTextFits(view: View, screen: String) {
        if (view.visibility != View.VISIBLE || view is HorizontalScrollView) return
        if (view is TextView && view.text.isNotEmpty()) {
            assertTrue("$screen: zero-width text ${view.text}", view.width > 0)
            view.layout?.let { textLayout ->
                assertTrue("$screen: vertically clipped text ${view.text}",
                    view.height >= textLayout.height + view.compoundPaddingTop + view.compoundPaddingBottom)
            }
            val parent = view.parent as? ViewGroup
            if (parent != null) {
                assertTrue("$screen: text exceeds its container ${view.text}",
                    view.left >= 0 && view.right <= parent.width)
            }
        }
        if (view is ViewGroup) for (index in 0 until view.childCount) assertTextFits(view.getChildAt(index), screen)
    }

    @Test fun languageChoicesFitLargeTextInCompactWindows() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (scale in listOf(1f, 1.5f, 2f)) {
                val configuration = Configuration(instrumentation.targetContext.resources.configuration).apply {
                    fontScale = scale
                    densityDpi = 320
                    screenWidthDp = 320
                    screenHeightDp = 480
                    smallestScreenWidthDp = 320
                }
                val context = ContextThemeWrapper(
                    instrumentation.targetContext.createConfigurationContext(configuration), R.style.Theme_BananaQ)
                val view = LayoutInflater.from(context).inflate(R.layout.activity_language, null)
                fun px(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    dp, context.resources.displayMetrics).toInt()
                view.measure(View.MeasureSpec.makeMeasureSpec(px(320f), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(px(480f), View.MeasureSpec.EXACTLY))
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                for (id in listOf(R.id.subTitleText, R.id.instructionText, R.id.labelEnglish, R.id.labelTagalog)) {
                    val label = view.findViewById<TextView>(id)
                    assertTrue("Text clipped at font scale $scale", label.height >=
                        label.layout.height + label.compoundPaddingTop + label.compoundPaddingBottom)
                    for (line in 0 until label.lineCount) {
                        assertEquals(0, label.layout.getEllipsisCount(line))
                    }
                }
                val english = view.findViewById<View>(R.id.btnEnglish)
                val tagalog = view.findViewById<View>(R.id.btnTagalog)
                assertTrue("Language choices overlap", english.bottom <= tagalog.top)
                assertTrue(english.height >= px(48f))
                assertTrue(tagalog.height >= px(48f))
            }
        }
    }

    @Test fun changingLocalePreservesAccessibilityAndWindowConfiguration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (scale in listOf(0.85f, 1f, 1.5f, 2f)) {
            for (density in listOf(160, 320, 480)) {
                val configuration = Configuration(context.resources.configuration).apply {
                    fontScale = scale
                    densityDpi = density
                    screenWidthDp = 320
                    screenHeightDp = 480
                }
                val source = context.createConfigurationContext(configuration)
                val wrapped = LocaleHelper.wrap(source).resources.configuration
                assertEquals(scale, wrapped.fontScale, 0f)
                assertEquals(density, wrapped.densityDpi)
                assertEquals(320, wrapped.screenWidthDp)
                assertEquals(480, wrapped.screenHeightDp)
                assertEquals(LocaleHelper.selectedLanguage(source), wrapped.locales[0].language)
            }
        }
    }
}
