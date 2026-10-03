package com.example.bananaq

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** Keep the original row when it fits; stack details when enlarged text needs more room. */
class ResponsiveScanRow @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var lastStacked: Boolean? = null
    private var originalDetails: LayoutParams? = null
    private var originalBadges: LayoutParams? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (childCount == 3 && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val image = getChildAt(0)
            val details = getChildAt(1)
            val badges = getChildAt(2) as LinearLayout
            if (originalDetails == null) {
                originalDetails = LayoutParams(details.layoutParams as LayoutParams)
                originalBadges = LayoutParams(badges.layoutParams as LayoutParams)
            }
            val available = (MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight).coerceAtLeast(0)
            badges.measure(MeasureSpec.makeMeasureSpec(minOf(available, (100 * resources.displayMetrics.density).toInt()), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            val name = details.findViewById<TextView>(R.id.tvDiseaseName)
            val minimumTextWidth = (name.textSize * 7f).toInt()
            val imageWidth = image.layoutParams.width.coerceAtLeast(0)
            val stacked = available < imageWidth + originalDetails!!.marginStart + badges.measuredWidth + minimumTextWidth
            if (lastStacked != stacked) {
                lastStacked = stacked
                orientation = if (stacked) VERTICAL else HORIZONTAL
                details.layoutParams = LayoutParams(originalDetails!!).apply {
                    if (stacked) {
                        width = ViewGroup.LayoutParams.MATCH_PARENT
                        weight = 0f
                        topMargin = marginStart
                        marginStart = 0
                    }
                }
                badges.layoutParams = LayoutParams(originalBadges!!).apply {
                    if (stacked) {
                        width = ViewGroup.LayoutParams.MATCH_PARENT
                        topMargin = (8 * resources.displayMetrics.density).toInt()
                    }
                }
                badges.gravity = if (stacked) Gravity.START else Gravity.END
                details.findViewById<LinearLayout>(R.id.accuracyRow).orientation =
                    if (stacked) VERTICAL else HORIZONTAL
            }
            for (index in 0 until badges.childCount) {
                val badge = badges.getChildAt(index) as? TextView ?: continue
                val limit = if (stacked) available else (100 * resources.displayMetrics.density).toInt()
                if (badge.maxWidth != limit) badge.maxWidth = limit
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
