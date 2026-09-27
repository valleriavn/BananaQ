package com.example.bananaq

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/** A labeled navigation bar with a prominent rounded active tab. */
class RaisedBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private data class Tab(
        val id: Int,
        val icon: Int,
        val title: Int,
        val root: FrameLayout,
        val indicator: View,
        val iconHolder: FrameLayout,
        val iconView: ImageView,
        val label: TextView
    )

    private val density = resources.displayMetrics.density
    private val tabs = mutableListOf<Tab>()
    private var itemSelectedListener: ((Int) -> Boolean)? = null

    var selectedItemId: Int = R.id.nav_home
        set(value) {
            field = value
            renderSelection(animate = isLaidOut)
        }

    init {
        clipChildren = false
        clipToPadding = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        background = ContextCompat.getDrawable(context, R.drawable.nav_floating_bar)
        elevation = 0f

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            clipChildren = false
            clipToPadding = false
            // This row is drawn after the bar so the active tab stays crisp.
            elevation = 0f
            setPadding(0, 0, 0, 0)
        }
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        row.bringToFront()

        val definitions = listOf(
            Triple(R.id.nav_home, R.drawable.nav_home_icon, R.string.nav_home),
            Triple(R.id.nav_history, R.drawable.nav_history_icon, R.string.nav_history),
            Triple(R.id.nav_scan, R.drawable.ic_nav_scan_rounded, R.string.nav_scan),
            Triple(R.id.nav_feedback, R.drawable.nav_feedback_icon, R.string.nav_feedback),
            Triple(R.id.nav_account, R.drawable.nav_account_icon, R.string.account_title)
        )

        definitions.forEach { (id, icon, title) ->
            val tabRoot = FrameLayout(context).apply {
                isClickable = true
                isFocusable = true
                contentDescription = context.getString(title)
                setOnClickListener {
                    if (itemSelectedListener?.invoke(id) != false) selectedItemId = id
                }
            }

            val indicator = View(context).apply {
                background = ContextCompat.getDrawable(context, R.drawable.nav_active_indicator)
                visibility = View.INVISIBLE
            }
            tabRoot.addView(indicator, FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(4), Gravity.TOP
            ).apply {
                leftMargin = dp(10)
                rightMargin = dp(10)
            })

            val iconHolder = FrameLayout(context).apply {
                clipToOutline = false
                elevation = 0f
            }
            tabRoot.addView(iconHolder, FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER))

            val iconView = ImageView(context).apply {
                setImageResource(icon)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            iconHolder.addView(iconView, FrameLayout.LayoutParams(dp(27), dp(27), Gravity.CENTER))

            val label = TextView(context).apply {
                text = context.getString(title)
                gravity = Gravity.CENTER
                textSize = 12f
                includeFontPadding = false
                maxLines = 1
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            tabRoot.addView(label, FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(24), Gravity.BOTTOM
            ).apply { bottomMargin = dp(6) })

            row.addView(tabRoot, LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            tabs += Tab(id, icon, title, tabRoot, indicator, iconHolder, iconView, label)
        }
        renderSelection(animate = false)
    }

    fun setOnItemSelectedListener(listener: (Int) -> Boolean) {
        itemSelectedListener = listener
    }

    fun setSystemBottomInset(bottomInset: Int) {
        tabs.forEach { tab ->
            tab.root.setPadding(0, 0, 0, bottomInset)
        }
    }

    private fun renderSelection(animate: Boolean) {
        val activeColor = ContextCompat.getColor(context, R.color.banana_green)
        val inactiveColor = ContextCompat.getColor(context, R.color.nav_yellow)
        val activeBackground = ContextCompat.getColor(context, R.color.nav_active_tint)
        val transparent = ContextCompat.getColor(context, android.R.color.transparent)
        tabs.forEach { tab ->
            val selected = tab.id == selectedItemId
            tab.root.isSelected = selected
            tab.root.isActivated = selected
            tab.root.setBackgroundColor(if (selected) activeBackground else transparent)
            tab.indicator.visibility = if (selected) View.VISIBLE else View.INVISIBLE
            tab.iconHolder.background = null
            tab.iconHolder.elevation = 0f
            val targetY = 0f
            if (animate) {
                if (selected) {
                    tab.iconHolder.alpha = 0.75f
                    tab.iconHolder.scaleX = 0.88f
                    tab.iconHolder.scaleY = 0.88f
                }
                tab.iconHolder.animate()
                    .translationY(targetY)
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220L)
                    .start()
            } else {
                tab.iconHolder.translationY = targetY
                tab.iconHolder.alpha = 1f
                tab.iconHolder.scaleX = 1f
                tab.iconHolder.scaleY = 1f
            }
            tab.label.animate().cancel()
            tab.label.alpha = 1f
            tab.label.visibility = View.GONE
            val iconSize = when {
                selected && tab.id == R.id.nav_scan -> dp(44)
                selected -> dp(40)
                tab.id == R.id.nav_scan -> dp(42)
                else -> dp(38)
            }
            tab.iconView.layoutParams = (tab.iconView.layoutParams as FrameLayout.LayoutParams).apply {
                width = iconSize
                height = iconSize
                gravity = Gravity.CENTER
            }
            tab.iconView.setColorFilter(if (selected) activeColor else inactiveColor)
            tab.label.setTextColor(
                if (selected) activeColor else inactiveColor
            )
            tab.label.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun dp(value: Int): Int = (value * density + 0.5f).toInt()
}
