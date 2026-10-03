package com.example.bananaq

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.ceil

/** Equal-width tabs when they fit, horizontally scrollable whole labels when they do not. */
class ResponsiveTabRow @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {
    init { isFillViewport = true }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val row = getChildAt(0) as? LinearLayout
        if (row != null && row.childCount > 0 && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val available = (MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight).coerceAtLeast(0)
            var widest = 0
            var margins = 0
            for (index in 0 until row.childCount) {
                val tab = row.getChildAt(index) as TextView
                val params = tab.layoutParams as LinearLayout.LayoutParams
                margins += params.leftMargin + params.rightMargin
                widest = maxOf(widest, ceil(tab.paint.measureText(tab.text.toString())).toInt() +
                    tab.compoundPaddingLeft + tab.compoundPaddingRight + (8 * resources.displayMetrics.density).toInt())
            }
            val width = maxOf(widest, (available - margins) / row.childCount)
            val rowWidth = width * row.childCount + margins
            if (row.layoutParams.width != rowWidth) row.layoutParams = row.layoutParams.apply { this.width = rowWidth }
            for (index in 0 until row.childCount) {
                val tab = row.getChildAt(index)
                val params = tab.layoutParams as LinearLayout.LayoutParams
                if (params.width != width || params.weight != 0f) {
                    tab.layoutParams = LinearLayout.LayoutParams(params).apply {
                        this.width = width
                        weight = 0f
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
