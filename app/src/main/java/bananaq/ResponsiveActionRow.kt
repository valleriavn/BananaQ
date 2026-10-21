package bananaq

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.ceil

/** Preserve native action labels by stacking buttons before they become too narrow. */
class ResponsiveActionRow @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private var originals: List<LayoutParams>? = null
    private var stacked: Boolean? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            if (originals == null) originals = (0 until childCount).map {
                LayoutParams(getChildAt(it).layoutParams as LayoutParams)
            }
            val available = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            var spacing = 0
            var widestAction = 0
            var actions = 0
            for (index in 0 until childCount) {
                val child = getChildAt(index)
                val params = originals!![index]
                spacing += params.marginStart + params.marginEnd
                if (child is TextView) {
                    widestAction = maxOf(widestAction, child.minimumWidth,
                        ceil(child.paint.measureText(child.text.toString().replace("\n", " "))).toInt() +
                            child.compoundPaddingLeft + child.compoundPaddingRight)
                    actions++
                } else spacing += params.width.coerceAtLeast(0)
            }
            val shouldStack = widestAction * actions + spacing > available
            if (stacked != shouldStack) {
                stacked = shouldStack
                orientation = if (shouldStack) VERTICAL else HORIZONTAL
                var action = 0
                for (index in 0 until childCount) {
                    val child = getChildAt(index)
                    val params = LayoutParams(originals!![index])
                    if (child is TextView) {
                        if (shouldStack) {
                            params.width = LayoutParams.MATCH_PARENT
                            params.weight = 0f
                            params.marginStart = 0
                            params.marginEnd = 0
                            if (action > 0) params.topMargin = resources.getDimensionPixelSize(R.dimen.spacing_8)
                        }
                        action++
                    } else child.visibility = if (shouldStack) View.GONE else View.VISIBLE
                    child.layoutParams = params
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
