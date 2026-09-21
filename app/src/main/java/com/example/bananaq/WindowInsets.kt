package com.example.bananaq

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun AppCompatActivity.applySystemInsets() {
    val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
    val left = root.paddingLeft
    val top = root.paddingTop
    val right = root.paddingRight
    val bottom = root.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(root)
}
