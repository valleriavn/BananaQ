package com.example.bananaq

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import android.graphics.Color
import android.view.Gravity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.cardview.widget.CardView
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var fullResultCard: View
    private lateinit var resultContentContainer: LinearLayout
    private lateinit var resultSectionTitle: TextView
    private lateinit var tabSymptoms: TextView
    private lateinit var tabTreatment: TextView
    private lateinit var tabPrevention: TextView
    
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var interpreter: Interpreter
    
    private val labels = arrayOf("Black Sigatoka", "Panama Disease", "Cordana Leaf Spot")
    private var lastPrediction = ""
    private var lastConfidence = 0f

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            if (cameraGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scanner)

        viewFinder = findViewById(R.id.viewFinder)
        fullResultCard = findViewById(R.id.fullResultCard)
        resultContentContainer = findViewById(R.id.resultContentContainer)
        resultSectionTitle = findViewById(R.id.resultSectionTitle)
        tabSymptoms = findViewById(R.id.tabSymptoms)
        tabTreatment = findViewById(R.id.tabTreatment)
        tabPrevention = findViewById(R.id.tabPrevention)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            if (fullResultCard.visibility == View.VISIBLE) {
                fullResultCard.visibility = View.GONE
            } else {
                finish()
            }
        }

        findViewById<View>(R.id.captureCircle).setOnClickListener {
            handleScanClick()
        }

        findViewById<View>(R.id.btnGallery).setOnClickListener {
            Toast.makeText(this, "Gallery opening...", Toast.LENGTH_SHORT).show()
        }

        // Tab Listeners for Result
        tabSymptoms.setOnClickListener { selectTab(1) }
        tabTreatment.setOnClickListener { selectTab(2) }
        tabPrevention.setOnClickListener { selectTab(3) }

        try {
            interpreter = Interpreter(loadModelFile())
        } catch (e: Exception) {
            Log.e("Scanner", "Model failed to load", e)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (hasPermissions()) {
            startCamera()
        }
        
        // Check for incoming data from MainActivity
        val incomingDisease = intent.getStringExtra("DISEASE_NAME")
        val incomingConfidence = intent.getIntExtra("CONFIDENCE", 0)
        if (incomingDisease != null) {
            lastPrediction = incomingDisease
            lastConfidence = incomingConfidence / 100f
            showFullResult()
        }
        
        setupBottomNavigation()

        // Bottom Sheet Behavior
        val behavior = BottomSheetBehavior.from(fullResultCard as CardView)
        val extraDetails = findViewById<View>(R.id.extraDetailsLayout)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    extraDetails.visibility = View.VISIBLE
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    extraDetails.visibility = View.INVISIBLE
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                extraDetails.visibility = View.VISIBLE
                extraDetails.alpha = slideOffset
            }
        })
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun handleScanClick() {
        if (!hasPermissions()) {
            showPermissionExplanationDialog()
        } else if (lastPrediction.isNotEmpty() && lastConfidence > 0.6) {
            showFullResult()
        } else {
            Toast.makeText(this, "Align leaf properly and try again", Toast.LENGTH_SHORT).show()
            startCamera()
        }
    }

    private fun showFullResult() {
        fullResultCard.visibility = View.VISIBLE
        
        findViewById<TextView>(R.id.resultDiseaseName).text = lastPrediction
        findViewById<TextView>(R.id.resultScientificName).text = when(lastPrediction) {
            "Panama Disease" -> "Fusarium oxysporum f. sp. cubense"
            "Black Sigatoka" -> "Mycosphaerella fijiensis"
            "Cordana Leaf Spot" -> "Cordana musae"
            else -> "Musa acuminata"
        }
        val confidenceInt = (lastConfidence * 100).toInt()
        findViewById<TextView>(R.id.resultAccuracyValue).text = "$confidenceInt%"
        findViewById<android.widget.ProgressBar>(R.id.resultAccuracyProgress).progress = confidenceInt
        
        selectTab(1)
    }

    private fun selectTab(index: Int) {
        val selectedColor = Color.parseColor("#F2D597") // banana_yellow
        val unselectedColor = Color.parseColor("#F2EBDC") // light_cream

        tabSymptoms.backgroundTintList = android.content.res.ColorStateList.valueOf(if (index == 1) selectedColor else unselectedColor)
        tabSymptoms.setTypeface(null, if (index == 1) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        
        tabTreatment.backgroundTintList = android.content.res.ColorStateList.valueOf(if (index == 2) selectedColor else unselectedColor)
        tabTreatment.setTypeface(null, if (index == 2) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        
        tabPrevention.backgroundTintList = android.content.res.ColorStateList.valueOf(if (index == 3) selectedColor else unselectedColor)
        tabPrevention.setTypeface(null, if (index == 3) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        resultContentContainer.removeAllViews()

        when (index) {
            1 -> {
                resultSectionTitle.text = "Visual Characteristics"
                showSymptoms()
            }
            2 -> {
                resultSectionTitle.text = "Recommended actions"
                showTreatment()
            }
            3 -> {
                resultSectionTitle.text = "Best practices to avoid spread"
                showPrevention()
            }
        }
    }

    private fun showSymptoms() {
        when (lastPrediction) {
            "Panama Disease" -> {
                addContentItem(1, "Yellowing leaves", "Yellowing starts from the leaf margins, and eventually the whole leaf turns yellow.")
                addContentItem(2, "Brown discoloration", "If you slice the stem, you'll see brown or reddish-brown streaks inside.")
                addContentItem(3, "Wilting and leaf drop", "Lower leaves wilt and collapse first, moving upwards.")
            }
            "Black Sigatoka" -> {
                addContentItem(1, "Small dark spots", "First appears as tiny, dark-brown reddish spots on the underside.")
                addContentItem(2, "Streaks development", "Spots expand into long, dark streaks parallel to veins.")
                addContentItem(3, "Leaf necrosis", "Large areas of the leaf turn brown and dry out.")
            }
            "Cordana Leaf Spot" -> {
                addContentItem(1, "Oval spots", "Large, oval spots with brown centers and bright yellow halos.")
                addContentItem(2, "Zonate patterns", "Spots often show concentric rings and can merge together.")
                addContentItem(3, "Edge infection", "Often starts at the leaf edges where water collects.")
            }
        }
    }

    private fun showTreatment() {
        when (lastPrediction) {
            "Panama Disease" -> {
                addContentItem(1, "Isolate affected plants", "Remove and bag infected plants immediately. Do not compost.")
                addContentItem(2, "Soil Treatment", "Apply calcium cyanamide to reduce fungal load.")
            }
            "Black Sigatoka" -> {
                addContentItem(1, "Fungicide application", "Apply systemic or contact fungicides regularly.")
                addContentItem(2, "Sanitation", "Remove and burn severely infected leaves.")
            }
        }
    }

    private fun showPrevention() {
        when (lastPrediction) {
            "Panama Disease" -> {
                addContentItem(1, "Sanitize tools", "Disinfect knives and spades with 70% alcohol.")
                addContentItem(2, "Improved drainage", "Waterlogged soil accelerates fungal spread.")
            }
        }
    }

    private fun addContentItem(number: Int, title: String, description: String) {
        val itemLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, (16 * resources.displayMetrics.density).toInt())
        }

        val numberCircle = TextView(this).apply {
            text = number.toString()
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor("#4A773C"))
            setBackgroundResource(R.drawable.rounded_button_bg)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F2D597"))
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
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val descView = TextView(this).apply {
            text = description
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
        }

        textLayout.addView(titleView)
        textLayout.addView(descView)
        itemLayout.addView(numberCircle)
        itemLayout.addView(textLayout)
        resultContentContainer.addView(itemLayout)
    }

    private fun showPermissionExplanationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Will you allow BananaQ to access camera and files to scan banana leaves?")
            .setPositiveButton("Allow") { _, _ ->
                val permissions = arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                requestPermissionLauncher.launch(permissions)
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = viewFinder.surfaceProvider
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                Log.e("Scanner", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()?.let {
            Bitmap.createScaledBitmap(it, 224, 224, true)
        }

        if (bitmap != null && ::interpreter.isInitialized) {
            val inputBuffer = convertBitmapToByteBuffer(bitmap)
            val output = Array(1) { FloatArray(3) }
            interpreter.run(inputBuffer, output)

            val probabilities = output[0]
            var maxIndex = 0
            for (i in probabilities.indices) {
                if (probabilities[i] > probabilities[maxIndex]) maxIndex = i
            }

            lastPrediction = labels[maxIndex]
            lastConfidence = probabilities[maxIndex]

            runOnUiThread {
                if (lastConfidence > 0.7 && fullResultCard.visibility != View.VISIBLE) {
                    findViewById<View>(R.id.captureCircle).backgroundTintList = 
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.LTGRAY)
                } else {
                    findViewById<View>(R.id.captureCircle).backgroundTintList = 
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                }
            }
        }
        imageProxy.close()
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(224 * 224)
        bitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)
        for (pixelValue in intValues) {
            byteBuffer.putFloat((pixelValue shr 16 and 0xFF) / 255f)
            byteBuffer.putFloat((pixelValue shr 8 and 0xFF) / 255f)
            byteBuffer.putFloat((pixelValue and 0xFF) / 255f)
        }
        return byteBuffer
    }

    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = assets.openFd("bananaq_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_scan
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_scan -> true
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_feedback -> {
                    startActivity(Intent(this, FeedbackActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        if (::interpreter.isInitialized) interpreter.close()
    }
}
