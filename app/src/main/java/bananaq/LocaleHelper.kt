package bananaq

import android.content.Context
import android.content.res.Configuration
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

        // Change only the locale. Preserve Android's font scale and display density
        // so sp text, dp geometry, and window resource qualifiers remain consistent.
        return context.createConfigurationContext(configuration)
    }

}
