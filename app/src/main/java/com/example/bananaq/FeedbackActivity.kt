package com.example.bananaq

import com.example.bananaq.data.BananaQDatabase
import com.example.bananaq.data.ScanHistoryStore

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FeedbackActivity : LocaleAwareActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_feedback)
        applySystemInsets()

        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        refreshScans()
    }

    private fun refreshScans() {
        val rvFeedback = findViewById<RecyclerView>(R.id.rvFeedbackSelection)
        val emptyState = findViewById<View>(R.id.emptyState)

        val feedbackItems = ScanHistoryStore(this).items()
        val submittedScanIds = BananaQDatabase.get(this).feedbackScanIds()

        if (feedbackItems.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvFeedback.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvFeedback.visibility = View.VISIBLE
            rvFeedback.layoutManager = LinearLayoutManager(this)
            
            val adapter = ScanHistoryAdapter(feedbackItems, submittedScanIds) { item ->
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
        configureBottomNavigation(R.id.nav_feedback)
    }
}
