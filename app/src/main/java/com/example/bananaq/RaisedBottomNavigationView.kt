package com.example.bananaq

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
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
    private val barHeight = resources.getDimensionPixelSize(R.dimen.bottom_navigation_height)
    private val fabSize = resources.getDimensionPixelSize(R.dimen.bottom_navigation_fab_size)
    private val standardIconSize = resources.getDimensionPixelSize(R.dimen.bottom_navigation_icon_size)
    private val scanIconSize = resources.getDimensionPixelSize(R.dimen.bottom_navigation_scan_icon_size)
    private val itemIndicatorWidth = resources.getDimensionPixelSize(R.dimen.bottom_navigation_item_width)
    private val itemIndicatorHeight = resources.getDimensionPixelSize(R.dimen.bottom_navigation_item_height)
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

        barBackground = NavigationBarBackgroundView(context)
        addView(
            barBackground,
            LayoutParams(LayoutParams.MATCH_PARENT, barHeight, Gravity.BOTTOM)
        )

        navigationRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            clipChildren = false
            clipToPadding = false
        }
        addView(
            navigationRow,
            LayoutParams(LayoutParams.MATCH_PARENT, barHeight, Gravity.BOTTOM)
        )

        addStandardTab(R.id.nav_home, R.drawable.ic_nav_home_rounded, R.string.nav_home)
        addStandardTab(R.id.nav_history, R.drawable.ic_nav_history_rounded, R.string.nav_history)
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
            FrameLayout.LayoutParams(itemIndicatorWidth, itemIndicatorHeight, Gravity.CENTER)
        )

        val iconView = ImageView(context).apply {
            setImageResource(icon)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        iconHolder.addView(
            iconView,
            FrameLayout.LayoutParams(standardIconSize, standardIconSize, Gravity.CENTER)
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
            stateListAnimator = null
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
            FrameLayout.LayoutParams(scanIconSize, scanIconSize, Gravity.CENTER)
        )

        addView(
            root, LayoutParams(
                fabSize, fabSize, Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply { topMargin = (barHeight - fabSize) / 2 }
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
            height = barHeight + bottomInset
        }
        navigationRow.layoutParams = navigationRow.layoutParams.apply {
            height = barHeight + bottomInset
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

            val targetScale = if (selected) {
                if (tab.isScanFab) 1.04f else 1.08f
            } else 1f
            if (animate) {
                tab.iconHolder.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .setDuration(180L)
                    .start()
            } else {
                tab.iconHolder.scaleX = targetScale
                tab.iconHolder.scaleY = targetScale
            }
        }
    }

    private fun dp(value: Int): Int = (value * density + 0.5f).toInt()
    private class NavigationBarBackgroundView(context: Context) : View(context) {
        private val density = resources.displayMetrics.density
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
            val top = borderPaint.strokeWidth / 2f
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)
            canvas.drawLine(0f, top, width.toFloat(), top, borderPaint)
        }
    }
}
