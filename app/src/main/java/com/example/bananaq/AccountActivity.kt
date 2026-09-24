package com.example.bananaq

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class AccountActivity : AppCompatActivity() {

    private data class AvatarOption(
        val id: String,
        val resId: Int,
        val nameResId: Int
    )

    private val avatarOptions = listOf(
        AvatarOption("cartoon_banana", R.drawable.avatar_cartoon_banana, R.string.avatar_cartoon_banana),
        AvatarOption("cool_banana", R.drawable.avatar_cool_banana, R.string.avatar_cool_banana),
        AvatarOption("banana_leaf", R.drawable.avatar_banana_leaf, R.string.avatar_banana_leaf),
        AvatarOption("banana_bunch", R.drawable.avatar_banana_bunch, R.string.avatar_banana_bunch),
        AvatarOption("banana_tree", R.drawable.avatar_banana_tree, R.string.avatar_banana_tree),
        AvatarOption("banana_smoothie", R.drawable.avatar_banana_smoothie, R.string.avatar_banana_smoothie),
        AvatarOption("young_plant", R.drawable.avatar_young_plant, R.string.avatar_young_plant),
        AvatarOption("farmer_hat", R.drawable.avatar_farmer_hat, R.string.avatar_farmer_hat),
        AvatarOption("banana_flower", R.drawable.avatar_banana_flower, R.string.avatar_banana_flower),
        AvatarOption("leaf_scan", R.drawable.avatar_leaf_scan, R.string.avatar_leaf_scan)
    )

    private lateinit var profileAvatar: ImageView
    private var selectedAvatarId = "cartoon_banana"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account)
        applySystemInsets()

        profileAvatar = findViewById(R.id.profileAvatar)
        selectedAvatarId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_SELECTED_AVATAR, "cartoon_banana") ?: "cartoon_banana"
        updateProfileAvatar()

        val openAvatarPicker = View.OnClickListener { showAvatarPicker() }
        findViewById<View>(R.id.profileAvatarFrame).setOnClickListener(openAvatarPicker)
        findViewById<View>(R.id.editAvatarButton).setOnClickListener(openAvatarPicker)

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }
        findViewById<View>(R.id.manualRow).setOnClickListener {
            startActivity(Intent(this, UserManualActivity::class.java))
        }

        mapOf(
            R.id.termsRow to UserAgreement.SECTION_TERMS,
            R.id.privacyRow to UserAgreement.SECTION_PRIVACY,
            R.id.agreementRow to UserAgreement.SECTION_AGREEMENT
        ).forEach { (id, section) ->
            findViewById<View>(id).setOnClickListener {
                startActivity(Intent(this, UserAgreement::class.java)
                    .putExtra(UserAgreement.EXTRA_SECTION, section))
            }
        }
    }

    private fun updateProfileAvatar() {
        val selected = avatarOptions.find { it.id == selectedAvatarId } ?: avatarOptions.first()
        selectedAvatarId = selected.id
        profileAvatar.setImageResource(selected.resId)
        profileAvatar.contentDescription = getString(selected.nameResId)
    }

    private fun showAvatarPicker() {
        val density = resources.displayMetrics.density
        val dialog = BottomSheetDialog(this)
        val sheet = layoutInflater.inflate(R.layout.dialog_avatar_picker, null)
        val grid = sheet.findViewById<GridLayout>(R.id.avatarPickerGrid)
        sheet.findViewById<View>(R.id.closeAvatarPicker).setOnClickListener { dialog.dismiss() }

        avatarOptions.forEachIndexed { index, option ->
            val selected = option.id == selectedAvatarId
            val optionView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding((10 * density).toInt(), (10 * density).toInt(),
                    (10 * density).toInt(), (8 * density).toInt())
                setBackgroundResource(if (selected) R.drawable.bg_avatar_item_selected
                    else R.drawable.bg_avatar_item_normal)
                contentDescription = if (selected)
                    "${getString(option.nameResId)}, selected"
                else "Choose ${getString(option.nameResId)}"
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedAvatarId = option.id
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
                        putString(KEY_SELECTED_AVATAR, selectedAvatarId)
                    }
                    updateProfileAvatar()
                    dialog.dismiss()
                }
            }
            optionView.addView(ImageView(this).apply {
                setImageResource(option.resId)
                scaleType = ImageView.ScaleType.FIT_CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams((68 * density).toInt(), (68 * density).toInt()))
            optionView.addView(TextView(this).apply {
                text = if (selected) "✓ ${getString(option.nameResId)}" else getString(option.nameResId)
                gravity = Gravity.CENTER
                maxLines = 1
                textSize = 12f
                setTypeface(null, if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                setTextColor(ContextCompat.getColor(this@AccountActivity,
                    if (selected) R.color.banana_green else R.color.banana_body))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = (4 * density).toInt() })

            grid.addView(optionView, GridLayout.LayoutParams(
                GridLayout.spec(index / 2), GridLayout.spec(index % 2, 1, 1f)
            ).apply {
                width = 0
                height = (116 * density).toInt()
                val margin = (6 * density).toInt()
                setMargins(margin, margin, margin, margin)
                setGravity(Gravity.FILL_HORIZONTAL)
            })
        }

        dialog.setContentView(sheet)
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            dialog.behavior.skipCollapsed = true
        }
        dialog.show()
    }

    companion object {
        const val PREFS_NAME = "settings"
        const val KEY_SELECTED_AVATAR = "selected_avatar"

        fun getSelectedAvatarResId(context: Context): Int {
            val avatarId = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_SELECTED_AVATAR, "cartoon_banana") ?: "cartoon_banana"
            return when (avatarId) {
                "cool_banana" -> R.drawable.avatar_cool_banana
                "banana_leaf" -> R.drawable.avatar_banana_leaf
                "banana_bunch" -> R.drawable.avatar_banana_bunch
                "banana_tree" -> R.drawable.avatar_banana_tree
                "banana_smoothie" -> R.drawable.avatar_banana_smoothie
                "young_plant" -> R.drawable.avatar_young_plant
                "farmer_hat" -> R.drawable.avatar_farmer_hat
                "banana_flower" -> R.drawable.avatar_banana_flower
                "leaf_scan" -> R.drawable.avatar_leaf_scan
                else -> R.drawable.avatar_cartoon_banana
            }
        }
    }
}
