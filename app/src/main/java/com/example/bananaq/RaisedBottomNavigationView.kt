package com.example.bananaq

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

/** Full-width bottom navigation with a centered, raised Scan action. */
class RaisedBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private data class Tab(
        val id: Int,
        val root: FrameLayout,
        val iconHolder: FrameLayout,
        val iconView: ImageView,
        val isScanFab: Boolean
    )

    private val density = resources.displayMetrics.density
    private val tabs = mutableListOf<Tab>()
    private val barBackground: View
    private val navigationRow: LinearLayout
    private var itemSelectedListener: ((Int) -> Boolean)? = null
    private var systemBottomInset = 0

    var selectedItemId: Int = R.id.nav_home
        set(value) {
            field = value
            renderSelection(animate = isLaidOut)
        }

    init {
        clipChildren = false
        clipToPadding = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        background = null

        barBackground = NotchedBarView(context)
        addView(
            barBackground,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(BAR_HEIGHT_DP), Gravity.BOTTOM)
        )

        navigationRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            clipChildren = false
            clipToPadding = false
        }
        addView(
            navigationRow,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(BAR_HEIGHT_DP), Gravity.BOTTOM)
        )

        addStandardTab(R.id.nav_home, R.drawable.ic_nav_home_rounded, R.string.nav_home)
        addStandardTab(R.id.nav_history, R.drawable.ic_nav_history_rounded, R.string.nav_history)

        // Preserve the middle column so the four standard actions remain balanced.
        navigationRow.addView(
            View(context),
            LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        )

        addStandardTab(R.id.nav_feedback, R.drawable.ic_nav_feedback_edit, R.string.nav_feedback)
        addStandardTab(R.id.nav_account, R.drawable.ic_nav_account_rounded, R.string.account_title)
        addScanFab()
        renderSelection(animate = false)
    }

    private fun addStandardTab(id: Int, icon: Int, title: Int) {
        val root = FrameLayout(context).apply {
            isClickable = true
            isFocusable = true
            contentDescription = context.getString(title)
            foreground = ContextCompat.getDrawable(context, R.drawable.nav_item_ripple)
            setOnClickListener { select(id) }
        }

        val iconHolder = FrameLayout(context)
        root.addView(
            iconHolder,
            FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER)
        )

        val iconView = ImageView(context).apply {
            setImageResource(icon)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        iconHolder.addView(
            iconView,
            FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER)
        )

        navigationRow.addView(
            root,
            LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        )
        tabs += Tab(id, root, iconHolder, iconView, false)
    }

    private fun addScanFab() {
        val root = FrameLayout(context).apply {
            isClickable = true
            isFocusable = true
            contentDescription = context.getString(R.string.nav_scan)
            elevation = dp(8).toFloat()
            setOnClickListener { select(R.id.nav_scan) }
        }

        val iconHolder = FrameLayout(context)
        root.addView(
            iconHolder,
            FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        val iconView = ImageView(context).apply {
            setImageResource(R.drawable.ic_nav_scan_filled)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        iconHolder.addView(
            iconView,
            FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER)
        )

        addView(
            root,
            LayoutParams(dp(FAB_SIZE_DP), dp(FAB_SIZE_DP), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                topMargin = dp(1)
            }
        )
        root.bringToFront()
        tabs += Tab(R.id.nav_scan, root, iconHolder, iconView, true)
    }

    private fun select(id: Int) {
        if (itemSelectedListener?.invoke(id) != false) selectedItemId = id
    }

    fun setOnItemSelectedListener(listener: (Int) -> Boolean) {
        itemSelectedListener = listener
    }

    fun setSystemBottomInset(bottomInset: Int) {
        systemBottomInset = bottomInset
        barBackground.layoutParams = barBackground.layoutParams.apply {
            height = dp(BAR_HEIGHT_DP) + bottomInset
        }
        navigationRow.layoutParams = navigationRow.layoutParams.apply {
            height = dp(BAR_HEIGHT_DP) + bottomInset
        }
        navigationRow.setPadding(0, 0, 0, bottomInset)
    }

    private fun renderSelection(animate: Boolean) {
        val activeIcon = ContextCompat.getColor(context, R.color.banana_green)
        val defaultIcon = ContextCompat.getColor(context, R.color.nav_yellow)

        tabs.forEach { tab ->
            val selected = tab.id == selectedItemId
            tab.root.isSelected = selected
            tab.root.isActivated = selected

            if (tab.isScanFab) {
                tab.root.setBackgroundResource(
                    if (selected) R.drawable.nav_scan_fab_active
                    else R.drawable.nav_scan_fab_default
                )
                tab.iconView.imageTintList = ColorStateList.valueOf(
                    if (selected) activeIcon else defaultIcon
                )
            } else {
                tab.iconHolder.setBackgroundResource(
                    if (selected) R.drawable.nav_item_selected_bg
                    else android.R.color.transparent
                )
                tab.iconView.imageTintList = ColorStateList.valueOf(
                    if (selected) activeIcon else defaultIcon
                )
            }

            val targetScale = if (selected) 1.06f else 1f
            if (animate) {
                tab.root.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .setDuration(180L)
                    .start()
            } else {
                tab.root.scaleX = targetScale
                tab.root.scaleY = targetScale
            }
        }
    }

    private fun dp(value: Int): Int = (value * density + 0.5f).toInt()

    /** Draws the soft center cradle shown in the selected reference design. */
    private class NotchedBarView(context: Context) : View(context) {
        private val density = resources.displayMetrics.density
        private val path = Path()
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, R.color.banana_surface)
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = density
            color = ContextCompat.getColor(context, R.color.banana_border)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val center = width / 2f
            val notchHalfWidth = 46f * density
            val notchShoulder = 34f * density
            val notchDepth = 34f * density
            val top = borderPaint.strokeWidth / 2f
            val bottom = height.toFloat() + borderPaint.strokeWidth

            path.reset()
            path.moveTo(0f, top)
            path.lineTo(center - notchHalfWidth, top)
            path.cubicTo(
                center - notchShoulder, top,
                center - notchShoulder, notchDepth,
                center, notchDepth
            )
            path.cubicTo(
                center + notchShoulder, notchDepth,
                center + notchShoulder, top,
                center + notchHalfWidth, top
            )
            path.lineTo(width.toFloat(), top)
            path.lineTo(width.toFloat(), bottom)
            path.lineTo(0f, bottom)
            path.close()

            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, borderPaint)
        }
    }

    companion object {
        private const val BAR_HEIGHT_DP = 56
        private const val FAB_SIZE_DP = 68
    }
}
