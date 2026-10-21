package com.example.bananaq.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class TFLiteModel(
    context: Context
) {

    val interpreter: Interpreter

    init {

        interpreter = Interpreter(
            loadModel(context)
        )
    }

    private fun loadModel(
        context: Context
    ): ByteBuffer {

        return context.assets.openFd(MODEL_ASSET).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { input ->
                input.channel.map(FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset, descriptor.declaredLength)
            }
        }
    }

    fun close() {
        interpreter.close()
    }

    companion object {
        const val MODEL_ASSET = "diseases/BananaQ_MobileNetV2_Final.tflite"
    }
}
