package com.example.watdasoil

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ResultActivity : AppCompatActivity() {

    private lateinit var contentContainer: LinearLayout
    private lateinit var sectionTitle: TextView
    private lateinit var tabDescription: TextView
    private lateinit var tabUses: TextView
    private lateinit var tabAdvantages: TextView

    private var currentSoilType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_result)

        val soilType = intent.getStringExtra("SOIL_TYPE") ?: "Unknown"
        val confidence = intent.getFloatExtra("CONFIDENCE", 0f)
        val bitmap = intent.getParcelableExtra<Bitmap>("SOIL_IMAGE")
        
        currentSoilType = soilType

        val resultImage = findViewById<ImageView>(R.id.resultImage)
        val backArrow = findViewById<ImageButton>(R.id.backArrow)
        val backButton = findViewById<Button>(R.id.backButton)
        val soilName = findViewById<TextView>(R.id.soilName)
        val scientificName = findViewById<TextView>(R.id.scientificName)
        val accuracyValue = findViewById<TextView>(R.id.accuracyValue)
        val accuracyProgress = findViewById<ProgressBar>(R.id.accuracyProgress)
        
        contentContainer = findViewById(R.id.contentContainer)
        sectionTitle = findViewById(R.id.sectionTitle)
        tabDescription = findViewById(R.id.tabDescription)
        tabUses = findViewById(R.id.tabUses)
        tabAdvantages = findViewById(R.id.tabAdvantages)

        // Set Basic Info
        soilName.text = soilType
        scientificName.text = when(soilType) {
            "Yellow Soil" -> "Hydrated iron oxides"
            "Red Soil" -> "High iron oxide content"
            "Black Soil" -> "High clay and organic matter"
            else -> "Soil Information"
        }
        accuracyValue.text = "%.0f%%".format(confidence)
        accuracyProgress.progress = confidence.toInt()
        
        if (bitmap != null) {
            resultImage.setImageBitmap(bitmap)
        }

        val header = findViewById<LinearLayout>(R.id.resultHeader)
        val cardContent = findViewById<LinearLayout>(R.id.cardContent)

        // Set Colors based on Soil Type
        val themeColor = when(soilType) {
            "Yellow Soil" -> Color.parseColor("#FBC02D")
            "Red Soil" -> Color.parseColor("#D32F2F")
            "Black Soil" -> Color.parseColor("#5D4037")
            else -> Color.parseColor("#795548") // Brown Main
        }
        header.setBackgroundColor(themeColor)
        backButton.backgroundTintList = ColorStateList.valueOf(themeColor)

        // Apply Window Insets for padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.resultRoot)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraTopPadding = (20 * resources.displayMetrics.density).toInt()
            header.setPadding(header.paddingLeft, systemBars.top + extraTopPadding, header.paddingRight, header.paddingBottom)
            cardContent.setPadding(cardContent.paddingLeft, cardContent.paddingTop, cardContent.paddingRight, systemBars.bottom)
            insets
        }

        // Tab Listeners
        tabDescription.setOnClickListener { selectTab(1) }
        tabUses.setOnClickListener { selectTab(2) }
        tabAdvantages.setOnClickListener { selectTab(3) }

        // Default Tab
        selectTab(1)

        backArrow.setOnClickListener { finish() }
        backButton.setOnClickListener { finish() }
    }

    private fun selectTab(index: Int) {
        tabDescription.isSelected = index == 1
        tabUses.isSelected = index == 2
        tabAdvantages.isSelected = index == 3

        contentContainer.removeAllViews()

        when (index) {
            1 -> {
                sectionTitle.text = "Soil Description"
                showDescription()
            }
            2 -> {
                sectionTitle.text = "Primary Uses"
                showUses()
            }
            3 -> {
                sectionTitle.text = "Key Advantages"
                showAdvantages()
            }
        }
    }

    private fun showDescription() {
        when (currentSoilType) {
            "Yellow Soil" -> {
                addContentItem(1, "Hydrated Iron Oxides", "Yellow soil has a yellowish color because of hydrated iron oxides.")
                addContentItem(2, "Regions", "It is commonly found in regions with high rainfall.")
                addContentItem(3, "Texture", "It is generally acidic, well-drained, and low in nutrients and organic matter.")
            }
            "Red Soil" -> {
                addContentItem(1, "Iron Oxide Content", "Red soil gets its reddish color from a high iron oxide content.")
                addContentItem(2, "Composition", "It is usually sandy to loamy, porous, and well-drained.")
                addContentItem(3, "Nutrients", "It is generally low in nitrogen, phosphorus, and organic matter.")
            }
            "Black Soil" -> {
                addContentItem(1, "Clay and Organic Matter", "Black soil is dark-colored due to its high clay and organic matter content.")
                addContentItem(2, "Moisture Retention", "It retains moisture very well and becomes sticky when wet.")
                addContentItem(3, "Nutrients", "It is rich in calcium, magnesium, and potassium but relatively low in phosphorus.")
            }
        }
    }

    private fun showUses() {
        when (currentSoilType) {
            "Yellow Soil" -> {
                addContentItem(1, "Plantations", "Suitable for tea, coffee, rubber, and cashew cultivation.")
                addContentItem(2, "Crops", "Can be used for growing maize, rice, and fruits with proper fertilization.")
                addContentItem(3, "Forestry", "Used in forestry and horticulture after soil improvement.")
            }
            "Red Soil" -> {
                addContentItem(1, "Basic Crops", "Suitable for growing peanuts, millet, maize, potatoes, and pulses.")
                addContentItem(2, "Supported Crops", "Can support crops like rice and wheat when fertilizers and irrigation are provided.")
                addContentItem(3, "Gardening", "Used in gardening and tree plantations.")
            }
            "Black Soil" -> {
                addContentItem(1, "Cotton", "Excellent for growing cotton (also called 'black cotton soil').")
                addContentItem(2, "Cash Crops", "Suitable for sugarcane, soybean, wheat, sunflower, and sorghum.")
                addContentItem(3, "Dry Areas", "Ideal for farming in areas with limited rainfall because it holds water well.")
            }
        }
    }

    private fun showAdvantages() {
        when (currentSoilType) {
            "Yellow Soil" -> {
                addContentItem(1, "Workability", "Well-drained and easy to work with.")
                addContentItem(2, "Plantation Support", "Suitable for plantation crops such as tea, coffee, and rubber.")
                addContentItem(3, "Versatility", "Can support various crops when improved with fertilizers and organic matter.")
            }
            "Red Soil" -> {
                addContentItem(1, "Drainage", "Well-drained, reducing the risk of waterlogging.")
                addContentItem(2, "Ease of Cultivation", "Easy to cultivate because it is light and porous.")
                addContentItem(3, "Variety", "Suitable for a variety of crops with proper irrigation and fertilizers.")
            }
            "Black Soil" -> {
                addContentItem(1, "Rich Nutrients", "Rich in essential nutrients like calcium, magnesium, and potassium.")
                addContentItem(2, "Water Holding", "Excellent water-holding capacity, making it suitable for dry regions.")
                addContentItem(3, "Fertility", "Highly fertile and ideal for many cash crops, especially cotton.")
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
            setTextColor(Color.parseColor("#795548"))
            setBackgroundResource(R.drawable.circle_number_bg)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                (32 * resources.displayMetrics.density).toInt(),
                (32 * resources.displayMetrics.density).toInt()
            ).apply {
                marginEnd = (16 * resources.displayMetrics.density).toInt()
            }
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }

        val descView = TextView(this).apply {
            text = description
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setLineSpacing(4f, 1f)
        }

        textLayout.addView(titleView)
        textLayout.addView(descView)

        itemLayout.addView(numberCircle)
        itemLayout.addView(textLayout)

        contentContainer.addView(itemLayout)
    }
}