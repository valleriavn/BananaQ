package com.example.bananaq

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class DiseaseDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_disease_details)
        applySystemInsets()
        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        val name = intent.getStringExtra("DISEASE_NAME")
        val disease = libraryDiseases.firstOrNull { it.name == name }
        if (disease == null) {
            Toast.makeText(this, R.string.disease_details_unavailable, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        findViewById<TextView>(R.id.detailDiseaseName).text = disease.name
        findViewById<TextView>(R.id.detailDescription).setText(disease.description)
        val language = getSharedPreferences("settings", MODE_PRIVATE).getString("language", "en")
        findViewById<TextView>(R.id.detailLanguage).setText(
            if (language == "tl") R.string.language_tagalog else R.string.language_english
        )
    }
}
