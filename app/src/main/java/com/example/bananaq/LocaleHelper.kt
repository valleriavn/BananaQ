package com.example.bananaq

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import java.util.Locale

object LocaleHelper {
    const val PREFS_NAME = "settings"
    const val KEY_LANGUAGE = "language"

    fun selectedLanguage(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "en") ?: "en"

    fun saveLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun wrap(context: Context): Context {
        val language = selectedLanguage(context)
        val locale = Locale(language)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        // Keep BananaQ's designed dimensions stable when the device's Font size
        // or Display size setting changes. Screen-size resource qualifiers still
        // adapt the UI for compact phones, landscape layouts, and tablets.
        configuration.fontScale = DEFAULT_APP_FONT_SCALE
        configuration.densityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE
        return context.createConfigurationContext(configuration)
    }

    private const val DEFAULT_APP_FONT_SCALE = 1f
}
