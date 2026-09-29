package com.example.bananaq.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.bananaq.UserActionLogger
import com.example.bananaq.ml.DiseaseClassifier
import com.example.bananaq.ml.ScanImageLoader
import com.example.bananaq.ml.TFLiteModel
import com.example.bananaq.model.ClassificationResult
import java.io.File

data class ProcessedClassification(
    val result: ClassificationResult,
    val savedImageUri: String?
)

/** Owns model lifecycle and persistence so ScannerActivity only coordinates UI and camera state. */
class ScanProcessor(context: Context) {
    private val appContext = context.applicationContext
    private var model: TFLiteModel? = null
    private var classifier: DiseaseClassifier? = null

    fun load(uri: Uri): Bitmap = ScanImageLoader.load(appContext, uri)

    fun classifyAndStore(bitmap: Bitmap, scanId: String): ProcessedClassification {
        val activeModel = model ?: TFLiteModel(appContext).also { model = it }
        val activeClassifier = classifier ?: DiseaseClassifier(activeModel).also { classifier = it }
        val result = activeClassifier.classify(bitmap)
        var savedImageUri: String? = null
        try {
            val directory = File(appContext.filesDir, "scan_photos")
            check(directory.isDirectory || directory.mkdirs())
            val photo = File.createTempFile("scan_", ".jpg", directory)
            photo.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            val candidateUri = Uri.fromFile(photo).toString()
            val inserted = ScanHistoryStore(appContext).add(result, scanId, candidateUri)
            if (inserted) savedImageUri = candidateUri else photo.delete()
            UserActionLogger.logScanEvent(scanId, result.diseaseName, result.confidence, result.isValid)
        } catch (error: Exception) {
            Log.w(TAG, "Unable to save scan history", error)
        }
        return ProcessedClassification(result, savedImageUri)
    }

    fun close() {
        classifier = null
        model?.close()
        model = null
    }

    private companion object {
        const val TAG = "ScanProcessor"
    }
}
