package com.example.bananaq

import android.app.Application
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.widget.NestedScrollView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ResponsiveLayoutTest {
    private val pages = listOf(R.layout.activity_main, R.layout.activity_language,
        R.layout.activity_get_started, R.layout.activity_user_agreement,
        R.layout.activity_history, R.layout.activity_feedback, R.layout.activity_feedback_detail,
        R.layout.activity_account, R.layout.activity_disease_details,
        R.layout.activity_user_manual, R.layout.activity_manual_topic)

    @Test fun populatedPagesFitAcrossWindowDensityLanguageAndFontChanges() {
        for ((width, height) in listOf(320 to 480, 360 to 800, 411 to 890,
                640 to 360, 600 to 960, 1024 to 768)) {
            for (dpi in listOf(160, 320, 480)) for (font in listOf(1f, 2f)) for (language in listOf("en", "tl")) {
                RuntimeEnvironment.setQualifiers("$language-w${width}dp-h${height}dp-${dpi}dpi")
                RuntimeEnvironment.setFontScale(font)
                val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_BananaQ)
                val density = context.resources.displayMetrics.density
                assertEquals("Header and page content use different horizontal grids",
                    context.resources.getDimensionPixelSize(R.dimen.page_horizontal_padding),
                    context.resources.getDimensionPixelSize(R.dimen.header_horizontal_padding))
                for (page in pages) {
                    val root = LayoutInflater.from(context).inflate(page, null)
                    populate(root)
                    measure(root, (width * density).toInt(), (height * density).toInt())
                    val label = "${context.resources.getResourceEntryName(page)} ${width}x$height dpi=$dpi font=$font $language"
                    assertText(root, label)
                    assertBrandHeader(root, label)
                    assertScrollViewports(root, label)
                    if (page == R.layout.activity_account) {
                        val heading = root.findViewById<TextView>(R.id.titleAccount)
                        val scroll = root.findViewById<ViewGroup>(R.id.accountContent)
                        val column = scroll.getChildAt(0)
                        assertEquals("Account title is not aligned with its body: $label",
                            heading.left + heading.compoundPaddingLeft,
                            scroll.left + column.left + column.paddingLeft)
                    }
                    if (page == R.layout.activity_feedback_detail) {
                        for (ids in listOf(listOf(R.id.rateVeryAccurate, R.id.rateAccurate),
                                listOf(R.id.rateNotSure, R.id.rateInaccurate, R.id.rateVeryInaccurate))) {
                            val iconTops = ids.map { id ->
                                val option = root.findViewById<ViewGroup>(id)
                                option.top + option.getChildAt(0).top
                            }
                            assertEquals("Rating icons are not aligned: $label", 1, iconTops.distinct().size)
                        }
                        val metadata = root.findViewById<View>(R.id.resultMetaLayout)
                        val photo = root.findViewById<View>(R.id.ivScanDetail)
                        if (width == 320 || font == 2f && width < 600) {
                            assertTrue("Metadata squeezed beside photo: $label", metadata.top >= photo.bottom)
                        }
                    }
                }
            }
        }
    }

    @Test fun submittedFeedbackAndHistoryRowsFitLongContent() {
        for (width in listOf(320, 600)) for (font in listOf(1f, 2f)) {
            RuntimeEnvironment.setQualifiers("tl-w${width}dp-h480dp-320dpi")
            RuntimeEnvironment.setFontScale(font)
            val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_BananaQ)
            val root = LayoutInflater.from(context).inflate(R.layout.activity_feedback_detail, null)
            populate(root)
            root.findViewById<View>(R.id.formScroll).visibility = View.GONE
            root.findViewById<View>(R.id.successScroll).visibility = View.VISIBLE
            for (id in listOf(R.id.summaryScan, R.id.summaryRating, R.id.summaryConfidence, R.id.summarySubmitted)) {
                root.findViewById<TextView>(id).text = "A long translated result value with several words"
            }
            root.findViewById<TextView>(R.id.summaryComments).text = "Long offline feedback. ".repeat(30)
            measure(root, width * 2, 960)
            assertText(root, "submitted feedback $width font=$font")
            assertScrollViewports(root, "submitted feedback")
            val row = LayoutInflater.from(context).inflate(R.layout.item_history_scan, null)
            for ((id, value) in mapOf(R.id.tvDiseaseName to "Cordana Leaf Spot",
                    R.id.tvDateTime to "Wednesday, October 21, 2026 at 04:15 PM",
                    R.id.tvAccuracy to "100.00%", R.id.tvStatus to "Hindi matukoy")) {
                row.findViewById<TextView>(id).text = value
            }
            row.findViewById<View>(R.id.tvFeedbackStatus).visibility = View.VISIBLE
            measure(row, width * 2, null)
            assertText(row, "history row $width font=$font")
        }
    }

    @Test fun dialogContentsCanScrollToTheirActionsInShortWindows() {
        for (language in listOf("en", "tl")) {
            RuntimeEnvironment.setQualifiers("$language-w320dp-h320dp-320dpi")
            RuntimeEnvironment.setFontScale(2f)
            val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_BananaQ)
            for (layout in listOf(R.layout.dialog_avatar_picker, R.layout.dialog_avatar_preview,
                    R.layout.dialog_feedback_confirmation, R.layout.dialog_unidentified_scan)) {
                val content = LayoutInflater.from(context).inflate(layout, null)
                val scroll = NestedScrollView(context).apply { addView(content) }
                measure(scroll, 560, 480)
                assertText(content, "dialog ${context.resources.getResourceEntryName(layout)} $language")
                scroll.fullScroll(View.FOCUS_DOWN)
                assertTrue("Dialog bottom cannot be reached", content.bottom - scroll.scrollY <= scroll.height)
            }
        }
    }

    internal fun populate(root: View) {
        for ((id, value) in mapOf(R.id.tvDiseaseName to "Cordana Leaf Spot",
                R.id.tvDateTime to "Wednesday, October 21, 2026 at 04:15 PM",
                R.id.tvAccuracy to "100.00%", R.id.detailDiseaseName to "Cordana Leaf Spot",
                R.id.detailScientificName to "Neocordana musae",
                R.id.detailDescription to "A detailed disease description. ".repeat(12),
                R.id.detailSymptoms to "Symptoms to look for on the leaf. ".repeat(10),
                R.id.detailSpread to "How the disease spreads. ".repeat(10),
                R.id.topicTitle to "Understanding the result and treatment recommendations",
                R.id.topicBody to "A longer translated manual topic. ".repeat(30),
                R.id.dateText to "Wednesday, 2026", R.id.dayMonthText to "October 21",
                R.id.timeText to "04:15:59 PM")) {
            root.findViewById<TextView>(id)?.text = value
        }
        root.findViewById<TextView>(R.id.etComments)?.text = android.text.SpannableStringBuilder(
            "Long feedback on the scan result. ".repeat(12))
        root.findViewById<View>(R.id.emptyState)?.visibility = View.VISIBLE
    }

    private fun measure(view: View, width: Int, height: Int?) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height ?: 0,
                if (height == null) View.MeasureSpec.UNSPECIFIED else View.MeasureSpec.EXACTLY))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    private fun assertBrandHeader(view: View, label: String) {
        if (view is ResponsiveHeaderRow) {
            val brand = view.getChildAt(0) as? ViewGroup
            val logo = brand?.getChildAt(0) as? android.widget.ImageView
            val name = brand?.getChildAt(1) as? TextView
            if (logo != null && name?.text == view.context.getString(R.string.app_name)) {
                val resources = view.resources
                assertEquals("Brand size differs: $label",
                    resources.getDimension(R.dimen.app_header_brand_text_size), name.textSize, 0.01f)
                val size = resources.getDimensionPixelSize(R.dimen.app_header_logo_size)
                assertEquals("Logo width differs: $label", size, logo.width)
                assertEquals("Logo height differs: $label", size, logo.height)
            }
        }
        if (view is ViewGroup) for (index in 0 until view.childCount) assertBrandHeader(view.getChildAt(index), label)
    }

    private fun assertText(view: View, label: String) {
        if (view.visibility != View.VISIBLE || view is HorizontalScrollView) return
        if (view is TextView && view.text.isNotEmpty()) {
            val text = view.layout
            assertNotNull("Text has no layout: $label ${view.text}", text)
            assertTrue("Clipped text: $label ${view.text}", view.height >=
                text.height + view.compoundPaddingTop + view.compoundPaddingBottom)
            val parent = view.parent as? ViewGroup
            if (parent != null) assertTrue("Text exceeds parent: $label ${view.text}",
                view.left >= 0 && view.right <= parent.width)
            if (view.isClickable && view.text.none { it.isWhitespace() }) {
                assertEquals("Single-word action broken across lines: $label ${view.text}", 1, text.lineCount)
            }
            for (line in 0 until text.lineCount) {
                assertEquals("Unexpected truncation: $label", 0, text.getEllipsisCount(line))
            }
        }
        if (view is ViewGroup) for (index in 0 until view.childCount) assertText(view.getChildAt(index), label)
    }

    private fun assertScrollViewports(view: View, label: String) {
        if (view.visibility != View.VISIBLE) return
        if (view is ScrollView || view is NestedScrollView) {
            assertTrue("No scroll viewport: $label", view.height > 0)
            val parent = view.parent as? ViewGroup
            if (parent != null) assertTrue("Scroll extends below its window: $label",
                view.bottom <= parent.height - parent.paddingBottom)
        }
        if (view is ViewGroup) for (index in 0 until view.childCount) assertScrollViewports(view.getChildAt(index), label)
    }
}
