package com.example.bananaq

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class LocaleAwareActivity : AppCompatActivity() {
    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        adaptToFontScale()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            overridePendingTransition(0, 0)
        }
    }

    override fun finish() {
        super.finish()
        if (android.os.Build.VERSION.SDK_INT < 34) overridePendingTransition(0, 0)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    fun changeAppLanguage(language: String) {
        if (LocaleHelper.selectedLanguage(this) == language) return
        LocaleHelper.saveLanguage(this, language)
        recreate()
    }
}
