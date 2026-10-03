package com.example.bananaq

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.math.PI
import kotlin.math.sin

/** Deform the upper leaf canopy; keep the inspection scene and ground fixed. */
private class SwayingLeavesDrawable(private val bitmap: Bitmap) : Drawable() {
    private val columns = 40
    private val rows = 48
    private val vertices = FloatArray((columns + 1) * (rows + 1) * 2)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    var phase = 0f
        set(value) { field = value; invalidateSelf() }

    override fun getIntrinsicWidth() = bitmap.width
    override fun getIntrinsicHeight() = bitmap.height

    override fun draw(canvas: Canvas) {
        var index = 0
        for (row in 0..rows) {
            val y = row.toFloat() / rows
            for (column in 0..columns) {
                val x = column.toFloat() / columns
                // Fade the deformation to zero before the fruit and held leaf.
                val canopy = ((0.45f - y) / 0.22f).coerceIn(0f, 1f)
                // Protect the farmer's hat at the lower left of the canopy.
                val farmerProtection = if (y <= 0.30f) 1f
                    else ((x - 0.31f) / 0.09f).coerceIn(0f, 1f)
                val weight = canopy * canopy * (3f - 2f * canopy) * farmerProtection
                val sway = sin(phase.toDouble() + x * 0.8).toFloat() * bitmap.width * 0.006f * weight
                vertices[index++] = x * bitmap.width + sway
                vertices[index++] = y * bitmap.height
            }
        }
        val save = canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width().toFloat() / bitmap.width, bounds.height().toFloat() / bitmap.height)
        canvas.drawBitmapMesh(bitmap, columns, rows, vertices, 0, null, 0, paint)
        canvas.restoreToCount(save)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable opacity is deprecated")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

fun LifecycleOwner.animateLeafCanopy(view: View) {
    val image = view as? ImageView ?: return
    // Prevent duplicate lifecycle observers when a screen returns to the foreground.
    if (image.drawable is SwayingLeavesDrawable) return
    val original = image.drawable as? BitmapDrawable ?: return
    val leaves = SwayingLeavesDrawable(original.bitmap)
    image.setImageDrawable(leaves)
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        private var motion: ValueAnimator? = null

        override fun onResume(owner: LifecycleOwner) {
            val enabled = if (Build.VERSION.SDK_INT >= 26) ValueAnimator.areAnimatorsEnabled()
                else Settings.Global.getFloat(view.context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
            if (!enabled || motion != null) return
            motion = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
                duration = 5200L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { leaves.phase = it.animatedValue as Float }
                start()
            }
        }

        override fun onPause(owner: LifecycleOwner) {
            motion?.cancel()
            motion = null
        }
    })
}
