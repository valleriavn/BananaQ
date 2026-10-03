package com.example.bananaq

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import androidx.core.widget.NestedScrollView

/** Reflow only enlarged text; retain the existing layout at the default font size. */
fun AppCompatActivity.adaptToFontScale() {
    val scale = resources.configuration.fontScale
    val density = resources.displayMetrics.density
    fun dp(value: Int) = (value * density + .5f).toInt()

    findViewById<LinearLayout>(R.id.ratingGrid)?.let { grid ->
        grid.doOnLayout {
            val available = grid.width - grid.paddingLeft - grid.paddingRight
            if (available <= 0) return@doOnLayout
            for (index in 0 until grid.childCount) {
                val row = grid.getChildAt(index) as? LinearLayout ?: continue
                val desiredWidth = dp(90) * row.childCount
                if (desiredWidth > available) {
                    row.layoutParams = row.layoutParams.apply { width = available }
                    for (childIndex in 0 until row.childCount) {
                        val child = row.getChildAt(childIndex)
                        child.layoutParams = (child.layoutParams as LinearLayout.LayoutParams).apply {
                            width = 0
                            weight = 1f
                        }
                    }
                }
            }
        }
    }
    if (scale <= 1f) return

    for (id in intArrayOf(R.id.summaryScan, R.id.summaryRating,
        R.id.summaryConfidence, R.id.summarySubmitted)) {
        findViewById<View>(id)?.let { value ->
            value.layoutParams = (value.layoutParams as LinearLayout.LayoutParams).apply {
                width = 0
                weight = 1f
            }
        }
    }
    findViewById<View>(R.id.scanFrame)?.let { frame ->
        frame.layoutParams = (frame.layoutParams as ConstraintLayout.LayoutParams).apply {
            topToTop = ConstraintLayout.LayoutParams.UNSET
            topToBottom = R.id.scannerHeader
            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            bottomToTop = R.id.tvInstruction
            matchConstraintDefaultWidth = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD
            marginStart = dp(32)
            marginEnd = dp(32)
        }
        findViewById<View>(R.id.tvInstruction).layoutParams =
            (findViewById<View>(R.id.tvInstruction).layoutParams as ConstraintLayout.LayoutParams).apply {
                bottomToTop = R.id.controlsLayout
            }
    }

    findViewById<View>(R.id.diseaseCarousel)?.let { carousel ->
        carousel.layoutParams = carousel.layoutParams.apply {
            height = resources.getDimensionPixelSize(R.dimen.home_disease_carousel_height) +
                (dp(48) * (scale - 1f)).toInt()
        }
    }
    findViewById<LinearLayout>(R.id.homeContent)?.let { content ->
        val parent = content.parent as ConstraintLayout
        val position = parent.indexOfChild(content)
        val constraints = content.layoutParams
        val scroll = NestedScrollView(this).apply {
            id = R.id.largeTextHomeScroll
            isFillViewport = true
            clipToPadding = true
        }
        parent.removeView(content)
        content.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        content.findViewById<View>(R.id.ivSoil).layoutParams =
            (content.findViewById<View>(R.id.ivSoil).layoutParams as LinearLayout.LayoutParams).apply {
                height = dp(180)
                weight = 0f
            }
        scroll.addView(content)
        parent.addView(scroll, position, constraints)
        content.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val image = content.findViewById<View>(R.id.ivSoil)
            var occupied = content.paddingTop + content.paddingBottom
            for (index in 0 until content.childCount) {
                val child = content.getChildAt(index)
                if (child !== image && child.visibility != View.GONE) {
                    val margins = child.layoutParams as ViewGroup.MarginLayoutParams
                    occupied += child.measuredHeight + margins.topMargin + margins.bottomMargin
                }
            }
            val imageMargins = image.layoutParams as ViewGroup.MarginLayoutParams
            val remaining = scroll.height - scroll.paddingTop - scroll.paddingBottom - occupied -
                imageMargins.topMargin - imageMargins.bottomMargin
            val desired = maxOf(dp(96), remaining)
            if (image.layoutParams.height != desired) image.layoutParams = image.layoutParams.apply { height = desired }
        }
        findViewById<LinearLayout>(R.id.homeDateTimeRow).let { row ->
            val dates = row.getChildAt(0) as LinearLayout
            val time = row.getChildAt(1) as android.widget.TextView
            val dateParams = LinearLayout.LayoutParams(dates.layoutParams as LinearLayout.LayoutParams)
            val timeParams = LinearLayout.LayoutParams(time.layoutParams as LinearLayout.LayoutParams)
            row.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                var dateWidth = 0f
                for (index in 0 until dates.childCount) {
                    val label = dates.getChildAt(index) as android.widget.TextView
                    dateWidth = maxOf(dateWidth, label.paint.measureText(label.text.toString()))
                }
                val required = dateWidth + time.paint.measureText(time.text.toString()) + timeParams.marginStart
                val stacked = required > row.width - row.paddingLeft - row.paddingRight
                val orientation = if (stacked) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                if (row.orientation != orientation) {
                    row.orientation = orientation
                    dates.layoutParams = LinearLayout.LayoutParams(dateParams).apply {
                        if (stacked) { width = ViewGroup.LayoutParams.MATCH_PARENT; weight = 0f }
                    }
                    time.layoutParams = LinearLayout.LayoutParams(timeParams).apply {
                        if (stacked) { marginStart = 0; topMargin = dp(8) }
                    }
                }
            }
        }
    }

    findViewById<View>(R.id.btnGetStarted)?.let { button ->
        val content = button.parent as ConstraintLayout
        val parent = content.parent as ViewGroup
        val position = parent.indexOfChild(content)
        val params = content.layoutParams
        val scroll = NestedScrollView(this).apply {
            id = R.id.largeTextOnboardingScroll
            isFillViewport = true
            background = content.background?.constantState?.newDrawable(resources)
        }
        val plant = content.findViewById<View>(R.id.bananaPlant)
        plant.layoutParams = (plant.layoutParams as ConstraintLayout.LayoutParams).apply {
            height = dp(240)
            topToBottom = R.id.btnGetStarted
        }
        content.findViewById<View>(R.id.topCurve).layoutParams =
            (content.findViewById<View>(R.id.topCurve).layoutParams as ConstraintLayout.LayoutParams).apply {
                bottomToTop = R.id.btnGetStarted
            }
        parent.removeView(content)
        content.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        scroll.addView(content)
        parent.addView(scroll, position, params)
    }

    findViewById<View>(R.id.fullResultCard)?.let { card ->
        val cardGroup = card as ViewGroup
        val content = cardGroup.getChildAt(0) as LinearLayout
        val params = content.layoutParams
        cardGroup.removeView(content)
        content.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val extra = content.findViewById<LinearLayout>(R.id.extraDetailsLayout)
        extra.layoutParams = (extra.layoutParams as LinearLayout.LayoutParams).apply {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            weight = 0f
        }
        for (index in 0 until extra.childCount) {
            val inner = extra.getChildAt(index) as? NestedScrollView ?: continue
            inner.layoutParams = (inner.layoutParams as LinearLayout.LayoutParams).apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                weight = 0f
            }
            inner.isNestedScrollingEnabled = false
        }
        val scroll = NestedScrollView(this).apply {
            id = R.id.largeTextResultScroll
            isFillViewport = true
        }
        scroll.addView(content)
        cardGroup.addView(scroll, params)
    }
}
