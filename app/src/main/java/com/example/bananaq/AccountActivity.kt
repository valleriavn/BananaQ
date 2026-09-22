package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class AccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account)
        applySystemInsets()
        findViewById<View>(R.id.backButton).setOnClickListener { finish() }
        findViewById<View>(R.id.manualRow).setOnClickListener {
            startActivity(Intent(this, UserManualActivity::class.java))
        }
        bindLegalLinks()
    }
}
