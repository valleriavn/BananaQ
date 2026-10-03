package com.example.bananaq

import com.example.bananaq.data.ScanHistoryStore

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.widget.NestedScrollView
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
                if (!item.isValid) {
                    showUnidentifiedScanDialog()
                } else {
                    startActivity(Intent(this, ScannerActivity::class.java).apply {
                        putExtra("FROM_HISTORY", true)
                        putExtra("SCAN_ID", item.scanId)
                        putExtra("DISEASE_NAME", item.diseaseName)
                        putExtra("CONFIDENCE", item.accuracy?.toFloatOrNull()?.toInt() ?: 0)
                        putExtra("RESULT_VALID", item.isValid)
                        putExtra("IMAGE_URI", item.imageUri)
                    })
                }
            }
            rvHistory.layoutManager?.onRestoreInstanceState(scrollState)
        }

    }

    private fun showUnidentifiedScanDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val content = layoutInflater.inflate(R.layout.dialog_unidentified_scan, null)
        val scroll = NestedScrollView(this).apply { addView(content) }
        dialog.setContentView(scroll)
        dialog.setCanceledOnTouchOutside(true)
        content.findViewById<View>(R.id.btnCancelRescan).setOnClickListener {
            dialog.dismiss()
        }
        content.findViewById<View>(R.id.btnConfirmRescan).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, ScannerActivity::class.java))
        }
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = 0.58f }
            setGravity(Gravity.CENTER)
        }
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    private fun setupBottomNavigation() {
        configureBottomNavigation(R.id.nav_history)
    }
}