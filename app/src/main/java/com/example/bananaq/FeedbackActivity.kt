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

        val rvFeedback = findViewById<RecyclerView>(R.id.rvFeedbackSelection)
        val emptyState = findViewById<View>(R.id.emptyState)

        // Mock data similar to History
        val feedbackItems = listOf(
            ScanHistoryAdapter.HistoryItem(true, headerTitle = "Today"),
            ScanHistoryAdapter.HistoryItem(false, diseaseName = "Black Sigatoka", dateTime = "May 15, 2026 at 2:15 PM", accuracy = "95.8", isHealthy = false),
            
            ScanHistoryAdapter.HistoryItem(true, headerTitle = "Yesterday"),
            ScanHistoryAdapter.HistoryItem(false, diseaseName = "Healthy Leaf", dateTime = "May 14, 2026 at 11:10 AM", accuracy = "98.2", isHealthy = true),
            ScanHistoryAdapter.HistoryItem(false, diseaseName = "Panama Disease", dateTime = "May 14, 2026 at 3:45 PM", accuracy = "92.5", isHealthy = false),
            
            ScanHistoryAdapter.HistoryItem(true, headerTitle = "Last 3 days"),
            ScanHistoryAdapter.HistoryItem(false, diseaseName = "Cordana Disease", dateTime = "May 12, 2026 at 10:15 AM", accuracy = "87.6", isHealthy = false)
        )

        if (feedbackItems.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvFeedback.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvFeedback.visibility = View.VISIBLE
            rvFeedback.layoutManager = LinearLayoutManager(this)
            
            val adapter = ScanHistoryAdapter(feedbackItems) { item ->
                val intent = Intent(this, FeedbackDetailActivity::class.java).apply {
                    putExtra("DISEASE_NAME", item.diseaseName)
                    putExtra("DATE_TIME", item.dateTime)
                    putExtra("ACCURACY", item.accuracy)
                }
                startActivity(intent)
            }
            rvFeedback.adapter = adapter
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_feedback
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
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
                else -> false
            }
        }
    }
}
