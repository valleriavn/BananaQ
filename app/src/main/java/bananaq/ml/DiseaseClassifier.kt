package bananaq.ml

import android.graphics.Bitmap
import bananaq.model.ClassificationResult
import bananaq.model.ConfidenceLevel
import org.tensorflow.lite.DataType
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DiseaseClassifier(private val model: TFLiteModel) {
    // Output order is part of the model contract. Keep this in sync with the
    // export's class_indices; filenames or confidence scores cannot establish it.
    private val labels = arrayOf("Black Sigatoka", "Cordana Leaf Spot", "Healthy", "Panama Disease")

    fun classify(bitmap: Bitmap): ClassificationResult {
        val interpreter = model.interpreter
        val input = interpreter.getInputTensor(0)
        val shape = input.shape()
        require(shape.size == 4 && shape[0] == 1 && shape[3] == 3 && shape[1] > 0 && shape[2] > 0) {
            "Expected a single RGB image tensor"
        }
        // Use filtered (bilinear) resizing to match the training image pipeline.
        val resized = Bitmap.createScaledBitmap(bitmap, shape[2], shape[1], true)
        try {
            val buffer = ImagePreprocessor.createInputBuffer(resized, input)
            val output = interpreter.getOutputTensor(0)
            require(output.numElements() == labels.size) { "Model class count does not match labels" }
            val type = output.dataType()
            require(type == DataType.FLOAT32 || type == DataType.UINT8 || type == DataType.INT8) {
                "Unsupported output type: $type"
            }
            val resultBuffer = ByteBuffer.allocateDirect(output.numBytes()).order(ByteOrder.nativeOrder())
            interpreter.run(buffer, resultBuffer)
            resultBuffer.rewind()
            val params = output.quantizationParams()
            if (type != DataType.FLOAT32) require(params.scale.isFinite() && params.scale > 0f) {
                "Invalid output quantization scale"
            }
            val probabilities = FloatArray(labels.size) {
                if (type == DataType.FLOAT32) resultBuffer.float else {
                    val value = resultBuffer.get().toInt()
                    val quantized = if (type == DataType.UINT8) value and 0xff else value
                    (quantized - params.zeroPoint) * params.scale
                }
            }
            val index = PredictionScores.winner(probabilities)
            val confidence = probabilities[index]
            val level = ConfidenceLevel.fromConfidence(confidence)
            return ClassificationResult(labels[index], confidence, level,
                PredictionScores.isAccepted(probabilities))
        } finally {
            if (resized !== bitmap) resized.recycle()
        }
    }
}

