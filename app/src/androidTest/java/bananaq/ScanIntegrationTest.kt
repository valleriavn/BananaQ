package bananaq

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import bananaq.data.DiseaseRepository
import bananaq.ml.DiseaseClassifier
import bananaq.ml.TFLiteModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanIntegrationTest {
    @Test fun bundledDiseaseFilesResolveForEveryModelLabel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = DiseaseRepository(context)
        for (label in listOf("Black Sigatoka", "Cordana Leaf Spot", "Healthy", "Panama Disease")) {
            val info = repository.getDiseaseInfo(label)
            assertNotNull(label, info)
            assertEquals(label, info!!.diseaseName)
            assertTrue(info.symptoms.isNotEmpty())
            assertTrue(info.treatment.isNotEmpty())
            assertTrue(info.prevention.isNotEmpty())
        }
    }

    @Test fun packagedModelLoadsAndRunsFourClassInference() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val model = TFLiteModel(context)
        val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.GREEN)
        try {
            assertArrayEquals(intArrayOf(1, 224, 224, 3), model.interpreter.getInputTensor(0).shape())
            assertTrue(model.interpreter.getInputTensor(0).name().contains("rgb_0_to_1"))
            assertEquals(4, model.interpreter.getOutputTensor(0).numElements())
            val result = DiseaseClassifier(model).classify(bitmap)
            assertTrue(result.confidence.isFinite())
            assertTrue(result.confidence in 0f..1f)
        } finally {
            bitmap.recycle()
            model.close()
        }
    }
}
