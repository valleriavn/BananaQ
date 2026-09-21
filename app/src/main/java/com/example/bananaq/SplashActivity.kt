package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import com.example.bananaq.R

class SplashActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val openNext = Runnable {
        val completed = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("onboarded", false)
        startActivity(Intent(this, if (completed) MainActivity::class.java else LanguageActivity::class.java))
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        applySystemInsets()
    }

    override fun onStart() {
        super.onStart()
        handler.postDelayed(openNext, 2000)
    }

    override fun onStop() {
        handler.removeCallbacks(openNext)
        super.onStop()
    }
}
