package bananaq

import bananaq.data.BananaQDatabase
import bananaq.data.ScanHistoryStore

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
            val scrollState = rvFeedback.layoutManager?.onSaveInstanceState()
            if (rvFeedback.layoutManager == null) rvFeedback.layoutManager = LinearLayoutManager(this)
            
            val adapter = ScanHistoryAdapter(feedbackItems, submittedScanIds) { item ->
                startActivity(FeedbackDetailActivity.intentForScan(this, item))
            }
            rvFeedback.adapter = adapter
            rvFeedback.layoutManager?.onRestoreInstanceState(scrollState)
        }

    }

    private fun setupBottomNavigation() {
        configureBottomNavigation(R.id.nav_feedback)
    }
}
