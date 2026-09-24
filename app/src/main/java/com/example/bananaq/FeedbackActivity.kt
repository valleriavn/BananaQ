package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class FeedbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_feedback)
        applySystemInsets()

        setupLanguageToggle()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        refreshScans()
    }

    private fun refreshScans() {
        val rvFeedback = findViewById<RecyclerView>(R.id.rvFeedbackSelection)
        val emptyState = findViewById<View>(R.id.emptyState)

        val feedbackItems = data.ScanHistoryStore(this).items()

        if (feedbackItems.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvFeedback.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvFeedback.visibility = View.VISIBLE
            rvFeedback.layoutManager = LinearLayoutManager(this)
            
            val adapter = ScanHistoryAdapter(feedbackItems) { item ->
                val intent = Intent(this, FeedbackDetailActivity::class.java).apply {
                    putExtra("SCAN_ID", item.scanId)
                    putExtra("DISEASE_NAME", item.diseaseName)
                    putExtra("DATE_TIME", item.dateTime)
                    putExtra("ACCURACY", item.accuracy)
                    putExtra("IMAGE_URI", item.imageUri)
                    putExtra("RESULT_VALID", item.isValid)
                }
                startActivity(intent)
            }
            rvFeedback.adapter = adapter
        }

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
                R.id.nav_feedback -> true
                R.id.nav_account -> {
                    startActivity(Intent(this, AccountActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }
}
