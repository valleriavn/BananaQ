package com.example.bananaq

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.content.res.ColorStateList

import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

import data.DiseaseRepository
import ml.DiseaseClassifier
import ml.TFLiteModel
import model.ClassificationResult
import model.ConfidenceLevel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.core.graphics.toColorInt

class ScannerActivity : AppCompatActivity() {

    private lateinit var viewFinder: View
    private lateinit var fullResultCard: View
    private lateinit var resultContentContainer: LinearLayout
    private lateinit var resultSectionTitle: TextView

    private lateinit var tabSymptoms: TextView
    private lateinit var tabTreatment: TextView
    private lateinit var tabPrevention: TextView

    private lateinit var resultDiseaseName: TextView
    private lateinit var resultScientificName: TextView
    private lateinit var resultAccuracyValue: TextView
    private lateinit var resultAccuracyProgress: ProgressBar

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private lateinit var liteModel: TFLiteModel
    private lateinit var classifier: DiseaseClassifier
    private lateinit var diseaseRepository: DiseaseRepository

    private var classificationResult: ClassificationResult? = null

    private var isProcessing = false

    private val worker = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var imageCapture: androidx.camera.core.ImageCapture? = null
    private var camera: androidx.camera.core.Camera? = null
    private var cameraProvider: androidx.camera.lifecycle.ProcessCameraProvider? = null
    private var selectedImage: String? = null
    private var scanId = java.util.UUID.randomUUID().toString()
    private var capturePending = false

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(this, "Camera permission denied. You can still select a photo.", Toast.LENGTH_LONG).show()
        }

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {
                    // Some providers only issue a temporary grant.
                }
                loadSelectedImage(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scanner)
        applySystemInsets()
        initializeViews()
        diseaseRepository = DiseaseRepository(applicationContext)
        setupBottomNavigation()
        setupBottomSheet()
        findViewById<View>(R.id.btnBack).setOnClickListener {
            if (fullResultCard.visibility == View.VISIBLE) hideResult() else finish()
        }
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fullResultCard.visibility == View.VISIBLE) hideResult() else finish()
            }
        })
        findViewById<View>(R.id.captureCircle).setOnClickListener { capturePhoto() }
        findViewById<View>(R.id.btnGallery).setOnClickListener {
            if (!isProcessing && !capturePending) openGallery()
        }
        tabSymptoms.setOnClickListener { selectTab(1) }
        tabTreatment.setOnClickListener { selectTab(2) }
        tabPrevention.setOnClickListener { selectTab(3) }
        selectedImage = savedInstanceState?.getString("selectedImage")
        scanId = savedInstanceState?.getString("scanId") ?: scanId
        val savedDisease = savedInstanceState?.getString("resultDisease")
        if (savedDisease != null) {
            val confidence = savedInstanceState.getFloat("resultConfidence")
            classificationResult = ClassificationResult(savedDisease, confidence,
                ConfidenceLevel.fromConfidence(confidence), savedInstanceState.getBoolean("resultValid"))
            displayResult(classificationResult!!)
        } else if (selectedImage != null) {
            loadSelectedImage(Uri.parse(selectedImage), restoring = true)
        } else if (savedInstanceState == null) {
            handleIncomingData()
        }
        if (savedInstanceState == null && intent.getStringExtra("SOURCE") == "gallery") openGallery()
        else if (intent.getStringExtra("DISEASE_NAME") == null) openCamera()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val future = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (isDestroyed || isFinishing) return@addListener
            try {
                val provider = future.get()
                cameraProvider = provider
                val selector = when {
                    provider.hasCamera(androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA) ->
                        androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                    provider.hasCamera(androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA) ->
                        androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> throw IllegalStateException("No camera is available")
                }
                val previewView = viewFinder as androidx.camera.view.PreviewView
                previewView.implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
                val preview = androidx.camera.core.Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                val capture = androidx.camera.core.ImageCapture.Builder()
                    .setCaptureMode(androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                provider.unbindAll()
                camera = provider.bindToLifecycle(this, selector, preview, capture)
                imageCapture = capture
                findViewById<View>(R.id.btnFlash).apply {
                    isEnabled = camera?.cameraInfo?.hasFlashUnit() == true
                    alpha = if (isEnabled) 1f else 0.4f
                    setOnClickListener {
                        val activeCamera = camera ?: return@setOnClickListener
                        val enabled = activeCamera.cameraInfo.torchState.value != androidx.camera.core.TorchState.ON
                        activeCamera.cameraControl.enableTorch(enabled)
                    }
                }
            } catch (error: Exception) {
                Toast.makeText(this, "Camera unavailable. Select a photo instead.", Toast.LENGTH_LONG).show()
                Log.e("ScannerActivity", "Camera initialization failed", error)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        if (isProcessing || capturePending) return
        val capture = imageCapture
        if (capture == null) {
            openCamera()
            return
        }
        capture.targetRotation = viewFinder.display?.rotation ?: android.view.Surface.ROTATION_0
        val file = try {
            java.io.File.createTempFile("scan_", ".jpg", cacheDir)
        } catch (_: java.io.IOException) {
            Toast.makeText(this, "Unable to save photo. Check available storage.", Toast.LENGTH_LONG).show()
            return
        }
        capturePending = true
        capture.takePicture(androidx.camera.core.ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(this), object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: androidx.camera.core.ImageCapture.OutputFileResults) {
                    capturePending = false
                    if (!isDestroyed && !isFinishing) loadSelectedImage(Uri.fromFile(file))
                }
                override fun onError(error: androidx.camera.core.ImageCaptureException) {
                    capturePending = false
                    file.delete()
                    if (!isDestroyed) Toast.makeText(this@ScannerActivity, "Unable to capture photo. Try again.", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun openGallery() {
        try {
            imagePicker.launch(arrayOf("image/*"))
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "No photo picker is installed.", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadSelectedImage(uri: Uri, restoring: Boolean = false) {
        if (isProcessing) return
        if (!restoring) scanId = java.util.UUID.randomUUID().toString()
        val currentScanId = scanId
        selectedImage = uri.toString()
        intent.removeExtra("LIBRARY")
        intent.removeExtra("DISEASE_NAME")
        intent.removeExtra("CONFIDENCE")
        classificationResult = null
        fullResultCard.visibility = View.GONE
        isProcessing = true
        findViewById<TextView>(R.id.tvInstruction).text = "Processing photo..."
        worker.execute {
            var bitmap: Bitmap? = null
            try {
                if (!::liteModel.isInitialized) liteModel = TFLiteModel(applicationContext)
                if (!::classifier.isInitialized) classifier = DiseaseClassifier(liteModel)
                bitmap = ml.ScanImageLoader.load(applicationContext, uri)
                val result = classifier.classify(bitmap)
                data.ScanHistoryStore(applicationContext).add(result, currentScanId)
                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
                        classificationResult = result
                        displayResult(result)
                    }
                }
            } catch (error: Exception) {
                reportScanError(error)
            } catch (error: LinkageError) {
                reportScanError(error)
            } finally {
                bitmap?.recycle()
                runOnUiThread {
                    isProcessing = false
                    if (!isDestroyed) findViewById<TextView>(R.id.tvInstruction).text = "Align the banana leaf within the frame to scan"
                }
            }
        }
    }

    private fun reportScanError(error: Throwable) {
        Log.e("ScannerActivity", "Scan failed", error)
        runOnUiThread {
            if (!isDestroyed && !isFinishing) Toast.makeText(this,
                "Unable to scan this photo. Try another image or restart the app.", Toast.LENGTH_LONG).show()
        }
    }

    private fun displayResult(result: ClassificationResult) {
        if (result.isValid) showFullResult() else showUncertainResult(result)
    }

    private fun handleIncomingData() {
        val diseaseName = intent.getStringExtra("DISEASE_NAME") ?: return
        if (diseaseRepository.getDiseaseInfo(diseaseName) == null) return
        val confidence = (intent.getIntExtra("CONFIDENCE", 0) / 100f).coerceIn(0f, 1f)
        val level = ConfidenceLevel.fromConfidence(confidence)
        classificationResult = ClassificationResult(diseaseName, confidence, level,
            intent.getBooleanExtra("LIBRARY", false) || level.isReliable)
        displayResult(classificationResult!!)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("selectedImage", selectedImage)
        outState.putString("scanId", scanId)
        classificationResult?.let {
            outState.putString("resultDisease", it.diseaseName)
            outState.putFloat("resultConfidence", it.confidence)
            outState.putBoolean("resultValid", it.isValid)
        }
        super.onSaveInstanceState(outState)
    }
    private fun initializeViews() {
        viewFinder =
            findViewById(R.id.viewFinder)
        fullResultCard =
            findViewById(R.id.fullResultCard)
        resultContentContainer =
            findViewById(R.id.resultContentContainer)
        resultSectionTitle =
            findViewById(R.id.resultSectionTitle)
        tabSymptoms =
            findViewById(R.id.tabSymptoms)
        tabTreatment =
            findViewById(R.id.tabTreatment)
        tabPrevention =
            findViewById(R.id.tabPrevention)
        resultDiseaseName =
            findViewById(R.id.resultDiseaseName)
        resultScientificName =
            findViewById(R.id.resultScientificName)
        resultAccuracyValue =
            findViewById(R.id.resultAccuracyValue)
        resultAccuracyProgress =
            findViewById(R.id.resultAccuracyProgress)
        fullResultCard.visibility = View.GONE
        val defaultColor =
            "#F2EBDC".toColorInt()
        tabSymptoms.backgroundTintList =
            ColorStateList.valueOf(defaultColor)
        tabTreatment.backgroundTintList =
            ColorStateList.valueOf(defaultColor)
        tabPrevention.backgroundTintList =
            ColorStateList.valueOf(defaultColor)
    }
    private fun showFullResult() {
        val result =
            classificationResult
                ?: return
        if (!result.isValid) {
            showUncertainResult(result)
            return
        }
        fullResultCard.visibility =
            View.VISIBLE
        bottomSheetBehavior.state =
            BottomSheetBehavior.STATE_COLLAPSED
        resultDiseaseName.text =
            result.diseaseName
        val diseaseInfo =
            diseaseRepository.getDiseaseInfo(
                result.diseaseName
            )
        resultScientificName.text =
            diseaseInfo?.scientificName
                ?: "Unknown"
        val confidenceInt =
            (
                    result.confidence * 100
                    )
                .coerceIn(
                    0f,
                    100f
                )
                .toInt()
        resultAccuracyValue.text =
            "$confidenceInt%"
        resultAccuracyProgress.progress =
            confidenceInt
        if (intent.getBooleanExtra("LIBRARY", false)) {
            resultAccuracyValue.text = "Library"
            resultAccuracyProgress.visibility = View.GONE
        } else {
            resultAccuracyProgress.visibility = View.VISIBLE
        }
        selectTab(1)
    }
    private fun showUncertainResult(
        result: ClassificationResult
    ) {
        fullResultCard.visibility =
            View.VISIBLE
        bottomSheetBehavior.state =
            BottomSheetBehavior.STATE_COLLAPSED
        resultDiseaseName.text =
            "Unable to confidently identify"
        resultScientificName.text =
            "Please capture another image with better lighting and focus."
        val confidenceInt =
            (
                    result.confidence * 100
                    )
                .coerceIn(
                    0f,
                    100f
                )
                .toInt()
        resultAccuracyValue.text =
            "$confidenceInt%"
        resultAccuracyProgress.progress =
            confidenceInt
        resultSectionTitle.text =
            "Try another scan"
        resultContentContainer
            .removeAllViews()
        addContentItem(
            1,
            "Use good lighting",
            "Make sure the banana leaf is clearly visible and well illuminated."
        )
        addContentItem(
            2,
            "Keep the leaf in focus",
            "Avoid blurry images and try to keep the affected area clearly visible."
        )
        addContentItem(
            3,
            "Show the leaf clearly",
            "Avoid excessive background objects, shadows, or obstructions."
        )
        updateTabStyle(
            tabSymptoms,
            false,
            Color.parseColor("#F2D597"),
            Color.parseColor("#F2EBDC")
        )
        updateTabStyle(
            tabTreatment,
            false,
            Color.parseColor("#F2D597"),
            Color.parseColor("#F2EBDC")
        )
        updateTabStyle(
            tabPrevention,
            false,
            Color.parseColor("#F2D597"),
            Color.parseColor("#F2EBDC")
        )
    }
    private fun setupBottomSheet() {
        bottomSheetBehavior =
            BottomSheetBehavior.from(
                fullResultCard as CardView
            )
        val extraDetails =
            findViewById<View>(
                R.id.extraDetailsLayout
            )
        bottomSheetBehavior.state =
            BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.addBottomSheetCallback(
            object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(
                    bottomSheet: View,
                    newState: Int
                ) {
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            extraDetails.visibility =
                                View.VISIBLE
                            extraDetails.alpha = 1f
                        }
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            extraDetails.visibility =
                                View.INVISIBLE
                            extraDetails.alpha = 0f
                        }
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            fullResultCard.visibility =
                                View.GONE
                        }
                        else -> {}
                    }
                }
                override fun onSlide(
                    bottomSheet: View,
                    slideOffset: Float
                ) {
                    if (slideOffset > 0f) {
                        extraDetails.visibility =
                            View.VISIBLE
                        extraDetails.alpha =
                            slideOffset.coerceIn(
                                0f,
                                1f
                            )
                    }
                }
            }
        )
    }
    private fun selectTab(
        index: Int
    ) {
        val result =
            classificationResult
                ?: return
        if (!result.isValid) return
        val info =
            diseaseRepository.getDiseaseInfo(
                result.diseaseName
            )
                ?: return
        val selectedColor =
            Color.parseColor("#F2D597")
        val unselectedColor =
            Color.parseColor("#F2EBDC")
        updateTabStyle(
            tabSymptoms,
            index == 1,
            selectedColor,
            unselectedColor
        )
        updateTabStyle(
            tabTreatment,
            index == 2,
            selectedColor,
            unselectedColor
        )
        updateTabStyle(
            tabPrevention,
            index == 3,
            selectedColor,
            unselectedColor
        )
        resultContentContainer
            .removeAllViews()
        when (index) {
            1 -> {
                resultSectionTitle.text =
                    if (
                        result.diseaseName ==
                        "Healthy"
                    ) {
                        "Leaf Condition"
                    } else {
                        "Visual Characteristics"
                    }
                showSymptoms(info)
            }
            2 -> {
                resultSectionTitle.text =
                    "Recommended actions"
                showTreatment(info)
            }
            3 -> {
                resultSectionTitle.text =
                    "Best practices to avoid spread"
                showPrevention(info)
            }
        }
    }
    private fun updateTabStyle(
        textView: TextView,
        isSelected: Boolean,
        selectedColor: Int,
        unselectedColor: Int
    ) {
        textView.backgroundTintList =
            ColorStateList.valueOf(
                if (isSelected)
                    selectedColor
                else
                    unselectedColor
            )
        textView.setTypeface(
            null,
            if (isSelected)
                Typeface.BOLD
            else
                Typeface.NORMAL
        )
    }
    private fun showSymptoms(
        info: model.DiseaseInfo
    ) {
        info.symptoms.forEachIndexed {
                index,
                tip ->
            addContentItem(
                index + 1,
                tip.title,
                tip.description
            )
        }
    }
    private fun showTreatment(
        info: model.DiseaseInfo
    ) {
        info.treatment.forEachIndexed {
                index,
                tip ->
            addContentItem(
                index + 1,
                tip.title,
                tip.description
            )
        }
    }
    private fun showPrevention(
        info: model.DiseaseInfo
    ) {
        info.prevention.forEachIndexed {
                index,
                tip ->
            addContentItem(
                index + 1,
                tip.title,
                tip.description
            )
        }
    }
    private fun addContentItem(
        number: Int,
        title: String,
        description: String
    ) {
        val density =
            resources.displayMetrics.density
        val itemLayout =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                setPadding(
                    0,
                    0,
                    0,
                    (16 * density).toInt()
                )
            }
        val numberCircle =
            TextView(this).apply {
                text =
                    number.toString()
                gravity =
                    Gravity.CENTER
                setTextColor(
                    Color.parseColor("#4A773C")
                )
                setBackgroundResource(
                    R.drawable.rounded_button_bg
                )
                backgroundTintList =
                    ColorStateList.valueOf(
                        Color.parseColor("#F2D597")
                    )
                textSize = 12f
                layoutParams =
                    LinearLayout.LayoutParams(
                        (28 * density).toInt(),
                        (28 * density).toInt()
                    ).apply {
                        marginEnd =
                            (16 * density).toInt()
                    }
            }
        val textLayout =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }
        val titleView =
            TextView(this).apply {
                text = title
                textSize = 14f
                setTextColor(Color.BLACK)
                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }
        val descView =
            TextView(this).apply {
                text = description
                textSize = 12f
                setTextColor(
                    Color.parseColor("#666666")
                )
            }
        textLayout.addView(
            titleView
        )
        textLayout.addView(
            descView
        )
        itemLayout.addView(
            numberCircle
        )
        itemLayout.addView(
            textLayout
        )
        resultContentContainer.addView(
            itemLayout
        )
    }
    private fun hideResult() {
        classificationResult = null
        selectedImage = null
        fullResultCard.visibility =
            View.GONE
        if (
            ::bottomSheetBehavior.isInitialized
        ) {
            bottomSheetBehavior.state =
                BottomSheetBehavior.STATE_HIDDEN
        }
    }
    private fun setupBottomNavigation() {
        val bottomNavigation =
            findViewById<BottomNavigationView>(
                R.id.bottomNavigation
            )
        bottomNavigation.selectedItemId =
            R.id.nav_scan
        bottomNavigation.setOnItemSelectedListener {
                item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(
                        Intent(
                            this,
                            MainActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                    finish()
                    true
                }
                R.id.nav_scan -> true
                R.id.nav_history -> {
                    startActivity(
                        Intent(
                            this,
                            HistoryActivity::class.java
                        )
                    )
                    finish()
                    true
                }
                R.id.nav_feedback -> {
                    startActivity(
                        Intent(
                            this,
                            FeedbackActivity::class.java
                        )
                    )
                    finish()
                    true
                }
                else -> false
            }
        }
    }
    override fun onDestroy() {
        cameraProvider?.unbindAll()
        // Close on the inference queue so the interpreter cannot close during a scan.
        worker.execute { if (::liteModel.isInitialized) liteModel.close() }
        worker.shutdown()
        super.onDestroy()
    }
}
