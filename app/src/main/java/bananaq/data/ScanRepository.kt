package bananaq.data

import android.graphics.Bitmap
import android.net.Uri

/** The scanner's data boundary; implementations own inference and local persistence. */
interface ScanRepository : AutoCloseable {
    fun load(uri: Uri): Bitmap
    fun classifyAndStore(bitmap: Bitmap, scanId: String): ProcessedClassification
    override fun close()
}
