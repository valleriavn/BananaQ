package com.example.bananaq

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat

class DiseaseDetailsActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_disease_details)
        applySystemInsets()
        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        val name = intent.getStringExtra("DISEASE_NAME")
        val disease = libraryDiseases.firstOrNull { it.key == name }
        if (disease == null) {
            Toast.makeText(this, R.string.disease_details_unavailable, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        findViewById<TextView>(R.id.detailDiseaseName).setText(disease.name)
        findViewById<ImageView>(R.id.detailDiseaseImage).apply {
            setImageResource(disease.image)
            contentDescription = getString(
                R.string.disease_image_content_description,
                getString(disease.name)
            )
        }
        findViewById<TextView>(R.id.detailScientificName).setText(disease.scientificName)
        findViewById<TextView>(R.id.detailDescription).setText(disease.description)
        findViewById<TextView>(R.id.detailSymptoms).setText(disease.symptoms)
        findViewById<TextView>(R.id.detailSpread).setText(disease.spread)
        setupLanguageToggle()
    }

    private fun setupLanguageToggle() {
        val english = findViewById<TextView>(R.id.detailLangEN)
        val tagalog = findViewById<TextView>(R.id.detailLangTL)
        val currentLanguage = LocaleHelper.selectedLanguage(this)

        fun renderSelection(selectedLanguage: String) {
            english.isSelected = selectedLanguage == "en"
            tagalog.isSelected = selectedLanguage == "tl"
            val activeColor = ContextCompat.getColor(this, R.color.white)
            val inactiveColor = ContextCompat.getColor(this, R.color.button_text_black)

            english.setBackgroundResource(
                if (selectedLanguage == "en") R.drawable.btn_language_green
                else android.R.color.transparent
            )
            english.setTextColor(if (selectedLanguage == "en") activeColor else inactiveColor)

            tagalog.setBackgroundResource(
                if (selectedLanguage == "tl") R.drawable.btn_language_green
                else android.R.color.transparent
            )
            tagalog.setTextColor(if (selectedLanguage == "tl") activeColor else inactiveColor)
        }

        renderSelection(currentLanguage)
        english.setOnClickListener { changeAppLanguage("en") }
        tagalog.setOnClickListener { changeAppLanguage("tl") }
    }
}
