package com.example.bananaq

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeedbackDetailActivity : LocaleAwareActivity() {

    private var selectedRating = ""
    private var submitted = false
    private var submittedAt = 0L
    private var submittedComments = ""
    private lateinit var diseaseName: String
    private lateinit var accuracy: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_feedback_detail)
        applySystemInsets()

        diseaseName = intent.getStringExtra("DISEASE_NAME") ?: getString(R.string.unknown_label)
        accuracy = intent.getStringExtra("ACCURACY") ?: ""
        findViewById<TextView>(R.id.tvDiseaseName).text = localizedDiseaseName(this, diseaseName)
        findViewById<TextView>(R.id.tvDateTime).text = intent.getStringExtra("DATE_TIME") ?: ""
        findViewById<TextView>(R.id.tvAccuracy).apply {
            text = if (accuracy.endsWith("%")) accuracy else "$accuracy%"
            visibility = if (intent.getBooleanExtra("RESULT_VALID", true)) View.VISIBLE else View.GONE
        }
        intent.getStringExtra("IMAGE_URI")?.let {
            findViewById<ImageView>(R.id.ivScanDetail).setImageURI(Uri.parse(it))
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        selectedRating = savedInstanceState?.getString("rating") ?: ""
        submitted = savedInstanceState?.getBoolean("submitted") ?: false
        submittedAt = savedInstanceState?.getLong("submittedAt") ?: 0L
        submittedComments = savedInstanceState?.getString("submittedComments") ?: ""
        setupRatingOptions()

        findViewById<View>(R.id.btnSubmitFeedback).setOnClickListener {
            when {
                selectedRating.isEmpty() ->
                    Toast.makeText(this, R.string.feedback_select_rating, Toast.LENGTH_SHORT).show()
                intent.getStringExtra("SCAN_ID").isNullOrEmpty() ->
                    Toast.makeText(this, R.string.feedback_select_scan, Toast.LENGTH_LONG).show()
                else -> confirmSubmission()
            }
        }
        findViewById<View>(R.id.btnReturn).setOnClickListener { finish() }
        if (submitted) showSubmittedState(submittedComments)
        setupBottomNavigation()
    }

    private fun setupRatingOptions() {
        val ratings = linkedMapOf(
            R.id.rateVeryAccurate to "Very Accurate",
            R.id.rateAccurate to "Accurate",
            R.id.rateNotSure to "Not Sure",
            R.id.rateInaccurate to "Inaccurate",
            R.id.rateVeryInaccurate to "Very Inaccurate"
        )
        fun renderRatings() {
            ratings.entries.forEachIndexed { index, (id, name) ->
                val option = findViewById<LinearLayout>(id)
                val isSelected = selectedRating == name
                val color = ContextCompat.getColor(this,
                    if (!isSelected) R.color.banana_yellow else when (index) {
                        0, 1 -> R.color.banana_green
                        2 -> R.color.banana_muted
                        else -> R.color.rating_negative
                    })
                option.background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 10 * resources.displayMetrics.density
                    setColor(ContextCompat.getColor(this@FeedbackDetailActivity, R.color.banana_surface))
                    setStroke((resources.displayMetrics.density * if (isSelected) 2 else 1).toInt(), color)
                }
                (option.getChildAt(0) as ImageView).setColorFilter(color)
                (option.getChildAt(1) as TextView).setTextColor(
                    ContextCompat.getColor(this, R.color.banana_body))
            }
        }

        ratings.forEach { (id, rating) ->
            val option = findViewById<LinearLayout>(id)
            option.contentDescription = (option.getChildAt(1) as TextView).text
            option.setOnClickListener {
                selectedRating = rating
                renderRatings()
            }
        }
        renderRatings()
    }

    private fun confirmSubmission() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_feedback_confirmation)
        dialog.setCanceledOnTouchOutside(true)
        dialog.findViewById<View>(R.id.btnCancelFeedback).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<View>(R.id.btnConfirmFeedback).setOnClickListener {
            dialog.dismiss()
            saveFeedback()
        }
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = 0.58f }
            setGravity(Gravity.CENTER)
        }
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun saveFeedback() {
        val scanId = intent.getStringExtra("SCAN_ID") ?: return
        val comments = findViewById<EditText>(R.id.etComments).text.toString().trim()
        submittedComments = comments
        submittedAt = System.currentTimeMillis()
        val feedback = org.json.JSONObject().apply {
            put("rating", selectedRating)
            put("comments", comments)
            put("time", submittedAt)
        }
        getSharedPreferences("scan_feedback", MODE_PRIVATE).edit()
            .putString(scanId, feedback.toString()).apply()
        submitted = true
        showSubmittedState(comments)
    }

    private fun showSubmittedState(comments: String) {
        findViewById<View>(R.id.formScroll).visibility = View.GONE
        findViewById<View>(R.id.successScroll).visibility = View.VISIBLE
        setSummary(R.id.summaryScan, getString(R.string.summary_scan), localizedDiseaseName(this, diseaseName), R.color.banana_green)
        setSummary(R.id.summaryRating, getString(R.string.summary_accuracy), localizedRating(), ratingColor())
        setSummary(R.id.summaryConfidence, getString(R.string.summary_confidence),
            if (accuracy.endsWith("%")) accuracy else "$accuracy%", R.color.rating_negative)
        val time = if (submittedAt > 0L)
            getString(R.string.today_at, SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(submittedAt)))
        else getString(R.string.summary_saved)
        setSummary(R.id.summarySubmitted, getString(R.string.summary_submitted), time)
        findViewById<TextView>(R.id.summaryComments).apply {
            if (comments.isBlank()) {
                visibility = View.GONE
            } else {
                text = getString(R.string.summary_additional_comments, comments)
                visibility = View.VISIBLE
            }
        }
    }

    private fun ratingColor(): Int = when (selectedRating) {
        "Very Accurate", "Accurate" -> R.color.banana_green
        "Not Sure" -> R.color.banana_muted
        else -> R.color.rating_negative
    }

    private fun localizedRating(): String = getString(when (selectedRating) {
        "Very Accurate" -> R.string.rating_very_accurate_single_line
        "Accurate" -> R.string.rating_accurate
        "Not Sure" -> R.string.rating_not_sure
        "Inaccurate" -> R.string.rating_inaccurate
        else -> R.string.rating_very_inaccurate_single_line
    })

    private fun setSummary(viewId: Int, label: String, value: String, valueColor: Int? = null) {
        val text = android.text.SpannableString("$label\t$value")
        text.setSpan(android.text.style.ForegroundColorSpan(
            ContextCompat.getColor(this, R.color.banana_muted)),
            0, label.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(android.text.style.StyleSpan(Typeface.BOLD),
            label.length + 1, text.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        valueColor?.let {
            text.setSpan(android.text.style.ForegroundColorSpan(ContextCompat.getColor(this, it)),
                label.length + 1, text.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        text.setSpan(android.text.style.TabStopSpan.Standard(
            (135 * resources.displayMetrics.density).toInt()),
            0, text.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        findViewById<TextView>(viewId).text = text
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("rating", selectedRating)
        outState.putBoolean("submitted", submitted)
        outState.putLong("submittedAt", submittedAt)
        outState.putString("submittedComments", submittedComments)
        super.onSaveInstanceState(outState)
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_feedback
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                    finish()
                    true
                }
                R.id.nav_scan -> {
                    startActivity(Intent(this, ScannerActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_feedback -> true
                R.id.nav_account -> {
                    startActivity(Intent(this, AccountActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }
}
