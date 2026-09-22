package com.example.bananaq

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class UserAgreement : AppCompatActivity() {
    override fun onResume() {
        super.onResume()
        val language = getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("language", "en")
        findViewById<TextView>(R.id.btnLanguage).setText(
            if (language == "tl") R.string.language_tagalog else R.string.language_english
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_agreement)
        
        val mainView = findViewById<android.view.View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val cbConfirmation = findViewById<CheckBox>(R.id.cbConfirmation)
        val btnDecline = findViewById<Button>(R.id.btnDecline)
        val btnSubmit = findViewById<Button>(R.id.btnAccept) // id is btnAccept in XML, text is Submit

        cbConfirmation.setOnCheckedChangeListener { _, isChecked ->
            btnSubmit.isEnabled = isChecked
            btnSubmit.alpha = if (isChecked) 1.0f else 0.5f
        }

        btnDecline.setOnClickListener {
            finish() // returns to GetStartedActivity
        }

        btnSubmit.setOnClickListener {
            // Save onboarding completion
            getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
            
            // Navigate to MainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
