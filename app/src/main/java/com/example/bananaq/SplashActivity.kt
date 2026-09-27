package com.example.bananaq

import android.content.Intent
import android.os.Bundle

/**
 * Startup router only. The branded launch theme supplies the one system splash;
 * this activity must not display a second splash layout afterward.
 */
class SplashActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val completed = getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("onboarded", false)
        startActivity(
            Intent(
                this,
                if (completed) MainActivity::class.java else LanguageActivity::class.java
            )
        )
        finish()
        overridePendingTransition(0, 0)
    }
}
