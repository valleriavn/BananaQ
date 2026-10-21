package com.example.bananaq

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** Keeps scan metadata readable beside a thumbnail, or below it in narrow windows. */
class ResponsiveFeedbackHeader @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private var originalPhoto: LayoutParams? = null
    private var originalMetadata: LayoutParams? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (childCount == 2 && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val photo = getChildAt(0)
            val metadata = getChildAt(1)
            if (originalPhoto == null) {
                originalPhoto = LayoutParams(photo.layoutParams as LayoutParams)
                originalMetadata = LayoutParams(metadata.layoutParams as LayoutParams)
            }
            val name = metadata.findViewById<TextView>(R.id.tvDiseaseName)
            val available = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            val stacked = available < originalPhoto!!.width + originalMetadata!!.marginStart + name.textSize * 9
            orientation = if (stacked) VERTICAL else HORIZONTAL
            (photo.layoutParams as LayoutParams).apply {
                width = if (stacked) ViewGroup.LayoutParams.MATCH_PARENT else originalPhoto!!.width
            }
            (metadata.layoutParams as LayoutParams).apply {
                width = if (stacked) ViewGroup.LayoutParams.MATCH_PARENT else 0
                weight = if (stacked) 0f else 1f
                marginStart = if (stacked) 0 else originalMetadata!!.marginStart
                topMargin = if (stacked) originalMetadata!!.marginStart else 0
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
