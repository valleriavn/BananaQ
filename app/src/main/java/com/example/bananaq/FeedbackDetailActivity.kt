package com.example.bananaq

import android.app.Dialog
import android.content.Intent
<<<<<<< Updated upstream
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
=======
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
>>>>>>> Stashed changes
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
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

    override fun onResume() {
        super.onResume()
        Tagalog.applyToActivity(this)
    }

    private fun setupRatingOptions() {
        data class RatingOptionInfo(
            val id: Int,
            val name: String,
            val selectedColor: Int,
            val selectedBgColor: Int
        )

        // Color palette according to user request:
        // Very Accurate -> Green (#2E7D32)
        // Accurate -> Light Green (#689F38)
        // Not Sure -> Black (#212121)
        // Inaccurate -> Orange close to Red (#FF3D00)
        // Very Inaccurate -> Red (#D32F2F)
        val options = listOf(
            RatingOptionInfo(R.id.rateVeryAccurate, "Very Accurate", Color.parseColor("#2E7D32"), Color.parseColor("#E8F5E9")),
            RatingOptionInfo(R.id.rateAccurate, "Accurate", Color.parseColor("#689F38"), Color.parseColor("#F1F8E9")),
            RatingOptionInfo(R.id.rateNotSure, "Not Sure", Color.parseColor("#212121"), Color.parseColor("#E0E0E0")),
            RatingOptionInfo(R.id.rateInaccurate, "Inaccurate", Color.parseColor("#FF3D00"), Color.parseColor("#FBE9E7")),
            RatingOptionInfo(R.id.rateVeryInaccurate, "Very Inaccurate", Color.parseColor("#D32F2F"), Color.parseColor("#FFEBEE"))
        )

        val defaultBorderColor = Color.parseColor("#EFE3C3")
        val defaultBgColor = Color.parseColor("#FFFDF5")
        val defaultIconColor = Color.parseColor("#F0B429")
        val defaultTextColor = ContextCompat.getColor(this, R.color.banana_body)
        val density = resources.displayMetrics.density

        fun renderRatings() {
            options.forEach { opt ->
                val cardView = findViewById<android.widget.LinearLayout>(opt.id) ?: return@forEach
                val isSelected = selectedRating == opt.name

                val strokeColor = if (isSelected) opt.selectedColor else defaultBorderColor
                val bgColor = if (isSelected) opt.selectedBgColor else defaultBgColor
                val strokeWidth = ((if (isSelected) 2.5f else 1.5f) * density).toInt()

                cardView.background = GradientDrawable().apply {
                    cornerRadius = 16f * density
                    setColor(bgColor)
                    setStroke(strokeWidth, strokeColor)
                }

                val imageView = cardView.getChildAt(0) as? ImageView
                imageView?.imageTintList = ColorStateList.valueOf(
                    if (isSelected) opt.selectedColor else defaultIconColor
                )

                val textView = cardView.getChildAt(1) as? TextView
                textView?.setTextColor(
                    if (isSelected) opt.selectedColor else defaultTextColor
                )
            }
        }

<<<<<<< Updated upstream
        ratings.forEach { (id, rating) ->
            val option = findViewById<LinearLayout>(id)
            option.contentDescription = (option.getChildAt(1) as TextView).text
            option.setOnClickListener {
                selectedRating = rating
=======
        options.forEach { opt ->
            val cardView = findViewById<android.widget.LinearLayout>(opt.id) ?: return@forEach
            cardView.setOnClickListener {
                selectedRating = opt.name
>>>>>>> Stashed changes
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
