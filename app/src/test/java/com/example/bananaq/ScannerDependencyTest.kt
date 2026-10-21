package com.example.bananaq

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.bananaq.data.ProcessedClassification
import com.example.bananaq.data.ScanRepository
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ScannerDependencyTest {
    @Test fun recreationReusesTheViewModelAndClearingTheOwnerClosesItsRepository() {
        val repository = FakeRepository()
        var creations = 0
        val factory = ScannerViewModelFactory { creations++; repository }
        val store = ViewModelStore()
        val owner = object : ViewModelStoreOwner { override val viewModelStore = store }
        val first = ViewModelProvider(owner, factory)[ScannerViewModel::class.java]
        val recreated = ViewModelProvider(owner, factory)[ScannerViewModel::class.java]
        assertSame(first, recreated)
        assertEquals(1, creations)
        assertEquals(1L, repository.closed.count)
        store.clear()
        assertTrue("Interpreter cleanup was not queued", repository.closed.await(2, TimeUnit.SECONDS))
    }

    @Test fun separateScannerOwnersReceiveSeparateRepositories() {
        val repositories = mutableListOf<FakeRepository>()
        val factory = ScannerViewModelFactory { FakeRepository().also(repositories::add) }
        val stores = List(2) { ViewModelStore() }
        try {
            for (store in stores) {
                val owner = object : ViewModelStoreOwner { override val viewModelStore = store }
                ViewModelProvider(owner, factory)[ScannerViewModel::class.java]
            }
            assertEquals(2, repositories.size)
            assertNotSame(repositories[0], repositories[1])
        } finally {
            stores.forEach { it.clear() }
        }
        repositories.forEach { assertTrue(it.closed.await(2, TimeUnit.SECONDS)) }
    }

    private class FakeRepository : ScanRepository {
        val closed = CountDownLatch(1)
        override fun load(uri: Uri): Bitmap = error("No image IO is needed for lifecycle tests")
        override fun classifyAndStore(bitmap: Bitmap, scanId: String): ProcessedClassification =
            error("No inference or database is needed for lifecycle tests")
        override fun close() { closed.countDown() }
    }
}
