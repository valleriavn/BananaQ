package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class FeedbackDetailActivity : AppCompatActivity() {

    private var selectedRating = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_feedback_detail)
        applySystemInsets()

        val diseaseName = intent.getStringExtra("DISEASE_NAME") ?: "Unknown"
        val dateTime = intent.getStringExtra("DATE_TIME") ?: ""
        val accuracy = intent.getStringExtra("ACCURACY") ?: ""

        findViewById<TextView>(R.id.tvDiseaseName).text = diseaseName
        findViewById<TextView>(R.id.tvDateTime).text = dateTime
        findViewById<TextView>(R.id.tvAccuracy).text = "$accuracy%"

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        selectedRating = savedInstanceState?.getString("rating") ?: ""
        setupRatingOptions()

        findViewById<View>(R.id.btnSubmitFeedback).setOnClickListener {
            if (selectedRating.isEmpty()) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
            } else {
                val scanId = intent.getStringExtra("SCAN_ID")
                if (scanId.isNullOrEmpty()) {
                    Toast.makeText(this, "Select a scan from your history first.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val feedback = org.json.JSONObject().apply {
                    put("rating", selectedRating)
                    put("comments", findViewById<EditText>(R.id.etComments).text.toString().trim())
                    put("time", System.currentTimeMillis())
                }
                getSharedPreferences("scan_feedback", MODE_PRIVATE).edit()
                    .putString(scanId, feedback.toString()).apply()
                Toast.makeText(this, "Feedback saved on this device.", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        setupBottomNavigation()
    }

    private fun setupRatingOptions() {
        val ratings = mapOf(
            R.id.rateVeryAccurate to "Very Accurate",
            R.id.rateAccurate to "Accurate",
            R.id.rateNotSure to "Not Sure",
            R.id.rateInaccurate to "Inaccurate",
            R.id.rateVeryInaccurate to "Very Inaccurate"
        )

        ratings.forEach { (viewId, ratingName) ->
            findViewById<View>(viewId).setOnClickListener {
                selectedRating = ratingName
                // Reset all backgrounds
                ratings.keys.forEach { id ->
                    findViewById<View>(id).setBackgroundResource(0)
                }
                // Highlight selected
                findViewById<View>(viewId).setBackgroundResource(R.drawable.rounded_button_bg)
            }
            if (selectedRating == ratingName) findViewById<View>(viewId).setBackgroundResource(R.drawable.rounded_button_bg)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("rating", selectedRating)
        super.onSaveInstanceState(outState)
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_feedback
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                    finish()
                    true
                }
                R.id.nav_scan -> {
                    startActivity(Intent(this, ScannerActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_feedback -> {
                    finish()
                    true
                }
                R.id.nav_account -> {
                    startActivity(Intent(this, AccountActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }
}
