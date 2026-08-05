package com.example.bananaq

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {

    private lateinit var contentContainer: LinearLayout
    private lateinit var sectionTitle: TextView
    private lateinit var tabSymptoms: TextView
    private lateinit var tabTreatment: TextView
    private lateinit var tabPrevention: TextView

    private var currentSoilType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_result)

        val diseaseNameStr = intent.getStringExtra("SOIL_TYPE") ?: "Healthy Leaf"
        val confidence = intent.getFloatExtra("CONFIDENCE", 0f)
        val bitmap = intent.getParcelableExtra<Bitmap>("SOIL_IMAGE")
        
        currentSoilType = diseaseNameStr

        val resultImage = findViewById<ImageView>(R.id.resultImage)
        val backArrow = findViewById<ImageButton>(R.id.backArrow)
        val diseaseName = findViewById<TextView>(R.id.soilName)
        val scientificName = findViewById<TextView>(R.id.scientificName)
        val accuracyValue = findViewById<TextView>(R.id.accuracyValue)
        val accuracyProgress = findViewById<ProgressBar>(R.id.accuracyProgress)
        
        contentContainer = findViewById(R.id.contentContainer)
        sectionTitle = findViewById(R.id.sectionTitle)
        tabSymptoms = findViewById(R.id.tabSymptoms)
        tabTreatment = findViewById(R.id.tabTreatment)
        tabPrevention = findViewById(R.id.tabPrevention)

        // Set Basic Info
        diseaseName.text = diseaseNameStr
        scientificName.text = when(diseaseNameStr) {
            "Panama Disease" -> "Fusarium oxysporum f. sp. cubense"
            "Black Sigatoka" -> "Mycosphaerella fijiensis"
            "Cordana Leaf Spot" -> "Cordana musae"
            else -> "Musa acuminata"
        }
        accuracyValue.text = "%.0f%%".format(confidence)
        accuracyProgress.progress = confidence.toInt()
        
        if (bitmap != null) {
            resultImage.setImageBitmap(bitmap)
        }

        // Tab Listeners
        tabSymptoms.setOnClickListener { selectTab(1) }
        tabTreatment.setOnClickListener { selectTab(2) }
        tabPrevention.setOnClickListener { selectTab(3) }

        // Default Tab
        selectTab(1)

        backArrow.setOnClickListener { finish() }
    }

    private fun selectTab(index: Int) {
        val selectedColor = Color.parseColor("#F2D597") // banana_yellow
        val unselectedColor = Color.parseColor("#F2EBDC") // light_cream

        tabSymptoms.backgroundTintList = ColorStateList.valueOf(if (index == 1) selectedColor else unselectedColor)
        tabSymptoms.setTypeface(null, if (index == 1) Typeface.BOLD else Typeface.NORMAL)
        
        tabTreatment.backgroundTintList = ColorStateList.valueOf(if (index == 2) selectedColor else unselectedColor)
        tabTreatment.setTypeface(null, if (index == 2) Typeface.BOLD else Typeface.NORMAL)
        
        tabPrevention.backgroundTintList = ColorStateList.valueOf(if (index == 3) selectedColor else unselectedColor)
        tabPrevention.setTypeface(null, if (index == 3) Typeface.BOLD else Typeface.NORMAL)

        contentContainer.removeAllViews()

        when (index) {
            1 -> {
                sectionTitle.text = "Visual Characteristics"
                showSymptoms()
            }
            2 -> {
                sectionTitle.text = "Recommended actions"
                showTreatment()
            }
            3 -> {
                sectionTitle.text = "Best practices to avoid spread"
                showPrevention()
            }
        }
    }

    private fun showSymptoms() {
        when (currentSoilType) {
            "Panama Disease" -> {
                addContentItem(1, "Yellowing leaves", "Yellowing starts from the leaf margins, and eventually the whole leaf turns yellow. This is the earliest warning sign.")
                addContentItem(2, "Brown discoloration inside the stem", "If you slice the stem, you'll see brown or reddish-brown streaks inside. A clear sign of severe infection.")
                addContentItem(3, "Wilting and leaf drop", "Lower leaves wilt and collapse first, moving upwards. It looks like drought stress, but watering does not help.")
            }
            "Black Sigatoka" -> {
                addContentItem(1, "Small dark spots", "First appears as tiny, dark-brown reddish spots on the underside of the leaves.")
                addContentItem(2, "Streaks development", "Spots expand into long, dark streaks that run parallel to the leaf veins.")
                addContentItem(3, "Leaf necrosis", "Large areas of the leaf turn brown and dry out, significantly reducing fruit yield.")
            }
            "Cordana Leaf Spot" -> {
                addContentItem(1, "Oval spots", "Large, oval spots with brown centers and bright yellow halos.")
                addContentItem(2, "Zonate patterns", "Spots often show concentric rings (zonate appearance) and can merge together.")
                addContentItem(3, "Edge infection", "Often starts at the leaf edges where water collects.")
            }
            else -> {
                addContentItem(1, "Vibrant green color", "The leaf is uniformly green with no spots or streaks.")
                addContentItem(2, "Strong structure", "The stem and veins are firm and clear of discoloration.")
            }
        }
    }

    private fun showTreatment() {
        when (currentSoilType) {
            "Panama Disease" -> {
                addContentItem(1, "Isolate affected plants", "Remove and bag infected plants immediately. Do not compost.")
                addContentItem(2, "Soil Treatment", "Apply calcium cyanamide or solarize soil for 6-8 weeks to reduce fungal load.")
            }
            "Black Sigatoka" -> {
                addContentItem(1, "Fungicide application", "Apply systemic or contact fungicides regularly, especially during rainy seasons.")
                addContentItem(2, "Sanitation", "Remove and burn severely infected leaves to reduce spore load.")
            }
            "Cordana Leaf Spot" -> {
                addContentItem(1, "Improved aeration", "Prune overlapping leaves to increase air flow and reduce humidity.")
                addContentItem(2, "Copper sprays", "Apply copper-based fungicides if the infection becomes widespread.")
            }
            else -> {
                addContentItem(1, "Maintenance", "Continue regular watering and organic fertilization.")
            }
        }
    }

    private fun showPrevention() {
        when (currentSoilType) {
            "Panama Disease" -> {
                addContentItem(1, "Sanitize tools between plants", "Disinfect knives and spades with 70% alcohol or bleach solution.")
                addContentItem(2, "Improved drainage", "Waterlogged soil accelerates fungal spread. Raised beds help.")
            }
            "Black Sigatoka" -> {
                addContentItem(1, "Wider spacing", "Plant bananas with adequate spacing to allow leaves to dry quickly after rain.")
                addContentItem(2, "Resistant varieties", "Consider planting FHIA hybrids which are more resistant to Sigatoka.")
            }
            "Cordana Leaf Spot" -> {
                addContentItem(1, "Weed control", "Keep the area around the base clean to prevent high local humidity.")
                addContentItem(2, "Avoid overhead irrigation", "Water at the base to keep leaves dry.")
            }
            else -> {
                addContentItem(1, "Vigilance", "Regularly inspect leaves for early signs of any spots.")
            }
        }
    }

    private fun addContentItem(number: Int, title: String, description: String) {
        val itemLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 24 * resources.displayMetrics.density.toInt())
        }

        val numberCircle = TextView(this).apply {
            text = number.toString()
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#4A773C")) // banana_text_dark
            setBackgroundResource(R.drawable.rounded_button_bg)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F2D597")) // banana_yellow
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                (28 * resources.displayMetrics.density).toInt(),
                (28 * resources.displayMetrics.density).toInt()
            ).apply {
                marginEnd = (16 * resources.displayMetrics.density).toInt()
            }
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }

        val descView = TextView(this).apply {
            text = description
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            setLineSpacing(4f, 1f)
        }

        textLayout.addView(titleView)
        textLayout.addView(descView)

        itemLayout.addView(numberCircle)
        itemLayout.addView(textLayout)

        contentContainer.addView(itemLayout)
    }
}
