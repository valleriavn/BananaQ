package bananaq

import android.app.Activity
import android.util.TypedValue
import android.view.ViewGroup
import kotlin.math.roundToInt

/** Dialog geometry follows the usable activity window, including split screen. */
fun Activity.dialogWidth(maxWidthDp: Int = 440, fraction: Float = 0.9f): Int {
    val content = findViewById<ViewGroup>(android.R.id.content)
    val root = content.getChildAt(0)
    val measuredWidth = root?.let { it.width - it.paddingLeft - it.paddingRight } ?: 0
    val metrics = resources.displayMetrics
    val available = if (measuredWidth > 0) measuredWidth else
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            resources.configuration.screenWidthDp.toFloat(), metrics).roundToInt()
    val maximum = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
        maxWidthDp.toFloat(), metrics).roundToInt()
    return minOf((available * fraction).roundToInt(), maximum).coerceAtLeast(1)
}

/** Bound scrollable dialogs to this activity's usable window, including IME and cutouts. */
fun android.app.Dialog.fitWithinActivity(
    activity: Activity, maxWidthDp: Int = 440, fraction: Float = 0.9f
) {
    val root = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
    val usableHeight = root?.let {
        val insets = androidx.core.view.ViewCompat.getRootWindowInsets(it)
        val bars = insets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or
            androidx.core.view.WindowInsetsCompat.Type.displayCutout())
        val ime = insets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
        it.height - maxOf(it.paddingTop, bars?.top ?: 0) -
            maxOf(it.paddingBottom, bars?.bottom ?: 0, ime?.bottom ?: 0)
    } ?: 0
    val width = activity.dialogWidth(maxWidthDp, fraction)
    val content = findViewById<ViewGroup>(android.R.id.content).getChildAt(0) ?: return
    if (usableHeight > 0) {
        val gap = activity.resources.getDimensionPixelSize(R.dimen.spacing_16)
        val maximum = (usableHeight - gap * 2).coerceAtLeast(1)
        content.measure(android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(maximum, android.view.View.MeasureSpec.AT_MOST))
        window?.setLayout(width, minOf(content.measuredHeight, maximum).coerceAtLeast(1))
    } else {
        window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
