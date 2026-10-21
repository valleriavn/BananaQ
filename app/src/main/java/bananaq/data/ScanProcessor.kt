package bananaq.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import bananaq.UserActionLogger
import bananaq.ml.DiseaseClassifier
import bananaq.ml.ScanImageLoader
import bananaq.ml.TFLiteModel
import bananaq.model.ClassificationResult
import java.io.File

data class ProcessedClassification(
    val result: ClassificationResult,
    val savedImageUri: String?
)

/** Owns model lifecycle and persistence so ScannerActivity only coordinates UI and camera state. */
class ScanProcessor(context: Context) : ScanRepository {
    private val appContext = context.applicationContext
    private var model: TFLiteModel? = null
    private var classifier: DiseaseClassifier? = null

    override fun load(uri: Uri): Bitmap = ScanImageLoader.load(appContext, uri)

    override fun classifyAndStore(bitmap: Bitmap, scanId: String): ProcessedClassification {
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
            if (inserted) {
                savedImageUri = candidateUri
                if (result.isValid) RecommendationProvider(appContext).forScan(result.diseaseName, scanId)
            } else photo.delete()
            UserActionLogger.logScanEvent(scanId, result.diseaseName, result.confidence, result.isValid)
        } catch (error: Exception) {
            Log.w(TAG, "Unable to save scan history", error)
        }
        return ProcessedClassification(result, savedImageUri)
    }

    override fun close() {
        classifier = null
        model?.close()
        model = null
    }

    private companion object {
        const val TAG = "ScanProcessor"
    }
}
