package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Startup router only. Android already provides the app launch splash, so this
 * activity must never display a second splash layout or add an artificial delay.
 */
class SplashActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val completed = getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("onboarded", false)
        startActivity(Intent(this,
            if (completed) MainActivity::class.java else LanguageActivity::class.java))
        finish()
        overridePendingTransition(0, 0)
    }
}
