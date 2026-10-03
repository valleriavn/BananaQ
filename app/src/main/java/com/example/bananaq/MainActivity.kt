package com.example.bananaq

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : LocaleAwareActivity() {

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
        UserActionLogger.log("screen_view", mapOf("screen" to "MainActivity"))

        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)
        dayMonthText = findViewById(R.id.dayMonthText)

        findViewById<RecyclerView>(R.id.diseaseCarousel).apply {
            val carouselAdapter = DiseaseLibraryAdapter(::showDiseaseDetails)
            val manager = LinearLayoutManager(this@MainActivity, RecyclerView.HORIZONTAL, false)
            layoutManager = manager
            adapter = carouselAdapter
            LinearSnapHelper().attachToRecyclerView(this)
            if (savedInstanceState == null) {
                manager.scrollToPositionWithOffset(carouselAdapter.initialPosition, 0)
            }
        }

        animatePlantIllustration(findViewById(R.id.ivSoil))

        setupLanguageToggle()
        setupBottomNavigation()
    }

    private fun setupLanguageToggle() {
        val tvLangEN = findViewById<TextView>(R.id.tvLangEN)
        val tvLangTL = findViewById<TextView>(R.id.tvLangTL)

        val sharedPrefs = getSharedPreferences("settings", MODE_PRIVATE)
        var currentLang = sharedPrefs.getString("language", "en") ?: "en"

        fun updateToggleUI(lang: String) {
            tvLangEN.isSelected = lang == "en"
            tvLangTL.isSelected = lang == "tl"
            if (lang == "en") {
                tvLangEN.setBackgroundResource(R.drawable.btn_language_green)
                tvLangEN.setTextColor(ContextCompat.getColor(this, R.color.white))
                tvLangEN.setTypeface(null, Typeface.NORMAL)

                tvLangTL.background = null
                tvLangTL.setTextColor(ContextCompat.getColor(this, R.color.button_text_black))
                tvLangTL.setTypeface(null, Typeface.NORMAL)
            } else {
                tvLangTL.setBackgroundResource(R.drawable.btn_language_green)
                tvLangTL.setTextColor(ContextCompat.getColor(this, R.color.white))
                tvLangTL.setTypeface(null, Typeface.NORMAL)

                tvLangEN.background = null
                tvLangEN.setTextColor(ContextCompat.getColor(this, R.color.button_text_black))
                tvLangEN.setTypeface(null, Typeface.NORMAL)
            }
        }

        updateToggleUI(currentLang)

        tvLangEN.setOnClickListener {
            if (currentLang != "en") {
                currentLang = "en"
                UserActionLogger.log("language_changed", mapOf("language" to "en"))
                LocaleHelper.saveLanguage(this, "en")
                recreate()
            }
        }

        tvLangTL.setOnClickListener {
            if (currentLang != "tl") {
                currentLang = "tl"
                UserActionLogger.log("language_changed", mapOf("language" to "tl"))
                LocaleHelper.saveLanguage(this, "tl")
                recreate()
            }
        }
    }

    private fun setupBottomNavigation() {
        configureBottomNavigation(R.id.nav_home)
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

    private fun showDiseaseDetails(diseaseName: String) {
        UserActionLogger.log("disease_card_clicked", mapOf("diseaseName" to diseaseName))
        val intent = Intent(this, DiseaseDetailsActivity::class.java).apply {
            putExtra("DISEASE_NAME", diseaseName)
        }
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        findViewById<RaisedBottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_home

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
