package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.example.bananaq.R

class LanguageActivity : AppCompatActivity() {

    private lateinit var btnEnglish: Button
    private lateinit var btnTagalog: Button
    private var selectedLanguage: String = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_language)
        
        applySystemInsets()

        btnEnglish = findViewById(R.id.btnEnglish)
        btnTagalog = findViewById(R.id.btnTagalog)
        val btnContinue = findViewById<Button>(R.id.btnContinue)

        btnEnglish.setOnClickListener {
            selectLanguage("en")
        }

        btnTagalog.setOnClickListener {
            selectLanguage("tl")
        }

        btnContinue.setOnClickListener {
            getSharedPreferences("settings", MODE_PRIVATE).edit().putString("language", selectedLanguage).apply()
            startActivity(Intent(this, GetStartedActivity::class.java))
            finish()
        }

        // Initialize with default language
        updateButtonStates()
    }

    private fun selectLanguage(lang: String) {
        selectedLanguage = lang
        updateButtonStates()
    }

    private fun updateButtonStates() {
        if (selectedLanguage == "en") {
            // English Active
            btnEnglish.setBackgroundResource(R.drawable.btn_language_green)
            btnEnglish.setTextColor(ContextCompat.getColor(this, R.color.white))

            // Tagalog Inactive
            btnTagalog.setBackgroundResource(R.drawable.btn_language_yellow)
            btnTagalog.setTextColor(ContextCompat.getColor(this, R.color.banana_text_dark))
        } else {
            // Tagalog Active
            btnTagalog.setBackgroundResource(R.drawable.btn_language_green)
            btnTagalog.setTextColor(ContextCompat.getColor(this, R.color.white))

            // English Inactive
            btnEnglish.setBackgroundResource(R.drawable.btn_language_yellow)
            btnEnglish.setTextColor(ContextCompat.getColor(this, R.color.banana_text_dark))
        }
    }
}
