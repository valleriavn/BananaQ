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
        findViewById<TextView>(R.id.tvAccuracy).visibility =
            if (intent.getBooleanExtra("RESULT_VALID", true)) View.VISIBLE else View.GONE
        intent.getStringExtra("IMAGE_URI")?.let {
            findViewById<android.widget.ImageView>(R.id.ivScanDetail).setImageURI(android.net.Uri.parse(it))
        }

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

        val faces = listOf("☺", "☺", "😐", "☹", "☹")
        fun renderRatings() {
            ratings.entries.forEachIndexed { index, (id, name) ->
                val option = findViewById<android.widget.LinearLayout>(id)
                val selected = selectedRating == name
                val color = androidx.core.content.ContextCompat.getColor(this,
                    if (!selected) R.color.banana_yellow else when(index) {
                        0, 1 -> R.color.banana_green
                        2 -> R.color.banana_muted
                        else -> R.color.rating_negative
                    })
                option.background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 12 * resources.displayMetrics.density
                    setColor(androidx.core.content.ContextCompat.getColor(this@FeedbackDetailActivity, R.color.banana_surface))
                    setStroke((resources.displayMetrics.density * if (selected) 2 else 1).toInt(), color)
                }
                (option.getChildAt(0) as TextView).setTextColor(color)
                (option.getChildAt(1) as TextView).setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.banana_body))
            }
        }
        ratings.entries.forEachIndexed { index, (viewId, ratingName) ->
            val option = findViewById<android.widget.LinearLayout>(viewId)
            val oldIcon = option.getChildAt(0)
            option.removeView(oldIcon)
            option.addView(TextView(this).apply {
                text = faces[index]
                textSize = 30f
                gravity = android.view.Gravity.CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, 0, oldIcon.layoutParams)
            option.contentDescription = ratingName
            val params = option.layoutParams as android.widget.GridLayout.LayoutParams
            params.columnSpec = android.widget.GridLayout.spec(
                if (index < 2) index * 3 else (index - 2) * 2,
                if (index < 2) 3 else 2, 1f)
            params.rowSpec = android.widget.GridLayout.spec(if (index < 2) 0 else 1)
            option.layoutParams = params
            findViewById<View>(viewId).setOnClickListener {
                selectedRating = ratingName
                renderRatings()
            }
        }
        renderRatings()
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
