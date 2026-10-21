package bananaq

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** Decides orientation before measuring, without post-layout mutation loops. */
class ResponsiveDateTimeRow @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (childCount == 2 && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val dates = getChildAt(0) as LinearLayout
            val time = getChildAt(1) as TextView
            val gap = resources.getDimensionPixelSize(R.dimen.spacing_8)
            val available = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            var dateWidth = 0f
            for (index in 0 until dates.childCount) {
                val text = dates.getChildAt(index) as TextView
                dateWidth = maxOf(dateWidth, text.paint.measureText(text.text.toString()) +
                    text.compoundPaddingLeft + text.compoundPaddingRight)
            }
            val stacked = dateWidth + gap + time.paint.measureText(time.text.toString()) +
                time.compoundPaddingLeft + time.compoundPaddingRight > available
            orientation = if (stacked) VERTICAL else HORIZONTAL
            (dates.layoutParams as LayoutParams).apply {
                width = if (stacked) ViewGroup.LayoutParams.MATCH_PARENT else 0
                weight = if (stacked) 0f else 1f
            }
            (time.layoutParams as LayoutParams).apply {
                width = if (stacked) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
                marginStart = if (stacked) 0 else gap
                topMargin = if (stacked) gap else 0
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
