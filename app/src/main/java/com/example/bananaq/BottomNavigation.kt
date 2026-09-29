package com.example.bananaq

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

/** One navigation policy shared by every top-level screen. */
fun AppCompatActivity.configureBottomNavigation(activeItemId: Int) {
    val navigation = findViewById<RaisedBottomNavigationView>(R.id.bottomNavigation)
    navigation.selectedItemId = activeItemId
    navigation.setOnItemSelectedListener { itemId ->
        if (itemId == activeItemId) return@setOnItemSelectedListener true
        val destination = when (itemId) {
            R.id.nav_home -> MainActivity::class.java
            R.id.nav_history -> HistoryActivity::class.java
            R.id.nav_scan -> ScannerActivity::class.java
            R.id.nav_feedback -> FeedbackActivity::class.java
            R.id.nav_account -> AccountActivity::class.java
            else -> return@setOnItemSelectedListener false
        }
        startActivity(Intent(this, destination).addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        ))
        // The full-screen scanner behaves like a temporary camera task, so
        // keep the current destination underneath it for a natural Back flow.
        if (itemId != R.id.nav_scan) finish()
        // Bottom destinations are peers, so a fade-through feels steadier than
        // the directional transition used for opening a detail screen.
        overridePendingTransition(R.anim.nav_fade_enter, R.anim.nav_fade_exit)
        true
    }
}
