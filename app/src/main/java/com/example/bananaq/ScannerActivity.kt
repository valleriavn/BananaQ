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
import com.google.android.material.bottomsheet.BottomSheetBehavior

class ScannerActivity : LocaleAwareActivity() {

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
    private var photoBitmap: Bitmap? = null
    private var detailsExpanded = false

    private fun showPhotoMode() {
        viewFinder.visibility = View.INVISIBLE
        cameraProvider?.unbindAll()
        imageCapture = null
        camera = null
        findViewById<View>(R.id.scannedPhoto).visibility = View.VISIBLE
        findViewById<View>(R.id.cameraScrim).apply {
            visibility = View.VISIBLE
            alpha = 0.2f
        }
        findViewById<View>(R.id.btnBack).visibility = View.VISIBLE
        findViewById<View>(R.id.scanResultTitle).visibility = View.GONE
    }

    private fun showCompactResultMode() {
        showPhotoMode()
        findViewById<View>(R.id.cameraScrim).alpha = 0.3f
        for (id in intArrayOf(R.id.scanFrame, R.id.tvInstruction, R.id.controlsLayout)) {
            findViewById<View>(id).visibility = View.VISIBLE
        }
        findViewById<View>(R.id.controlsLayout).alpha = 0.55f
        findViewById<View>(R.id.captureCircle).isEnabled = false
        findViewById<View>(R.id.btnGallery).isEnabled = false
        findViewById<View>(R.id.btnFlash).isEnabled = false
    }

    private fun showDetailsMode() {
        showPhotoMode()
        findViewById<View>(R.id.cameraScrim).alpha = 0.18f
        findViewById<View>(R.id.scanResultTitle).visibility = View.VISIBLE
        for (id in intArrayOf(R.id.scanFrame, R.id.tvInstruction, R.id.controlsLayout)) {
            findViewById<View>(id).visibility = View.GONE
        }
    }

    private fun showPhoto(bitmap: Bitmap) {
        // The displayed bitmap is independent of the inference bitmap recycled by the worker.
        val previous = photoBitmap
        photoBitmap = bitmap
        findViewById<android.widget.ImageView>(R.id.scannedPhoto).setImageBitmap(bitmap)
        if (previous !== bitmap && previous?.isRecycled == false) previous.recycle()
    }

