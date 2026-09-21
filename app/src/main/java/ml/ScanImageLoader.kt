package ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

object ScanImageLoader {
    /** Bound decoded memory before allocating pixels, including on older low-memory phones. */
    fun load(context: Context, uri: Uri): Bitmap {
        fun stream() = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open image")
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        stream().use { BitmapFactory.decodeStream(it, null, options) }
        require(options.outWidth > 0 && options.outHeight > 0) { "Unsupported image format" }
        options.inSampleSize = sampleSize(options.outWidth, options.outHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmap = stream().use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalArgumentException("Cannot decode image")
        try {
            val orientation = stream().use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
                }
            }
            if (matrix.isIdentity) return bitmap
            val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (oriented !== bitmap) bitmap.recycle()
            return oriented
        } catch (_: java.io.IOException) {
            // Images without readable EXIF are still valid image inputs.
            return bitmap
        }
    }

    internal fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > 1024 || height / sample > 1024) sample *= 2
        return sample
    }
}
