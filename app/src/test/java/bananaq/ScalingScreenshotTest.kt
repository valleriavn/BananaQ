package bananaq

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import bananaq.data.DiseaseRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** JVM native-rendered screenshots; no emulator, camera, interpreter, or network is started. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScalingScreenshotTest {
    @Test fun normalPhone() = captureCase("phone", 360, 640, 420, 1f, "en")
    @Test fun enlargedPhone() = captureCase("phone_large", 360, 640, 420, 2f, "en")
    @Test fun compactFilipino() = captureCase("compact_tl", 320, 480, 320, 2f, "tl")
    @Test fun tallPhone() = captureCase("tall_phone", 411, 890, 420, 1.08f, "en")
    @Test fun tablet() = captureCase("tablet", 1024, 768, 160, 1f, "en")
    @Test fun landscape() = captureCase("landscape", 640, 360, 320, 1.5f, "en")

    private fun captureCase(name: String, widthDp: Int, heightDp: Int, dpi: Int, scale: Float, language: String) {
        RuntimeEnvironment.setQualifiers("$language-w${widthDp}dp-h${heightDp}dp-${dpi}dpi")
        RuntimeEnvironment.setFontScale(scale)
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_BananaQ)
        val density = context.resources.displayMetrics.density
        val width = (widthDp * density).toInt()
        val height = (heightDp * density).toInt()
        for ((layoutId, suffix) in listOf(R.layout.activity_language to "language",
                R.layout.activity_get_started to "welcome")) {
            val onboarding = LayoutInflater.from(context).inflate(layoutId, null)
            onboarding.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            onboarding.layout(0, 0, width, height)
            prepareText(onboarding)
            onboarding.viewTreeObserver.dispatchOnPreDraw()
            if (layoutId == R.layout.activity_language) {
                for (id in intArrayOf(R.id.labelEnglish, R.id.labelTagalog)) {
                    val label = onboarding.findViewById<View>(id)
                    val button = label.parent as View
                    assertEquals("Language label is off center", button.width / 2f,
                        (label.left + label.right) / 2f, 1f)
                }
            } else {
                val farmer = onboarding.findViewById<View>(R.id.bananaPlant)
                val content = farmer.parent as View
                assertTrue("Farmer overlaps the welcome panel",
                    farmer.top >= onboarding.findViewById<View>(R.id.curveBottom).bottom)
                assertTrue("Farmer is not anchored near the bottom",
                    content.height - farmer.bottom <= 13 * density)
            }
            snapshot(onboarding, "${name}_$suffix")
        }
        if (name in listOf("phone", "compact_tl", "tablet")) {
            for (layoutId in listOf(R.layout.activity_account, R.layout.activity_feedback_detail,
                    R.layout.activity_history, R.layout.activity_feedback, R.layout.activity_user_manual,
                    R.layout.activity_manual_topic, R.layout.activity_user_agreement, R.layout.activity_disease_details)) {
                val page = LayoutInflater.from(context).inflate(layoutId, null)
                ResponsiveLayoutTest().populate(page)
                fun arrange() {
                    page.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                    page.layout(0, 0, width, height)
                    prepareText(page)
                    page.viewTreeObserver.dispatchOnPreDraw()
                }
                arrange()
                val pageName = context.resources.getResourceEntryName(layoutId)
                snapshot(page, "${name}_$pageName")
                if (layoutId == R.layout.activity_user_agreement) {
                    page.findViewById<androidx.core.widget.NestedScrollView>(R.id.agreementScroll).fullScroll(View.FOCUS_DOWN)
                    arrange()
                    snapshot(page, "${name}_agreement_actions")
                }
                if (layoutId == R.layout.activity_feedback_detail) {
                    page.findViewById<View>(R.id.formScroll).visibility = View.GONE
                    page.findViewById<View>(R.id.successScroll).visibility = View.VISIBLE
                    page.findViewById<TextView>(R.id.summaryScan).text = "Cordana Leaf Spot"
                    page.findViewById<TextView>(R.id.summaryRating).text = "Very Accurate"
                    page.findViewById<TextView>(R.id.summaryConfidence).text = "99.50%"
                    page.findViewById<TextView>(R.id.summaryComments).text = "A longer saved feedback comment. ".repeat(12)
                    arrange()
                    snapshot(page, "${name}_feedback_saved")
                }
            }
        }
        val home = LayoutInflater.from(context).inflate(R.layout.activity_main, null)
        home.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.diseaseCarousel).apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context,
                androidx.recyclerview.widget.RecyclerView.HORIZONTAL, false)
            val library = DiseaseLibraryAdapter {}
            adapter = library
            (layoutManager as androidx.recyclerview.widget.LinearLayoutManager)
                .scrollToPositionWithOffset(library.initialPosition, 0)
        }
        home.findViewById<TextView>(R.id.dateText).text = "Wednesday, 2026"
        home.findViewById<TextView>(R.id.dayMonthText).text = "October 21"
        home.findViewById<TextView>(R.id.timeText).text = "04:14 PM"
        home.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        home.layout(0, 0, width, height)
        prepareText(home)
        home.viewTreeObserver.dispatchOnPreDraw()
        val header = home.findViewById<View>(R.id.header)
        if (scale == 1f) assertTrue("Home header is oversized", header.height <= 62 * density)
        for (id in intArrayOf(R.id.tvLangEN, R.id.tvLangTL)) {
            val control = home.findViewById<View>(id)
            assertTrue("Language touch target shrank", control.height >= 48 * density)
        }
        val homeContent = home.findViewById<View>(R.id.homeContent)
        val library = home.findViewById<View>(R.id.diseaseCarousel)
        assertTrue("Empty space remains below the library",
            homeContent.height - library.bottom <= 25 * density)
        snapshot(home, "${name}_home")
        val root = LayoutInflater.from(context).inflate(R.layout.activity_scanner, null)
        fun layout() {
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, width, height)
            prepareText(root)
            root.viewTreeObserver.dispatchOnPreDraw()
        }
        fun bounds(id: Int): Rect = root.findViewById<View>(id).let { Rect(it.left, it.top, it.right, it.bottom) }
        root.findViewById<View>(R.id.fullResultCard).visibility = View.GONE
        layout()
        val frame = bounds(R.id.scanFrame)
        val controls = bounds(R.id.controlsLayout)
        val anchor = bounds(R.id.resultCoordinator)
        snapshot(root, "${name}_camera")
        root.findViewById<TextView>(R.id.tvInstruction).setText(R.string.processing_photo)
        root.findViewById<View>(R.id.controlsLayout).alpha = 0.55f
        layout()
        assertEquals("Frame moved during inference", frame, bounds(R.id.scanFrame))
        assertEquals("Controls moved during inference", controls, bounds(R.id.controlsLayout))
        snapshot(root, "${name}_loading")
        root.findViewById<TextView>(R.id.tvInstruction).setText(R.string.scan_instruction)
        root.findViewById<View>(R.id.controlsLayout).alpha = 1f
        layout()

        root.findViewById<TextView>(R.id.predictionSummary).text = context.getString(
            R.string.prediction_summary, context.getString(R.string.disease_black_sigatoka), 90.0)
        root.findViewById<View>(R.id.compactResultPanel).visibility = View.VISIBLE
        root.findViewById<View>(R.id.scannerHeader).visibility = View.INVISIBLE
        root.findViewById<View>(R.id.scanResultTitle).visibility = View.VISIBLE
        for (id in intArrayOf(R.id.scanFrame, R.id.controlsLayout, R.id.instructionContainer)) {
            root.findViewById<View>(id).visibility = View.INVISIBLE
        }
        layout()
        assertEquals("Camera frame moved when the summary appeared", frame, bounds(R.id.scanFrame))
        assertEquals("Controls moved when the summary appeared", controls, bounds(R.id.controlsLayout))
        assertEquals("Details anchor moved", anchor, bounds(R.id.resultCoordinator))
        assertTrue("Result summary extends above the window", bounds(R.id.compactResultPanel).top >= 0)
        snapshot(root, "${name}_summary")

        root.findViewById<View>(R.id.compactResultPanel).visibility = View.GONE
        val card = root.findViewById<View>(R.id.fullResultCard)
        card.visibility = View.VISIBLE
        BottomSheetBehavior.from(card).state = BottomSheetBehavior.STATE_EXPANDED
        root.findViewById<View>(R.id.extraDetailsLayout).visibility = View.VISIBLE
        root.findViewById<TextView>(R.id.resultDiseaseName).setText(R.string.disease_black_sigatoka)
        root.findViewById<TextView>(R.id.resultAccuracyValue).text = "90.00%"
        val info = requireNotNull(DiseaseRepository(context).getDiseaseInfo("Black Sigatoka"))
        root.findViewById<TextView>(R.id.resultScientificName).text = info.scientificName
        root.findViewById<TextView>(R.id.resultSectionTitle).setText(R.string.visual_characteristics)
        val body = root.findViewById<LinearLayout>(R.id.resultContentContainer)
        info.symptoms.forEach { tip ->
            body.addView(TextView(context).apply {
                text = tip.title + "\n" + tip.description
                textSize = 15f
                setTextColor(context.getColor(R.color.banana_body))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            })
        }
        layout()
        assertEquals(anchor, bounds(R.id.resultCoordinator))
        val handle = root.findViewById<View>(R.id.resultDragHandle)
        val scroll = root.findViewById<View>(R.id.largeTextResultScroll)
        assertTrue("Handle is covered by scrolling content", handle.bottom <= scroll.top)
        assertTrue("Details have no scroll viewport", scroll.height > 0)
        for (id in intArrayOf(R.id.tabSymptoms, R.id.tabTreatment, R.id.tabPrevention)) {
            val tab = root.findViewById<TextView>(id)
            assertTrue("Empty result tab", tab.text.isNotBlank())
            assertTrue("Tab text has no layout", tab.layout != null)
        }
        snapshot(root, "${name}_details")
    }

    private fun prepareText(view: View) {
            // Detached fixtures have no ViewRoot to invoke TextView's pre-draw
            // callback (which positions single-line centered text horizontally).
            if (view is TextView) view.onPreDraw()
            if (view is ViewGroup) for (index in 0 until view.childCount) prepareText(view.getChildAt(index))
        }

    private fun snapshot(view: View, name: String) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            val output = File("build/reports/scaling/$name.png").apply { parentFile?.mkdirs() }
            output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val baseline = File("src/test/screenshots/$name.png")
            if (System.getProperty("bananaq.screenshots") == "record") {
                baseline.parentFile?.mkdirs()
                output.copyTo(baseline, overwrite = true)
            } else {
                assertTrue("Missing $baseline; review images and record with -Pscreenshots=record", baseline.isFile)
                val expected = ImageIO.read(baseline)
                val actual = ImageIO.read(output)
                assertEquals("Screenshot width changed", expected.width, actual.width)
                assertEquals("Screenshot height changed", expected.height, actual.height)
                var changed = 0
                for (y in 0 until actual.height) for (x in 0 until actual.width) {
                    if (expected.getRGB(x, y) != actual.getRGB(x, y)) changed++
                }
                assertEquals("Screenshot changed: $name; see $output", 0, changed)
            }
        } finally {
            bitmap.recycle()
        }
    }
}
