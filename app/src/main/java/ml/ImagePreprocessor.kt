package ml

import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Tensor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

object ImagePreprocessor {
    // Training uses rescale=1/255. Quantization describes storage, not normalization.
    fun createInputBuffer(bitmap: Bitmap, inputTensor: Tensor): ByteBuffer {
        require(inputTensor.shape().contentEquals(intArrayOf(1, bitmap.height, bitmap.width, 3))) {
            "Bitmap dimensions do not match the RGB input tensor"
        }
        val type = inputTensor.dataType()
        require(type == DataType.FLOAT32 || type == DataType.UINT8 || type == DataType.INT8) {
            "Unsupported input type: $type"
        }
        val params = inputTensor.quantizationParams()
        if (type != DataType.FLOAT32) require(params.scale.isFinite() && params.scale > 0f) {
            "Invalid input quantization scale"
        }
        val buffer = ByteBuffer.allocateDirect(inputTensor.numBytes()).order(ByteOrder.nativeOrder())
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val shifts = intArrayOf(16, 8, 0)
        for (pixel in pixels) {
            for (shift in shifts) {
                val normalized = ((pixel shr shift) and 0xff) / 255f
                if (type == DataType.FLOAT32) buffer.putFloat(normalized)
                else buffer.put(quantize(normalized, params.scale, params.zeroPoint, type == DataType.INT8))
            }
        }
        buffer.rewind()
        return buffer
    }

    internal fun quantize(value: Float, scale: Float, zeroPoint: Int, signed: Boolean): Byte {
        require(scale.isFinite() && scale > 0f) { "Invalid input quantization scale" }
        val quantized = (value / scale + zeroPoint).roundToInt()
        return (if (signed) quantized.coerceIn(-128, 127) else quantized.coerceIn(0, 255)).toByte()
    }
}

