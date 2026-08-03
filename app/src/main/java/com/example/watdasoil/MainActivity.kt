package com.example.watdasoil

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var interpreter: Interpreter
    private lateinit var soilImage: ImageView
    private lateinit var selectButton: LinearLayout
    private lateinit var cameraButton: LinearLayout
    private lateinit var detectButton: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var dayMonthText: TextView
    
    private var selectedBitmap: Bitmap? = null

    private val labels = arrayOf(
        "Black Soil",
        "Red Soil",
        "Yellow Soil"
    )

    private val handler = Handler(Looper.getMainLooper())
    private val timeUpdater = object : Runnable {
        override fun run() {
            updateDateTime()
            handler.postDelayed(this, 1000)
        }
    }

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                loadSelectedImage(uri)
            }
        }

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap: Bitmap? ->
            if (bitmap != null) {
                selectedBitmap = bitmap
                soilImage.setImageBitmap(bitmap)
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                cameraLauncher.launch(null)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val header = findViewById<View>(R.id.header)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            header.setPadding(header.paddingLeft, systemBars.top, header.paddingRight, header.paddingBottom)
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        soilImage = findViewById(R.id.soilImage)
        selectButton = findViewById(R.id.selectButton)
        cameraButton = findViewById(R.id.cameraButton)
        detectButton = findViewById(R.id.detectButton)
        statusText = findViewById(R.id.statusText)
        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)
        dayMonthText = findViewById(R.id.dayMonthText)

        try {
            interpreter = Interpreter(loadModelFile())
        } catch (error: Exception) {
            statusText.visibility = View.VISIBLE
            statusText.text = "Model loading error: ${error.message}"
        }

        selectButton.setOnClickListener {
            imagePicker.launch("image/*")
        }
        
        cameraButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(null)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        detectButton.setOnClickListener {
            val bitmap = selectedBitmap
            if (bitmap == null) {
                statusText.visibility = View.VISIBLE
                statusText.text = "Please select or capture a soil image first."
            } else if (!::interpreter.isInitialized) {
                statusText.visibility = View.VISIBLE
                statusText.text = "The AI model is not available."
            } else {
                classifyImage(bitmap)
            }
        }

        handler.post(timeUpdater)
    }

    private fun updateDateTime() {
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE, yyyy", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMMM dd", Locale.getDefault())

        timeText.text = timeFormat.format(calendar.time)
        dateText.text = dayFormat.format(calendar.time)
        dayMonthText.text = monthFormat.format(calendar.time)
    }

    private fun loadSelectedImage(uri: Uri) {
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    selectedBitmap = bitmap
                    soilImage.setImageBitmap(bitmap)
                }
            }
        } catch (error: Exception) {
            statusText.visibility = View.VISIBLE
            statusText.text = "Image error: ${error.message}"
        }
    }

    private fun classifyImage(bitmap: Bitmap) {
        try {
            val resizedBitmap = bitmap.scale(224, 224, true)
            val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)
            val output = Array(1) { FloatArray(3) }
            interpreter.run(inputBuffer, output)

            val probabilities = output[0]
            var highestIndex = 0
            for (index in probabilities.indices) {
                if (probabilities[index] > probabilities[highestIndex]) {
                    highestIndex = index
                }
            }

            val prediction = labels[highestIndex]
            val confidence = probabilities[highestIndex] * 100
            
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra("SOIL_TYPE", prediction)
                putExtra("CONFIDENCE", confidence)
                putExtra("SOIL_IMAGE", resizedBitmap)
            }
            startActivity(intent)
            
        } catch (error: Exception) {
            statusText.visibility = View.VISIBLE
            statusText.text = "Prediction error: ${error.message}"
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3)
        inputBuffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(224 * 224)
        bitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224)
        for (pixel in pixels) {
            val red = (pixel shr 16 and 0xFF) / 255.0f
            val green = (pixel shr 8 and 0xFF) / 255.0f
            val blue = (pixel and 0xFF) / 255.0f
            inputBuffer.putFloat(red)
            inputBuffer.putFloat(green)
            inputBuffer.putFloat(blue)
        }
        inputBuffer.rewind()
        return inputBuffer
    }

    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = assets.openFd("watdasoil_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeUpdater)
        if (::interpreter.isInitialized) {
            interpreter.close()
        }
    }
}