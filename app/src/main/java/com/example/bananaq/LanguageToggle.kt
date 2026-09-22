package com.example.bananaq

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

fun AppCompatActivity.setupLanguageToggle() {
    val tvLangEN = findViewById<TextView>(R.id.tvLangEN) ?: return
    val tvLangTL = findViewById<TextView>(R.id.tvLangTL) ?: return

    val sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
    var currentLang = sharedPrefs.getString("language", "en") ?: "en"

    fun updateToggleUI(lang: String) {
        if (lang == "en") {
            tvLangEN.setBackgroundResource(R.drawable.rounded_button_bg)
            tvLangEN.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.banana_green)
            )
            tvLangEN.setTextColor(ContextCompat.getColor(this, R.color.white))
            tvLangEN.setTypeface(null, Typeface.BOLD)

            tvLangTL.background = null
            tvLangTL.setTextColor(ContextCompat.getColor(this, R.color.banana_yellow))
            tvLangTL.setTypeface(null, Typeface.NORMAL)
        } else {
            tvLangTL.setBackgroundResource(R.drawable.rounded_button_bg)
            tvLangTL.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.banana_green)
            )
            tvLangTL.setTextColor(ContextCompat.getColor(this, R.color.white))
            tvLangTL.setTypeface(null, Typeface.BOLD)

            tvLangEN.background = null
            tvLangEN.setTextColor(ContextCompat.getColor(this, R.color.banana_yellow))
            tvLangEN.setTypeface(null, Typeface.NORMAL)
        }
    }

    updateToggleUI(currentLang)

    tvLangEN.setOnClickListener {
        if (currentLang != "en") {
            currentLang = "en"
            sharedPrefs.edit().putString("language", "en").apply()
            updateToggleUI("en")
        }
    }

    tvLangTL.setOnClickListener {
        if (currentLang != "tl") {
            currentLang = "tl"
            sharedPrefs.edit().putString("language", "tl").apply()
            updateToggleUI("tl")
        }
    }
}
