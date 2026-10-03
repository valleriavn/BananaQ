package com.example.bananaq

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
        val nameResId: Int,
        val backgroundColor: Int
    )

    private val avatarOptions = listOf(
        AvatarOption("farmer_1", R.drawable.avatar_farmer_1, R.string.avatar_farmer_1, Color.parseColor("#FFFAC8")),
        AvatarOption("farmer_2", R.drawable.avatar_farmer_2, R.string.avatar_farmer_2, Color.parseColor("#FFE38D")),
        AvatarOption("farmer_3", R.drawable.avatar_farmer_3, R.string.avatar_farmer_3, Color.parseColor("#CCE8CD")),
        AvatarOption("farmer_4", R.drawable.avatar_farmer_4, R.string.avatar_farmer_4, Color.parseColor("#C8E8CD")),
        AvatarOption("farmer_5", R.drawable.avatar_farmer_5, R.string.avatar_farmer_5, Color.parseColor("#B7ECF3")),
        AvatarOption("farmer_6", R.drawable.avatar_farmer_6, R.string.avatar_farmer_6, Color.parseColor("#FFE38D"))
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

        val openAvatarPreview = View.OnClickListener { showAvatarPreview() }
        findViewById<View>(R.id.profileAvatarFrame).setOnClickListener(openAvatarPreview)
        findViewById<View>(R.id.editAvatarButton).setOnClickListener { showAvatarPicker() }

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

    private fun showAvatarPreview() {
        val selected = avatarOptions.find { it.id == selectedAvatarId } ?: avatarOptions.first()
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val content = layoutInflater.inflate(R.layout.dialog_avatar_preview, null)
        content.findViewById<ImageView>(R.id.avatarPreviewImage).apply {
            setImageResource(selected.resId)
            contentDescription = getString(selected.nameResId)
        }
        content.findViewById<View>(R.id.closeAvatarPreview).setOnClickListener { dialog.dismiss() }
        val scroll = androidx.core.widget.NestedScrollView(this).apply { addView(content) }
        dialog.setContentView(scroll)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = 0.55f }
            setGravity(Gravity.CENTER)
        }
        dialog.show()
        dialog.window?.setLayout(
            minOf((resources.displayMetrics.widthPixels * 0.86f).toInt(),
                (360 * resources.displayMetrics.density).toInt()),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    private fun avatarTileBackground(option: AvatarOption, selected: Boolean): android.graphics.drawable.Drawable? {
        if (!selected) return ContextCompat.getDrawable(this, R.drawable.avatar_tile_default)
        val density = resources.displayMetrics.density
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 18 * density
            setColor(option.backgroundColor)
            setStroke((2 * density + 0.5f).toInt(), ContextCompat.getColor(this@AccountActivity, R.color.banana_green))
        }
    }
    private fun showAvatarPicker() {
        UserActionLogger.log("dialog_opened", mapOf("dialog" to "AvatarPicker"))
        val density = resources.displayMetrics.density
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val sheet = layoutInflater.inflate(R.layout.dialog_avatar_picker, null)
        var pendingAvatarId = selectedAvatarId
        val preview = sheet.findViewById<ImageView>(R.id.avatarPickerPreview)
        val initialAvatar = avatarOptions.first { it.id == pendingAvatarId }
        preview.setImageResource(initialAvatar.resId)
        preview.contentDescription = getString(initialAvatar.nameResId)
        val grid = sheet.findViewById<GridLayout>(R.id.avatarPickerGrid)
        val tiles = mutableListOf<LinearLayout>()
        val selectionMarks = mutableListOf<TextView?>()
        sheet.findViewById<View>(R.id.cancelAvatarSelection).setOnClickListener { dialog.dismiss() }
        sheet.findViewById<View>(R.id.saveAvatarSelection).setOnClickListener {
            selectedAvatarId = pendingAvatarId
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
                putString(KEY_SELECTED_AVATAR, selectedAvatarId)
            }
            UserActionLogger.log("avatar_changed", mapOf("avatarId" to selectedAvatarId))
            updateProfileAvatar()
            android.widget.Toast.makeText(this, R.string.avatar_updated, android.widget.Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
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
                background = avatarTileBackground(option, selected)
                foreground = ContextCompat.getDrawable(
                    this@AccountActivity, R.drawable.nav_item_ripple
                )
                clipToOutline = true
                val padding = (6 * density).toInt()
                setPadding(padding, padding, padding, padding)
                contentDescription = if (selected)
                    "${getString(option.nameResId)}, selected"
                else getString(R.string.choose_named_avatar, getString(option.nameResId))
                isSelected = selected
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    pendingAvatarId = option.id
                    preview.setImageResource(option.resId)
                    preview.contentDescription = getString(option.nameResId)
                    tiles.forEachIndexed { tileIndex, tile ->
                        val active = avatarOptions[tileIndex].id == pendingAvatarId
                        tile.isSelected = active
                        tile.background = avatarTileBackground(avatarOptions[tileIndex], active)
                        selectionMarks[tileIndex]?.visibility = if (active) View.VISIBLE else View.GONE
                        tile.contentDescription = getString(R.string.choose_named_avatar,
                            getString(avatarOptions[tileIndex].nameResId))
                    }
                }
            }

            tiles += optionView
            var selectionMark: TextView? = null
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
                run {
                    addView(TextView(this@AccountActivity).apply {
                        selectionMark = this
                        visibility = if (selected) View.VISIBLE else View.GONE
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

            selectionMarks += selectionMark
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

        val pickerScroll = androidx.core.widget.NestedScrollView(this)
        pickerScroll.addView(sheet)
        dialog.setContentView(pickerScroll)
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
    }
}
