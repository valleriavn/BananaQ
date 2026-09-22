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
        mapOf(
            R.id.termsRow to UserAgreement.SECTION_TERMS,
            R.id.privacyRow to UserAgreement.SECTION_PRIVACY,
            R.id.agreementRow to UserAgreement.SECTION_AGREEMENT
        ).forEach { (id, section) ->
            findViewById<View>(id).setOnClickListener {
                startActivity(Intent(this, UserAgreement::class.java)
                    .putExtra(UserAgreement.EXTRA_SECTION, section))
            }
        }
    }
}
