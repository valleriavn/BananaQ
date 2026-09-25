package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class UserManualActivity : LocaleAwareActivity() {
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
