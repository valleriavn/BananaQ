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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var tvPrediction: TextView
    private lateinit var predictionCard: View
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var interpreter: Interpreter
    
    private val labels = arrayOf("Black Sigatoka", "Panama Disease", "Cordana Leaf Spot")
    private var lastPrediction = ""
    private var lastConfidence = 0f

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
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
        tvPrediction = findViewById(R.id.tvPrediction)
        predictionCard = findViewById(R.id.predictionCard)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.captureCircle).setOnClickListener {
            if (lastPrediction.isNotEmpty()) {
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("SOIL_TYPE", lastPrediction)
                    putExtra("CONFIDENCE", lastConfidence * 100)
                }
                startActivity(intent)
            }
        }

        findViewById<View>(R.id.btnGallery).setOnClickListener {
            // Gallery logic could go here
            Toast.makeText(this, "Gallery opening...", Toast.LENGTH_SHORT).show()
        }

        try {
            interpreter = Interpreter(loadModelFile())
        } catch (e: Exception) {
            Log.e("Scanner", "Model failed to load", e)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        checkPermissionAndStart()
        setupBottomNavigation()
        
        predictionCard.setOnClickListener {
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra("SOIL_TYPE", lastPrediction)
                putExtra("CONFIDENCE", lastConfidence * 100)
            }
            startActivity(intent)
        }
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
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
                if (lastConfidence > 0.7) {
                    predictionCard.visibility = View.VISIBLE
                    tvPrediction.text = "Prediction: $lastPrediction (${(lastConfidence * 100).toInt()}%)"
                } else {
                    predictionCard.visibility = View.GONE
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
