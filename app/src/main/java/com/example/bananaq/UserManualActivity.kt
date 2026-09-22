package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UserManualActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_manual)
        applySystemInsets()
        findViewById<View>(R.id.backButton).setOnClickListener { finish() }
        mapOf(
            R.id.howToRow to ManualTopic.HOW_TO,
            R.id.scanLeafRow to ManualTopic.SCAN,
            R.id.understandRow to ManualTopic.RESULTS,
            R.id.treatmentRow to ManualTopic.TREATMENT,
            R.id.preventionRow to ManualTopic.PREVENTION,
            R.id.photoRow to ManualTopic.PHOTO
        ).forEach { (id, topic) ->
            findViewById<View>(id).setOnClickListener {
                startActivity(Intent(this, ManualTopicActivity::class.java)
                    .putExtra(ManualTopicActivity.EXTRA_TOPIC, topic.name))
            }
        }
    }
}

internal fun AppCompatActivity.bindLegalLinks() {
    listOf(R.id.termsRow to R.string.terms_of_use,
        R.id.privacyRow to R.string.privacy_policy,
        R.id.agreementRow to R.string.user_agreement).forEach { (id, title) ->
        findViewById<View>(id).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(R.string.legal_not_available)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
}

