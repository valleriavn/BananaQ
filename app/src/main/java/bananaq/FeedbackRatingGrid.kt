package bananaq

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

/** Two centered choices above three choices, with equal cells that grow for translated text. */
class FeedbackRatingGrid @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {
    private var cellWidth = 0
    private var cellHeight = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        cellWidth = max(0, width - paddingLeft - paddingRight) / 3
        cellHeight = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val margins = child.layoutParams as MarginLayoutParams
            child.measure(
                MeasureSpec.makeMeasureSpec(max(0, cellWidth - margins.leftMargin - margins.rightMargin), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            cellHeight = max(cellHeight, child.measuredHeight + margins.topMargin + margins.bottomMargin)
        }
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val margins = child.layoutParams as MarginLayoutParams
            child.measure(
                MeasureSpec.makeMeasureSpec(max(0, cellWidth - margins.leftMargin - margins.rightMargin), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(max(0, cellHeight - margins.topMargin - margins.bottomMargin), MeasureSpec.EXACTLY)
            )
        }
        setMeasuredDimension(resolveSize(width, widthMeasureSpec),
            resolveSize(paddingTop + cellHeight * 2 + paddingBottom, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val available = width - paddingLeft - paddingRight
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val margins = child.layoutParams as MarginLayoutParams
            val firstRow = index < 2
            val columns = if (firstRow) 2 else 3
            val column = if (firstRow) index else index - 2
            val position = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) columns - 1 - column else column
            val x = paddingLeft + (available - columns * cellWidth) / 2 + position * cellWidth + margins.leftMargin
            val y = paddingTop + (if (firstRow) 0 else cellHeight) + margins.topMargin
            child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams = MarginLayoutParams(context, attrs)
    override fun generateDefaultLayoutParams(): LayoutParams = MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    override fun generateLayoutParams(params: LayoutParams): LayoutParams = MarginLayoutParams(params)
    override fun checkLayoutParams(params: LayoutParams): Boolean = params is MarginLayoutParams
}
