package com.example.bananaq

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun AppCompatActivity.applySystemInsets() {
    findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)?.apply {
        isItemActiveIndicatorEnabled = false
        itemIconTintList = androidx.core.content.ContextCompat.getColorStateList(this@applySystemInsets, R.color.nav_item_color)
        itemTextColor = itemIconTintList
        itemBackground = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        itemRippleColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
    }
    val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
    val left = root.paddingLeft
    val top = root.paddingTop
    val right = root.paddingRight
    val bottom = root.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.setPadding(left + bars.left, top + bars.top, right + bars.right,
            bottom + maxOf(bars.bottom, keyboard.bottom))
        // The root handles these insets; navigation widgets must not add them again.
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(root)
}
