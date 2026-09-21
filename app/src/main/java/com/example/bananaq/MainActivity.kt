package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var selectButton: LinearLayout
    private lateinit var cameraButton: LinearLayout
    private lateinit var detectButton: LinearLayout
    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var dayMonthText: TextView
    

    private val handler = Handler(Looper.getMainLooper())
    private val timeUpdater = object : Runnable {
        override fun run() {
            updateDateTime()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        applySystemInsets()

        selectButton = findViewById(R.id.selectButton)
        cameraButton = findViewById(R.id.cameraButton)
        detectButton = findViewById(R.id.detectButton)
        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)
        dayMonthText = findViewById(R.id.dayMonthText)

        // Disease Library Card Listeners
        findViewById<View>(R.id.cardBlackSigatoka).setOnClickListener {
            showDiseaseDetails("Black Sigatoka")
        }
        findViewById<View>(R.id.cardPanama).setOnClickListener {
            showDiseaseDetails("Panama Disease")
        }
        findViewById<View>(R.id.cardCordana).setOnClickListener {
            showDiseaseDetails("Cordana Leaf Spot")
        }

        selectButton.setOnClickListener { openScanner("gallery") }
        cameraButton.setOnClickListener { openScanner("camera") }

        detectButton.setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_home
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.nav_scan -> {
                    startActivity(Intent(this, ScannerActivity::class.java))
                    true
                }
                R.id.nav_feedback -> {
                    startActivity(Intent(this, FeedbackActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun updateDateTime() {
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE, yyyy", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMMM dd", Locale.getDefault())

        timeText.text = timeFormat.format(calendar.time)
        dateText.text = dayFormat.format(calendar.time)
        dayMonthText.text = monthFormat.format(calendar.time)
    }

    private fun openScanner(source: String) {
        startActivity(Intent(this, ScannerActivity::class.java).putExtra("SOURCE", source))
    }

    private fun showDiseaseDetails(diseaseName: String) {
        val intent = Intent(this, ScannerActivity::class.java).apply {
            putExtra("DISEASE_NAME", diseaseName)
            putExtra("LIBRARY", true)
        }
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_home
        handler.post(timeUpdater)
    }

    override fun onStop() {
        handler.removeCallbacks(timeUpdater)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeUpdater)
    }
}
