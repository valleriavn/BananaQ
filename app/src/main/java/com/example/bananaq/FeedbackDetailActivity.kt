package com.example.bananaq

import com.example.bananaq.data.BananaQDatabase
import com.example.bananaq.data.FeedbackRecord
import com.example.bananaq.data.sync.SupabaseSyncManager

import android.app.Dialog
import android.graphics.Color
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
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeedbackDetailActivity : LocaleAwareActivity() {

    private var confirmationDialog: Dialog? = null
    private var selectedRating = ""
    private var submitted = false
    private var submittedAt = 0L
    private var submittedComments = ""
    private lateinit var diseaseName: String
    private lateinit var accuracy: String
    private var resultValid = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_feedback_detail)
        applySystemInsets()

        resultValid = intent.getBooleanExtra("RESULT_VALID", true)
        diseaseName = if (resultValid) {
            intent.getStringExtra("DISEASE_NAME") ?: getString(R.string.unknown_label)
        } else {
            getString(R.string.unable_identify)
        }
        accuracy = if (resultValid) intent.getStringExtra("ACCURACY") ?: "" else ""
        val diseaseNameView = findViewById<TextView>(R.id.tvDiseaseName)
        val dateTimeView = findViewById<TextView>(R.id.tvDateTime)
        diseaseNameView.apply {
            text = if (resultValid) localizedDiseaseName(this@FeedbackDetailActivity, diseaseName)
                else diseaseName
            if (!resultValid) {
                textSize = 15f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(this@FeedbackDetailActivity, R.color.button_text_black))
            }
        }
        dateTimeView.apply {
            text = intent.getStringExtra("DATE_TIME") ?: ""
            visibility = View.VISIBLE
            if (!resultValid) gravity = Gravity.START
        }
        if (!resultValid) {
            findViewById<LinearLayout>(R.id.resultMetaLayout).apply {
                removeView(dateTimeView)
                addView(dateTimeView, 1)
            }
        }
        findViewById<TextView>(R.id.tvConfidenceLabel).visibility =
            if (resultValid) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tvAccuracy).apply {
            text = if (accuracy.endsWith("%")) accuracy else "$accuracy%"
            visibility = if (resultValid) View.VISIBLE else View.GONE
        }
        ScanThumbnailLoader.load(findViewById(R.id.ivScanDetail), intent.getStringExtra("IMAGE_URI"))

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        selectedRating = savedInstanceState?.getString("rating") ?: ""
        submitted = savedInstanceState?.getBoolean("submitted") ?: false
        submittedAt = savedInstanceState?.getLong("submittedAt") ?: 0L
        submittedComments = savedInstanceState?.getString("submittedComments") ?: ""
        if (savedInstanceState == null) restoreSubmittedFeedback()
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
                option.isSelected = isSelected
                val color = ContextCompat.getColor(this,
                    if (!isSelected) R.color.banana_border else when (index) {
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
        if (submitted || confirmationDialog?.isShowing == true) return
        val dialog = Dialog(this)
        confirmationDialog = dialog
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
        val database = BananaQDatabase.get(this)
        if (database.feedbackForScan(scanId) != null) {
            restoreSubmittedFeedback()
            showSubmittedState(submittedComments)
            return
        }
        val comments = findViewById<EditText>(R.id.etComments).text.toString().trim()
        submittedComments = comments
        submittedAt = System.currentTimeMillis()
        submitted = database.insertFeedback(FeedbackRecord(
            feedbackId = java.util.UUID.randomUUID().toString(),
            scanId = scanId,
            accuracyRating = selectedRating,
            comments = comments,
            submittedAt = submittedAt
        ))
        if (submitted) {
            UserActionLogger.logFeedbackEvent(scanId, selectedRating, comments)
            SupabaseSyncManager.request(applicationContext)
            showSubmittedState(comments)
        } else {
            restoreSubmittedFeedback()
            if (submitted) showSubmittedState(submittedComments)
            else Toast.makeText(this, R.string.feedback_save_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun restoreSubmittedFeedback() {
        val scanId = intent.getStringExtra("SCAN_ID") ?: return
        val feedback = BananaQDatabase.get(this).feedbackForScan(scanId) ?: return
        selectedRating = feedback.accuracyRating
        submittedComments = feedback.comments
        submittedAt = feedback.submittedAt
        submitted = true
    }

    private fun showSubmittedState(comments: String) {
        val keyboard = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        keyboard.hideSoftInputFromWindow(findViewById<View>(R.id.etComments).windowToken, 0)
        findViewById<View>(R.id.etComments).clearFocus()
        findViewById<View>(R.id.formScroll).visibility = View.GONE
        findViewById<View>(R.id.successScroll).visibility = View.VISIBLE
        findViewById<View>(R.id.successScroll).announceForAccessibility(getString(R.string.feedback_thank_you))
        setSummary(
            R.id.summaryScan,
            getString(R.string.summary_scan),
            if (resultValid) localizedDiseaseName(this, diseaseName) else diseaseName,
            R.color.banana_green
        )
        setSummary(R.id.summaryRating, getString(R.string.summary_accuracy), localizedRating(), ratingColor())
        findViewById<View>(R.id.summaryConfidenceRow).visibility =
            if (resultValid) View.VISIBLE else View.GONE
        if (resultValid) {
            setSummary(R.id.summaryConfidence, getString(R.string.summary_confidence),
                if (accuracy.endsWith("%")) accuracy else "$accuracy%", R.color.rating_negative)
        }
        val time = if (submittedAt > 0L)
            getString(R.string.today_at, SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(submittedAt)))
        else getString(R.string.summary_saved)
        setSummary(R.id.summarySubmitted, getString(R.string.summary_submitted), time)
        findViewById<TextView>(R.id.summaryComments).apply {
            if (comments.isBlank()) {
                visibility = View.GONE
            } else {
                text = comments
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
        findViewById<TextView>(viewId).apply {
            text = value
            contentDescription = "$label: $value"
            setTextColor(ContextCompat.getColor(
                this@FeedbackDetailActivity,
                valueColor ?: R.color.banana_body
            ))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("rating", selectedRating)
        outState.putBoolean("submitted", submitted)
        outState.putLong("submittedAt", submittedAt)
        outState.putString("submittedComments", submittedComments)
        super.onSaveInstanceState(outState)
    }

    private fun setupBottomNavigation() {
        configureBottomNavigation(R.id.nav_feedback)
    }
}
