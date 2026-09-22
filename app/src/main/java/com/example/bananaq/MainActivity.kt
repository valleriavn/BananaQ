package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var selectButton: LinearLayout
    private lateinit var cameraButton: LinearLayout
    private lateinit var detectButton: LinearLayout
    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var dayMonthText: TextView
    

    private val handler = Handler(Looper.getMainLooper())
    private val timeUpdater = object : Runnable {
        override fun run() {
            updateDateTime()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        applySystemInsets()

        selectButton = findViewById(R.id.selectButton)
        cameraButton = findViewById(R.id.cameraButton)
        detectButton = findViewById(R.id.detectButton)
        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)
        dayMonthText = findViewById(R.id.dayMonthText)

        // Disease Library Card Listeners
        findViewById<View>(R.id.cardBlackSigatoka).setOnClickListener {
            showDiseaseDetails("Black Sigatoka")
        }
        findViewById<View>(R.id.cardPanama).setOnClickListener {
            showDiseaseDetails("Panama Disease")
        }
        findViewById<View>(R.id.cardCordana).setOnClickListener {
            showDiseaseDetails("Cordana Leaf Spot")
        }

        selectButton.setOnClickListener { openScanner("gallery") }
        cameraButton.setOnClickListener { openScanner("camera") }

        detectButton.setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        setupLanguageToggle()
        setupBottomNavigation()
    }

    private fun setupLanguageToggle() {
        val tvLangEN = findViewById<TextView>(R.id.tvLangEN)
        val tvLangTL = findViewById<TextView>(R.id.tvLangTL)

        // Read current language from preferences
        val sharedPrefs = getSharedPreferences("settings", MODE_PRIVATE)
        var currentLang = sharedPrefs.getString("language", "en") ?: "en"

        fun updateToggleUI(lang: String) {
            if (lang == "en") {
                tvLangEN.setBackgroundResource(R.drawable.rounded_button_bg)
                tvLangEN.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.banana_green)
                )
                tvLangEN.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white))
                tvLangEN.setTypeface(null, android.graphics.Typeface.BOLD)

                tvLangTL.background = null
                tvLangTL.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.banana_yellow))
                tvLangTL.setTypeface(null, android.graphics.Typeface.NORMAL)
            } else {
                tvLangTL.setBackgroundResource(R.drawable.rounded_button_bg)
                tvLangTL.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.banana_green)
                )
                tvLangTL.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white))
                tvLangTL.setTypeface(null, android.graphics.Typeface.BOLD)

                tvLangEN.background = null
                tvLangEN.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.banana_yellow))
                tvLangEN.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }

        // Initialize UI based on saved preference
        updateToggleUI(currentLang)

        tvLangEN.setOnClickListener {
            if (currentLang != "en") {
                currentLang = "en"
                sharedPrefs.edit().putString("language", "en").apply()
                updateToggleUI("en")
                // Here you would typically trigger activity recreation or string updates
            }
        }

        tvLangTL.setOnClickListener {
            if (currentLang != "tl") {
                currentLang = "tl"
                sharedPrefs.edit().putString("language", "tl").apply()
                updateToggleUI("tl")
                // Here you would typically trigger activity recreation or string updates
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_home
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.nav_scan -> {
                    startActivity(Intent(this, ScannerActivity::class.java))
                    true
                }
                R.id.nav_feedback -> {
                    startActivity(Intent(this, FeedbackActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun updateDateTime() {
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE, yyyy", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMMM dd", Locale.getDefault())

        timeText.text = timeFormat.format(calendar.time)
        dateText.text = dayFormat.format(calendar.time)
        dayMonthText.text = monthFormat.format(calendar.time)
    }

    private fun openScanner(source: String) {
        startActivity(Intent(this, ScannerActivity::class.java).putExtra("SOURCE", source))
    }

    private fun showDiseaseDetails(diseaseName: String) {
        val intent = Intent(this, ScannerActivity::class.java).apply {
            putExtra("DISEASE_NAME", diseaseName)
            putExtra("LIBRARY", true)
        }
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_home
        
        // Refresh language toggle in case it was changed elsewhere
        setupLanguageToggle()
        
        handler.post(timeUpdater)
    }

    override fun onStop() {
        handler.removeCallbacks(timeUpdater)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeUpdater)
    }
}
