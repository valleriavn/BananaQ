package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import com.example.bananaq.R

class GetStartedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_get_started)
        applySystemInsets()

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        btnGetStarted.setOnClickListener {
            getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            finish()
        }
    }
}
