package bananaq.ui.scan

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import bananaq.R
import bananaq.model.DefaultPrevention
import bananaq.model.DiseaseInfo
import bananaq.model.DiseaseTip
import kotlin.math.ceil

/** Activity-owned renderer; shared geometry for all result tabs and both languages. */
class ScanRecommendationRenderer(
    private val container: LinearLayout,
    private val defaults: DefaultPrevention
) {
    private val context = container.context
    private val resources = context.resources

    fun showSymptoms(info: DiseaseInfo) = showItems(info.symptoms)

    fun showTreatment(info: DiseaseInfo) {
        if (info.diseaseName == "Healthy") {
            container.addView(TextView(context).apply {
                setText(R.string.healthy_result_care_note)
                textSize = 12f
                setTextColor(color(R.color.banana_body))
                setPadding(0, 0, 0, spacing(R.dimen.spacing_16))
            })
        }
        showItems(defaults.diseaseSpecificTreatment(info))
    }

    fun showPrevention(info: DiseaseInfo) =
        showItems(defaults.prevention(info))

    private fun showItems(tips: List<DiseaseTip>) {
        tips.forEachIndexed { index, tip -> addItem(index + 1, tip) }
    }

    private fun addItem(number: Int, tip: DiseaseTip) {
        val density = resources.displayMetrics.density
        val itemGap = spacing(R.dimen.spacing_12)
        val badge = TextView(context).apply {
            text = number.toString()
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(color(R.color.button_text_black))
            setBackgroundResource(R.drawable.result_number_circle)
        }
        // Include actual font metrics, including Android's nonlinear enlarged-font scaling.
        val badgePadding = spacing(R.dimen.spacing_8)
        val badgeSize = maxOf((26 * density).toInt(),
            ceil(badge.paint.fontMetrics.bottom - badge.paint.fontMetrics.top).toInt() + badgePadding,
            ceil(badge.paint.measureText(badge.text.toString())).toInt() + badgePadding)
        badge.layoutParams = LinearLayout.LayoutParams(badgeSize, badgeSize).apply { marginEnd = itemGap }

        val title = TextView(context).apply {
            text = tip.title
            textSize = 14f
            setTextColor(color(R.color.banana_body))
            setTypeface(null, Typeface.BOLD)
        }
        val description = TextView(context).apply {
            text = tip.description
            textSize = 12f
            setTextColor(color(R.color.banana_muted))
            setLineSpacing(resources.getDimension(R.dimen.spacing_4) / 2, 1f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = spacing(R.dimen.spacing_4) }
        }
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(title)
            addView(description)
        }
        if (container.getChildAt(container.childCount - 1)?.tag == ITEM_TAG) {
            container.addView(View(context).apply {
                setBackgroundColor(color(R.color.banana_yellow_light))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    density.toInt().coerceAtLeast(1)).apply {
                    marginStart = badgeSize + itemGap
                    topMargin = spacing(R.dimen.spacing_8)
                    bottomMargin = spacing(R.dimen.spacing_8)
                }
            })
        }
        container.addView(LinearLayout(context).apply {
            tag = ITEM_TAG
            orientation = LinearLayout.HORIZONTAL
            addView(badge)
            addView(body)
        })
    }

    private fun spacing(id: Int) = resources.getDimensionPixelSize(id)
    private fun color(id: Int) = ContextCompat.getColor(context, id)

    private companion object {
        const val ITEM_TAG = "recommendation-item"
    }
}
