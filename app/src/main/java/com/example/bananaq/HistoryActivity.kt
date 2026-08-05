package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)
        val emptyState = findViewById<View>(R.id.emptyState)

        // Mock data to match the design "History Eng Not Empty"
        val historyItems = listOf(
            ScanHistoryAdapter.HistoryItem(true, headerTitle = "Today"),
            ScanHistoryAdapter.HistoryItem(false, diseaseName = "Black Sigatoka", dateTime = "May 15, 2026 at 2:15 PM", accuracy = "95.8", isHealthy = false),
            
            ScanHistoryAdapter.HistoryItem(true, headerTitle = "Yesterday"),
            ScanHistoryAdapter.HistoryItem(false, diseaseName = "Healthy Leaf", dateTime = "May 14, 2026 at 11:10 AM", accuracy = "98.2", isHealthy = true),
            ScanHistoryAdapter.HistoryItem(false, diseaseName = "Panama Disease", dateTime = "May 14, 2026 at 3:45 PM", accuracy = "92.5", isHealthy = false),
            ScanHistoryAdapter.HistoryItem(false, diseaseName = "Cordana Disease", dateTime = "May 14, 2026 at 5:30 PM", accuracy = "89.4", isHealthy = false),
            
            ScanHistoryAdapter.HistoryItem(true, headerTitle = "Last 3 days"),
            ScanHistoryAdapter.HistoryItem(false, diseaseName = "Healthy Leaf", dateTime = "May 13, 2026 at 9:00 AM", accuracy = "99.1", isHealthy = true),
            ScanHistoryAdapter.HistoryItem(false, diseaseName = "Panama Disease", dateTime = "May 13, 2026 at 2:00 PM", accuracy = "91.8", isHealthy = false),
            ScanHistoryAdapter.HistoryItem(false, diseaseName = "Black Sigatoka", dateTime = "May 13, 2026 at 4:20 PM", accuracy = "94.5", isHealthy = false),
            ScanHistoryAdapter.HistoryItem(false, diseaseName = "Cordana Disease", dateTime = "May 12, 2026 at 10:15 AM", accuracy = "87.6", isHealthy = false)
        )

        if (historyItems.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            rvHistory.layoutManager = LinearLayoutManager(this)
            rvHistory.adapter = ScanHistoryAdapter(historyItems)
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_history
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
                R.id.nav_history -> true
                R.id.nav_feedback -> {
                    startActivity(Intent(this, FeedbackActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
