package com.example.bananaq

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge

class UserAgreementActivity : LocaleAwareActivity() {
    override fun onResume() {
        super.onResume()
        val language = getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("language", "en")
        findViewById<TextView>(R.id.btnLanguage).setText(
            if (language == "tl") R.string.language_tagalog else R.string.language_english
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_agreement)
        if (LocaleHelper.selectedLanguage(this) == "tl") {
            localizeAgreementToTagalog(findViewById(android.R.id.content))
        }
        
        applySystemInsets()

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val sectionTitle = when (intent.getStringExtra(EXTRA_SECTION)) {
            SECTION_TERMS -> R.id.titleTermsOfUse
            SECTION_PRIVACY -> R.id.titlePrivacyPolicy
            SECTION_AGREEMENT -> R.id.titleUserAgreement
            else -> null
        }
        if (sectionTitle != null) {
            val documentTitle = findViewById<TextView>(sectionTitle).text
            // Only the selected document participates in scrolling and accessibility.
            mapOf(
                R.id.titleUserAgreement to R.id.userAgreementContent,
                R.id.titleTermsOfUse to R.id.termsOfUseContent,
                R.id.titlePrivacyPolicy to R.id.privacyPolicyContent
            ).forEach { (title, content) ->
                val visibility = if (title == sectionTitle) View.VISIBLE else View.GONE
                findViewById<View>(title).visibility = View.GONE
                findViewById<View>(content).visibility = visibility
            }
            findViewById<TextView>(R.id.titleUserAgreement).apply {
                text = documentTitle
                visibility = View.VISIBLE
            }
            findViewById<View>(R.id.agreementConfirmation).visibility = View.GONE
            findViewById<View>(R.id.agreementActions).visibility = View.GONE
            return
        }

        val cbConfirmation = findViewById<CheckBox>(R.id.cbConfirmation)
        cbConfirmation.contentDescription = findViewById<TextView>(R.id.tvConfirmationText).text
        findViewById<View>(R.id.tvConfirmationText).setOnClickListener {
            cbConfirmation.toggle()
        }
        val btnDecline = findViewById<Button>(R.id.btnDecline)
        val btnSubmit = findViewById<Button>(R.id.btnAccept)

        cbConfirmation.setOnCheckedChangeListener { _, isChecked ->
            btnSubmit.isEnabled = isChecked
        }

        btnDecline.setOnClickListener {
            finish()
        }

        btnSubmit.setOnClickListener {
            getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
            
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    companion object {
        const val EXTRA_SECTION = "legal_section"
        const val SECTION_TERMS = "terms"
        const val SECTION_PRIVACY = "privacy"
        const val SECTION_AGREEMENT = "agreement"
    }
}
