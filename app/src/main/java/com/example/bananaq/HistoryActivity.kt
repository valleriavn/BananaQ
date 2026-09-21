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
        applySystemInsets()

        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        refreshScans()
    }

    private fun refreshScans() {
        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)
        val emptyState = findViewById<View>(R.id.emptyState)

        val historyItems = data.ScanHistoryStore(this).items()

        if (historyItems.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            rvHistory.layoutManager = LinearLayoutManager(this)
            rvHistory.adapter = ScanHistoryAdapter(historyItems) { item ->
                startActivity(Intent(this, ScannerActivity::class.java).apply {
                    putExtra("DISEASE_NAME", item.diseaseName)
                    putExtra("CONFIDENCE", item.accuracy?.toIntOrNull() ?: 0)
                })
            }
        }

    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_history
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
