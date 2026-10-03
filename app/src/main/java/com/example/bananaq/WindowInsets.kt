package com.example.bananaq

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun AppCompatActivity.applySystemInsets(bottomInsetColor: Int? = null) {
    val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
    val navigation: RaisedBottomNavigationView? = root.findViewById(R.id.bottomNavigation)
    val insetBackground = bottomInsetColor?.let { color ->
        android.graphics.drawable.LayerDrawable(arrayOf(
            root.background ?: android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT),
            android.graphics.drawable.ColorDrawable(color)
        )).apply {
            setLayerGravity(1, android.view.Gravity.BOTTOM)
            setLayerHeight(1, 0)
            root.background = this
        }
    }
    val left = root.paddingLeft
    val top = root.paddingTop
    val right = root.paddingRight
    val bottom = root.paddingBottom
    val navigationHeight = navigation?.layoutParams?.height ?: 0
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
        insetBackground?.setLayerHeight(1, maxOf(bars.bottom, keyboard.bottom))
        if (navigation != null) {
            val keyboardOffset = maxOf(0, keyboard.bottom - bars.bottom)
            view.setPadding(left + bars.left, top + bars.top, right + bars.right,
                bottom + keyboardOffset)
            navigation.layoutParams = navigation.layoutParams.apply {
                height = navigationHeight + bars.bottom
            }
            navigation.setSystemBottomInset(bars.bottom)
        } else {
            view.setPadding(left + bars.left, top + bars.top, right + bars.right,
                bottom + maxOf(bars.bottom, keyboard.bottom))
        }
        // The root handles these insets; navigation widgets must not add them again.
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(root)
}
