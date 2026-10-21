package bananaq

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/** Bounds reading width while allowing the scroll container to use its whole viewport. */
class ContentColumn @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        val maximum = resources.getDimensionPixelSize(R.dimen.page_content_max_width)
        val width = if (mode == MeasureSpec.UNSPECIFIED) maximum
            else minOf(MeasureSpec.getSize(widthMeasureSpec), maximum)
        super.onMeasure(MeasureSpec.makeMeasureSpec(width,
            if (mode == MeasureSpec.UNSPECIFIED) MeasureSpec.AT_MOST else mode), heightMeasureSpec)
    }
}