    private fun restorePhoto() {
        val uri = selectedImage ?: return
        worker.execute {
            try {
                val bitmap = ml.ScanImageLoader.load(applicationContext, Uri.parse(uri))
                runOnUiThread {
                    if (!isDestroyed && !isFinishing && selectedImage == uri) showPhoto(bitmap)
                    else bitmap.recycle()
                }
            } catch (error: Exception) {
                Log.w("ScannerActivity", "Saved photo unavailable", error)
            }
        }
    }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show()
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
            } else if (selectedImage == null && classificationResult == null) {
                openCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scanner)
        applySystemInsets()
        initializeViews()
        // Use the wrapped Activity context so recommendation JSON follows the
        // language currently selected in BananaQ.
        diseaseRepository = DiseaseRepository(this)
        setupBottomNavigation()
        setupBottomSheet()
        findViewById<View>(R.id.btnBack).setOnClickListener {
            if (classificationResult != null) hideResult() else finish()
        }
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (classificationResult != null) hideResult() else finish()
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
        detailsExpanded = savedInstanceState?.getBoolean("detailsExpanded") ?: false
        if (savedDisease != null) {
            val confidence = savedInstanceState.getFloat("resultConfidence")
            classificationResult = ClassificationResult(savedDisease, confidence,
                ConfidenceLevel.fromConfidence(confidence), savedInstanceState.getBoolean("resultValid"))
            displayResult(classificationResult!!)
            restorePhoto()
        } else if (selectedImage != null) {
            loadSelectedImage(Uri.parse(selectedImage), restoring = true)
        } else if (savedInstanceState == null) {
            handleIncomingData()
        }
        if (savedInstanceState == null && intent.getStringExtra("SOURCE") == "gallery") openGallery()
        else if (classificationResult == null && selectedImage == null) openCamera()
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
            if (isDestroyed || isFinishing || selectedImage != null || classificationResult != null) return@addListener
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
                val group = androidx.camera.core.UseCaseGroup.Builder()
                    .addUseCase(preview).addUseCase(capture)
                previewView.viewPort?.let { group.setViewPort(it) }
                camera = provider.bindToLifecycle(this, selector, group.build())
                imageCapture = capture
                findViewById<View>(R.id.btnFlash).apply {
                    isEnabled = camera?.cameraInfo?.hasFlashUnit() == true
                    alpha = if (isEnabled) 1f else 0.4f
                    setOnClickListener {
                        val activeCamera = camera ?: return@setOnClickListener
                        val enabled = activeCamera.cameraInfo.torchState.value != androidx.camera.core.TorchState.ON
                        activeCamera.cameraControl.enableTorch(enabled)
                        contentDescription = getString(if (enabled) R.string.flash_off else R.string.flash_on)
                    }
                }
            } catch (error: Exception) {
                Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_LONG).show()
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
            Toast.makeText(this, R.string.photo_save_failed, Toast.LENGTH_LONG).show()
            return
        }
        capturePending = true
        findViewById<View>(R.id.captureCircle).isEnabled = false
        capture.takePicture(androidx.camera.core.ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(this), object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: androidx.camera.core.ImageCapture.OutputFileResults) {
                    capturePending = false
                    findViewById<View>(R.id.captureCircle).isEnabled = true
                    if (!isDestroyed && !isFinishing) loadSelectedImage(Uri.fromFile(file))
                }
                override fun onError(error: androidx.camera.core.ImageCaptureException) {
                    capturePending = false
                    findViewById<View>(R.id.captureCircle).isEnabled = true
                    file.delete()
                    if (!isDestroyed) Toast.makeText(this@ScannerActivity, R.string.photo_capture_failed, Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun openGallery() {
        try {
            imagePicker.launch(arrayOf("image/*"))
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, R.string.photo_picker_missing, Toast.LENGTH_LONG).show()
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
        intent.removeExtra("RESULT_VALID")
        classificationResult = null
        detailsExpanded = false
        fullResultCard.visibility = View.GONE
        findViewById<View>(R.id.predictionSummary).visibility = View.GONE
        isProcessing = true
        findViewById<View>(R.id.controlsLayout).alpha = 0.55f
        findViewById<View>(R.id.captureCircle).isEnabled = false
        findViewById<View>(R.id.btnGallery).isEnabled = false
        findViewById<View>(R.id.btnFlash).isEnabled = false
        findViewById<TextView>(R.id.tvInstruction).setText(R.string.processing_photo)
        worker.execute {
            var bitmap: Bitmap? = null
            try {
                val decoded = ml.ScanImageLoader.load(applicationContext, uri)
                bitmap = decoded
                val preview = requireNotNull(decoded.copy(Bitmap.Config.ARGB_8888, false))
                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
                        showPhoto(preview)
                        showPhotoMode()
                    } else preview.recycle()
                }
                if (!::liteModel.isInitialized) liteModel = TFLiteModel(applicationContext)
                if (!::classifier.isInitialized) classifier = DiseaseClassifier(liteModel)
                val result = classifier.classify(decoded)
                var savedPhoto: String? = null
                try {
                    val directory = java.io.File(filesDir, "scan_photos")
                    check(directory.isDirectory || directory.mkdirs())
                    val photo = java.io.File.createTempFile("scan_", ".jpg", directory)
                    photo.outputStream().use { check(decoded.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
                    savedPhoto = Uri.fromFile(photo).toString()
                    data.ScanHistoryStore(applicationContext).add(result, currentScanId, savedPhoto)
                } catch (error: Exception) {
                    Log.w("ScannerActivity", "Unable to save scan history", error)
                }
                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
                        selectedImage = savedPhoto ?: selectedImage
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
                    if (!isDestroyed) findViewById<TextView>(R.id.tvInstruction).setText(R.string.scan_instruction)
                }
            }
        }
    }

    private fun reportScanError(error: Throwable) {
        Log.e("ScannerActivity", "Scan failed", error)
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                hideResult()
                Toast.makeText(this, R.string.scan_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displayResult(result: ClassificationResult) {
        findViewById<TextView>(R.id.scanResultTitle).setText(R.string.scan_result)
        if (result.isValid && detailsExpanded) {
            findViewById<View>(R.id.predictionSummary).visibility = View.GONE
            showFullResult()
        } else if (!result.isValid) {
            showUncertainResult(result)
            fullResultCard.visibility = View.GONE
            showCompactResultMode()
            findViewById<TextView>(R.id.predictionSummary).apply {
                setText(R.string.unable_identify)
                setOnClickListener(null)
                isClickable = false
                revealResultView(this)
            }
        } else {
            fullResultCard.visibility = View.GONE
            showCompactResultMode()
            findViewById<TextView>(R.id.predictionSummary).apply {
                val heading = getString(R.string.prediction_heading,
                    localizedDiseaseName(this@ScannerActivity, result.diseaseName),
                    (result.confidence * 100).toInt())
                val detail = getString(R.string.tap_for_details)
                text = android.text.SpannableString("$heading\n$detail").apply {
                    setSpan(android.text.style.StyleSpan(Typeface.BOLD), 0, heading.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(android.text.style.ForegroundColorSpan(
                        ContextCompat.getColor(this@ScannerActivity, R.color.banana_text_dark)),
                        0, heading.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(android.text.style.RelativeSizeSpan(0.8f), heading.length + 1, length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(android.text.style.ForegroundColorSpan(
                        ContextCompat.getColor(this@ScannerActivity, R.color.banana_muted)),
                        heading.length + 1, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                isClickable = true
                setOnClickListener {
                    detailsExpanded = true
                    visibility = View.GONE
                    showFullResult()
                }
                revealResultView(this)
            }
        }
    }

    private fun revealResultView(view: View) {
        view.animate().cancel()
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.translationY = 12f * resources.displayMetrics.density
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220L)
            .start()
    }

    private fun handleIncomingData() {
        val diseaseName = intent.getStringExtra("DISEASE_NAME") ?: return
        if (diseaseRepository.getDiseaseInfo(diseaseName) == null) return
        val confidence = (intent.getIntExtra("CONFIDENCE", 0) / 100f).coerceIn(0f, 1f)
        val level = ConfidenceLevel.fromConfidence(confidence)
        classificationResult = ClassificationResult(diseaseName, confidence, level,
            intent.getBooleanExtra("LIBRARY", false) ||
                (intent.getBooleanExtra("RESULT_VALID", false) && level.isReliable))
        displayResult(classificationResult!!)
        selectedImage = intent.getStringExtra("IMAGE_URI")
        restorePhoto()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("detailsExpanded", detailsExpanded)
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
            ContextCompat.getColor(this, R.color.banana_yellow)
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
        showDetailsMode()
        fullResultCard.animate().cancel()
        fullResultCard.alpha = 0f
        fullResultCard.visibility = View.VISIBLE
        bottomSheetBehavior.state =
            BottomSheetBehavior.STATE_EXPANDED
        fullResultCard.animate().alpha(1f).setDuration(220L).start()
        resultDiseaseName.text =
            localizedDiseaseName(this, result.diseaseName)
        resultScientificName.visibility = View.VISIBLE
        findViewById<View>(R.id.accuracyLayout).visibility = View.VISIBLE
        findViewById<View>(R.id.extraDetailsLayout).apply {
            visibility = View.VISIBLE
            alpha = 1f
        }
        val diseaseInfo =
            diseaseRepository.getDiseaseInfo(
                result.diseaseName
            )
        resultScientificName.text =
            diseaseInfo?.scientificName
                ?: getString(R.string.unknown_label)
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
            resultAccuracyValue.setText(R.string.library_label)
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
            BottomSheetBehavior.STATE_EXPANDED
        resultDiseaseName.text =
            getString(R.string.unable_identify)
        resultScientificName.text = ""
        resultScientificName.visibility = View.GONE
        resultAccuracyValue.text = ""
        findViewById<View>(R.id.accuracyLayout).visibility = View.GONE
        resultAccuracyProgress.visibility = View.GONE
        resultAccuracyProgress.progress = 0
        resultSectionTitle.text = ""
        findViewById<View>(R.id.extraDetailsLayout).apply {
            visibility = View.GONE
            alpha = 0f
        }
        resultContentContainer.animate().cancel()
        resultSectionTitle.animate().cancel()
        resultContentContainer.alpha = 0f
        resultSectionTitle.alpha = 0f
        resultContentContainer.removeAllViews()
        updateTabStyle(
            tabSymptoms,
            false,
            ContextCompat.getColor(this, R.color.banana_green),
            ContextCompat.getColor(this, R.color.banana_yellow)
        )
        updateTabStyle(
            tabTreatment,
            false,
            ContextCompat.getColor(this, R.color.banana_green),
            ContextCompat.getColor(this, R.color.banana_yellow)
        )
        updateTabStyle(
            tabPrevention,
            false,
            ContextCompat.getColor(this, R.color.banana_green),
            ContextCompat.getColor(this, R.color.banana_yellow)
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
        bottomSheetBehavior.isDraggable = false
        val dragHandle = findViewById<View>(R.id.resultDragHandle)
        dragHandle.setOnClickListener {
            val result = classificationResult ?: return@setOnClickListener
            detailsExpanded = false
            fullResultCard.animate().cancel()
            fullResultCard.animate()
                .alpha(0f)
                .setDuration(140L)
                .withEndAction {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    fullResultCard.alpha = 1f
                    fullResultCard.visibility = View.GONE
                    displayResult(result)
                }
                .start()
        }
        bottomSheetBehavior.addBottomSheetCallback(
            object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(
                    bottomSheet: View,
                    newState: Int
                ) {
                    if (classificationResult?.isValid != true) {
                        extraDetails.visibility = View.GONE
                        extraDetails.alpha = 0f
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            fullResultCard.visibility = View.GONE
                        }
                        return
                    }
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            detailsExpanded = true
                            dragHandle.contentDescription = getString(R.string.collapse_scan_result)
                            extraDetails.visibility =
                                View.VISIBLE
                            extraDetails.alpha = 1f
                        }
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            detailsExpanded = false
                            dragHandle.contentDescription = getString(R.string.expand_scan_result)
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
                    if (classificationResult?.isValid != true) {
                        extraDetails.visibility = View.GONE
                        extraDetails.alpha = 0f
                        return
                    }
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
            ContextCompat.getColor(this, R.color.banana_green)
        val unselectedColor =
            ContextCompat.getColor(this, R.color.banana_yellow)
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
        resultContentContainer.animate().cancel()
        resultSectionTitle.animate().cancel()
        resultContentContainer.alpha = 0f
        resultSectionTitle.alpha = 0f
        resultContentContainer.removeAllViews()
        when (index) {
            1 -> {
                resultSectionTitle.text =
                    if (
                        result.diseaseName ==
                        "Healthy"
                    ) {
                        getString(R.string.leaf_condition)
                    } else {
                        getString(R.string.visual_characteristics)
                    }
                showSymptoms(info)
            }
            2 -> {
                resultSectionTitle.text =
                    getString(R.string.recommended_actions)
                showTreatment(info)
            }
            3 -> {
                resultSectionTitle.text =
                    getString(R.string.prevention_best_practices)
                showPrevention(info)
            }
        }
        resultSectionTitle.animate().alpha(1f).setDuration(160L).start()
        resultContentContainer.animate().alpha(1f).setDuration(180L).start()
    }
    private fun updateTabStyle(
        textView: TextView,
        isSelected: Boolean,
        selectedColor: Int,
        unselectedColor: Int
    ) {
        textView.backgroundTintList =
            ColorStateList.valueOf(
                if (isSelected) selectedColor else unselectedColor
            )
        textView.setTextColor(
            if (isSelected) Color.WHITE
            else ContextCompat.getColor(this, R.color.button_text_black)
        )
        textView.setTypeface(null, Typeface.NORMAL)
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
                    (18 * density).toInt()
                )
            }
        val numberCircle =
            TextView(this).apply {
                text =
                    number.toString()
                gravity =
                    Gravity.CENTER
                setTextColor(
                    ContextCompat.getColor(this@ScannerActivity, R.color.button_text_black)
                )
                setBackgroundResource(
                    R.drawable.result_number_circle
                )
                textSize = 12f
                layoutParams =
                    LinearLayout.LayoutParams(
                        (26 * density).toInt(),
                        (26 * density).toInt()
                    ).apply {
                        marginEnd =
                            (14 * density).toInt()
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
                setTextColor(ContextCompat.getColor(this@ScannerActivity, R.color.banana_body))
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
                    ContextCompat.getColor(this@ScannerActivity, R.color.banana_muted)
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
        resultContentContainer.addView(View(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@ScannerActivity, R.color.banana_yellow_light))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                density.toInt().coerceAtLeast(1)
            ).apply {
                marginStart = (40 * density).toInt()
                bottomMargin = (12 * density).toInt()
            }
        })
    }
    private fun hideResult() {
        findViewById<View>(R.id.predictionSummary).visibility = View.GONE
        classificationResult = null
        selectedImage = null
        findViewById<android.widget.ImageView>(R.id.scannedPhoto).apply {
            setImageDrawable(null)
            visibility = View.GONE
        }
        photoBitmap?.takeIf { !it.isRecycled }?.recycle()
        photoBitmap = null
        findViewById<View>(R.id.scanResultTitle).visibility = View.GONE
        findViewById<View>(R.id.cameraScrim).visibility = View.GONE
        findViewById<View>(R.id.btnBack).visibility = View.GONE
        viewFinder.visibility = View.VISIBLE
        for (id in intArrayOf(R.id.scanFrame, R.id.tvInstruction, R.id.controlsLayout)) {
            findViewById<View>(id).visibility = View.VISIBLE
        }
        findViewById<View>(R.id.controlsLayout).alpha = 1f
        findViewById<View>(R.id.captureCircle).isEnabled = true
        findViewById<View>(R.id.btnGallery).isEnabled = true
        openCamera()
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
            findViewById<RaisedBottomNavigationView>(
                R.id.bottomNavigation
            )
        bottomNavigation.selectedItemId =
            R.id.nav_scan
        bottomNavigation.setOnItemSelectedListener {
                itemId ->
            when (itemId) {
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
                R.id.nav_account -> {
                    startActivity(Intent(this, AccountActivity::class.java))
                    false
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
