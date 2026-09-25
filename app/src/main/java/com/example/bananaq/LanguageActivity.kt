package com.example.bananaq

import android.content.Intent
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.example.bananaq.R

class LanguageActivity : LocaleAwareActivity() {

    private lateinit var btnEnglish: View
    private lateinit var btnTagalog: View
    private lateinit var labelEnglish: TextView
    private lateinit var labelTagalog: TextView
    private var selectedLanguage: String = "en"
    private var isTransitioning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_language)
        
        applySystemInsets()

        btnEnglish = findViewById(R.id.btnEnglish)
        btnTagalog = findViewById(R.id.btnTagalog)
        labelEnglish = findViewById(R.id.labelEnglish)
        labelTagalog = findViewById(R.id.labelTagalog)
        selectedLanguage = LocaleHelper.selectedLanguage(this)
        val btnContinue = findViewById<Button>(R.id.btnContinue)

        btnEnglish.setOnClickListener {
            selectLanguage("en")
        }

        btnTagalog.setOnClickListener {
            selectLanguage("tl")
        }

        btnContinue.setOnClickListener {
            if (isTransitioning) return@setOnClickListener
            isTransitioning = true
            LocaleHelper.saveLanguage(this, selectedLanguage)
            btnEnglish.isEnabled = false
            btnTagalog.isEnabled = false
            btnContinue.isEnabled = false

            val overlay = findViewById<View>(R.id.beigeTransitionOverlay)
            overlay.visibility = View.VISIBLE
            val overlayPosition = IntArray(2)
            val buttonPosition = IntArray(2)
            overlay.getLocationOnScreen(overlayPosition)
            btnContinue.getLocationOnScreen(buttonPosition)
            val centerX = buttonPosition[0] - overlayPosition[0] + btnContinue.width / 2
            val centerY = buttonPosition[1] - overlayPosition[1] + btnContinue.height / 2
            val farthestX = maxOf(centerX, overlay.width - centerX)
            val farthestY = maxOf(centerY, overlay.height - centerY)
            val radius = kotlin.math.hypot(farthestX.toDouble(), farthestY.toDouble()).toFloat()
            ViewAnimationUtils.createCircularReveal(overlay, centerX, centerY, 0f, radius).apply {
                duration = 650L
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        startActivity(Intent(this@LanguageActivity, GetStartedActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                })
                start()
            }
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
            labelEnglish.setTextColor(ContextCompat.getColor(this, R.color.white))

            // Tagalog Inactive
            btnTagalog.setBackgroundResource(R.drawable.btn_language_yellow)
            labelTagalog.setTextColor(ContextCompat.getColor(this, R.color.banana_text_dark))
        } else {
            // Tagalog Active
            btnTagalog.setBackgroundResource(R.drawable.btn_language_green)
            labelTagalog.setTextColor(ContextCompat.getColor(this, R.color.white))

            // English Inactive
            btnEnglish.setBackgroundResource(R.drawable.btn_language_yellow)
            labelEnglish.setTextColor(ContextCompat.getColor(this, R.color.banana_text_dark))
        }
    }
}
