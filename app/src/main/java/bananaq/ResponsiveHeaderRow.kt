package bananaq

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** Stacks trailing metadata below the main content when native text cannot fit side by side. */
class ResponsiveHeaderRow @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private fun desiredWidth(view: View): Float {
        if (view.visibility == GONE) return 0f
        val content = when (view) {
            is TextView -> view.paint.measureText(view.text.toString()) +
                view.compoundPaddingLeft + view.compoundPaddingRight
            is ViewGroup -> {
                val widths = (0 until view.childCount).map { desiredWidth(view.getChildAt(it)) }
                (if (view is LinearLayout && view.orientation == HORIZONTAL) widths.sum()
                    else widths.maxOrNull() ?: 0f) + view.paddingLeft + view.paddingRight
            }
            else -> view.layoutParams.width.coerceAtLeast(view.minimumWidth).toFloat()
        }
        val margins = view.layoutParams as? ViewGroup.MarginLayoutParams
        return maxOf(content, view.minimumWidth.toFloat()) +
            (margins?.marginStart ?: 0) + (margins?.marginEnd ?: 0)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (childCount == 2 && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val brand = getChildAt(0)
            val language = getChildAt(1)
            val available = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            val stacked = desiredWidth(brand) + desiredWidth(language) > available
            orientation = if (stacked) VERTICAL else HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            (brand.layoutParams as LayoutParams).apply {
                width = if (stacked) ViewGroup.LayoutParams.MATCH_PARENT else 0
                weight = if (stacked) 0f else 1f
            }
            (language.layoutParams as LayoutParams).apply {
                gravity = Gravity.END
                topMargin = if (stacked) resources.getDimensionPixelSize(R.dimen.spacing_8) else 0
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
