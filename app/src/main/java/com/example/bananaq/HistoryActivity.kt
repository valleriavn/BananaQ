package com.example.bananaq

import com.example.bananaq.data.ScanHistoryStore

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : LocaleAwareActivity() {

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

        val historyItems = ScanHistoryStore(this).items()

        if (historyItems.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            val scrollState = rvHistory.layoutManager?.onSaveInstanceState()
            if (rvHistory.layoutManager == null) rvHistory.layoutManager = LinearLayoutManager(this)
            rvHistory.adapter = ScanHistoryAdapter(historyItems) { item ->
                startActivity(Intent(this, ScannerActivity::class.java).apply {
                    putExtra("DISEASE_NAME", item.diseaseName)
                    putExtra("CONFIDENCE", item.accuracy?.toFloatOrNull()?.toInt() ?: 0)
                    putExtra("RESULT_VALID", item.isValid)
                    putExtra("IMAGE_URI", item.imageUri)
                })
            }
            rvHistory.layoutManager?.onRestoreInstanceState(scrollState)
        }

    }

    private fun setupBottomNavigation() {
        configureBottomNavigation(R.id.nav_history)
    }
}
