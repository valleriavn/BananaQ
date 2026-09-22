package com.example.bananaq

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

internal enum class ManualTopic(val title: Int, val body: Int) {
    HOW_TO(R.string.manual_how_to, R.string.manual_how_to_body),
    SCAN(R.string.manual_scan_leaf, R.string.manual_scan_leaf_body),
    RESULTS(R.string.manual_understand, R.string.manual_understand_body),
    TREATMENT(R.string.manual_treatment, R.string.manual_treatment_body),
    PREVENTION(R.string.manual_prevention, R.string.manual_prevention_body),
    PHOTO(R.string.manual_photo, R.string.manual_photo_body)
}

class ManualTopicActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val topic = ManualTopic.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_TOPIC) }
        if (topic == null) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_manual_topic)
        applySystemInsets()
        findViewById<TextView>(R.id.topicTitle).setText(topic.title)
        findViewById<TextView>(R.id.topicBody).setText(topic.body)
        findViewById<View>(R.id.backButton).setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_TOPIC = "manual_topic"
    }
}
