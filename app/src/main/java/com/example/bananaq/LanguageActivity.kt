package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import com.example.bananaq.R

class LanguageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_language)
        applySystemInsets()
        // Only English resources are currently bundled; do not silently promise a translation.
        findViewById<Button>(R.id.btnTagalog).setOnClickListener {
            android.widget.Toast.makeText(this, "Tagalog translation is not available yet.", android.widget.Toast.LENGTH_LONG).show()
        }
        findViewById<Button>(R.id.btnEnglish).setOnClickListener {
            getSharedPreferences("settings", MODE_PRIVATE).edit().putString("language", "en").apply()
        }

        val btnContinue = findViewById<Button>(R.id.btnContinue)
        btnContinue.setOnClickListener {
            startActivity(Intent(this, GetStartedActivity::class.java))
            finish()
        }
    }
}
