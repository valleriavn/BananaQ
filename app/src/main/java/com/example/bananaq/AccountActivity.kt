package com.example.bananaq

import android.content.Context
import android.content.Intent
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class AccountActivity : LocaleAwareActivity() {

    private data class AvatarOption(
        val id: String,
        val resId: Int,
        val nameResId: Int
    )

    private val avatarOptions = listOf(
        AvatarOption("farmer_1", R.drawable.avatar_farmer_1, R.string.avatar_farmer_1),
        AvatarOption("farmer_2", R.drawable.avatar_farmer_2, R.string.avatar_farmer_2),
        AvatarOption("farmer_3", R.drawable.avatar_farmer_3, R.string.avatar_farmer_3),
        AvatarOption("farmer_4", R.drawable.avatar_farmer_4, R.string.avatar_farmer_4),
        AvatarOption("farmer_5", R.drawable.avatar_farmer_5, R.string.avatar_farmer_5),
        AvatarOption("farmer_6", R.drawable.avatar_farmer_6, R.string.avatar_farmer_6)
    )

    private lateinit var profileAvatar: ImageView
    private var selectedAvatarId = DEFAULT_AVATAR_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account)
        applySystemInsets()
        setupBottomNavigation()

        UserActionLogger.log("screen_view", mapOf("screen" to "AccountActivity"))

        profileAvatar = findViewById(R.id.profileAvatar)
        selectedAvatarId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_SELECTED_AVATAR, DEFAULT_AVATAR_ID) ?: DEFAULT_AVATAR_ID
        updateProfileAvatar()

        val openAvatarPicker = View.OnClickListener { showAvatarPicker() }
        findViewById<View>(R.id.profileAvatarFrame).setOnClickListener(openAvatarPicker)

        findViewById<View>(R.id.manualRow).setOnClickListener {
            UserActionLogger.log("button_click", mapOf("target" to "UserManual"))
            startActivity(Intent(this, UserManualActivity::class.java))
        }

        mapOf(
            R.id.termsRow to UserAgreementActivity.SECTION_TERMS,
            R.id.privacyRow to UserAgreementActivity.SECTION_PRIVACY,
            R.id.agreementRow to UserAgreementActivity.SECTION_AGREEMENT
        ).forEach { (id, section) ->
            findViewById<View>(id).setOnClickListener {
                UserActionLogger.log("button_click", mapOf("target" to "LegalSection", "section" to section))
                startActivity(Intent(this, UserAgreementActivity::class.java)
                    .putExtra(UserAgreementActivity.EXTRA_SECTION, section))
            }
        }
    }

    private fun setupBottomNavigation() {
        configureBottomNavigation(R.id.nav_account)
    }

    private fun updateProfileAvatar() {
        val selected = avatarOptions.find { it.id == selectedAvatarId } ?: avatarOptions.first()
        if (selected.id != selectedAvatarId) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
                putString(KEY_SELECTED_AVATAR, selected.id)
            }
        }
        selectedAvatarId = selected.id
        profileAvatar.setImageResource(selected.resId)
        profileAvatar.contentDescription = getString(selected.nameResId)
    }

    private fun showAvatarPicker() {
        UserActionLogger.log("dialog_opened", mapOf("dialog" to "AvatarPicker"))
        val density = resources.displayMetrics.density
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val sheet = layoutInflater.inflate(R.layout.dialog_avatar_picker, null)
        val grid = sheet.findViewById<GridLayout>(R.id.avatarPickerGrid)
        sheet.findViewById<View>(R.id.closeAvatarPicker).setOnClickListener { dialog.dismiss() }
        val maximumWidth = (440 * density).toInt()
        val dialogWidth = minOf(
            (resources.displayMetrics.widthPixels * 0.90f).toInt(),
            maximumWidth
        )
        val columnCount = if (dialogWidth >= (300 * density).toInt()) 3 else 2
        grid.columnCount = columnCount

        avatarOptions.forEachIndexed { index, option ->
            val selected = option.id == selectedAvatarId
            val optionView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = ContextCompat.getDrawable(
                    this@AccountActivity,
                    if (selected) R.drawable.avatar_tile_selected
                    else R.drawable.avatar_tile_default
                )
                foreground = ContextCompat.getDrawable(
                    this@AccountActivity, R.drawable.nav_item_ripple
                )
                clipToOutline = true
                val padding = (6 * density).toInt()
                setPadding(padding, padding, padding, padding)
                contentDescription = if (selected)
                    "${getString(option.nameResId)}, selected"
                else getString(R.string.choose_named_avatar, getString(option.nameResId))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedAvatarId = option.id
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
                        putString(KEY_SELECTED_AVATAR, selectedAvatarId)
                    }
                    UserActionLogger.log("avatar_changed", mapOf("avatarId" to option.id))
                    updateProfileAvatar()
                    dialog.dismiss()
                }
            }

            val avatarSize = ((if (columnCount == 3) 76 else 86) * density).toInt()
            optionView.addView(FrameLayout(this).apply {
                addView(ImageView(this@AccountActivity).apply {
                    setImageResource(option.resId)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))
                if (selected) {
                    addView(TextView(this@AccountActivity).apply {
                        text = "✓"
                        gravity = Gravity.CENTER
                        textSize = 13f
                        setTextColor(Color.WHITE)
                        background = ContextCompat.getDrawable(
                            this@AccountActivity, R.drawable.feedback_success_circle)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }, FrameLayout.LayoutParams(
                        (24 * density).toInt(),
                        (24 * density).toInt(),
                        Gravity.END or Gravity.BOTTOM
                    ))
                }
            }, LinearLayout.LayoutParams(avatarSize, avatarSize))

            grid.addView(optionView, GridLayout.LayoutParams(
                GridLayout.spec(index / columnCount),
                GridLayout.spec(index % columnCount, 1, 1f)
            ).apply {
                width = 0
                height = avatarSize + (16 * density).toInt()
                val margin = (4 * density).toInt()
                setMargins(margin, margin, margin, margin)
                setGravity(Gravity.FILL_HORIZONTAL)
            })
        }

        dialog.setContentView(sheet)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = 0.55f }
            setGravity(Gravity.CENTER)
        }
        dialog.show()
        dialog.window?.setLayout(
            dialogWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        const val PREFS_NAME = "settings"
        const val KEY_SELECTED_AVATAR = "selected_avatar"
        private const val DEFAULT_AVATAR_ID = "farmer_1"

        fun getSelectedAvatarResId(context: Context): Int {
            val avatarId = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_SELECTED_AVATAR, DEFAULT_AVATAR_ID) ?: DEFAULT_AVATAR_ID
            return when (avatarId) {
                "farmer_2" -> R.drawable.avatar_farmer_2
                "farmer_3" -> R.drawable.avatar_farmer_3
                "farmer_4" -> R.drawable.avatar_farmer_4
                "farmer_5" -> R.drawable.avatar_farmer_5
                "farmer_6" -> R.drawable.avatar_farmer_6
                else -> R.drawable.avatar_farmer_1
            }
        }
    }
}
