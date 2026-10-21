package bananaq

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import bananaq.data.ProcessedClassification
import bananaq.data.ScanRepository
import java.util.concurrent.Executors

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Processing(val uri: String, val scanId: String) : ScanUiState
    data class Success(val uri: String, val scanId: String, val scan: ProcessedClassification) : ScanUiState
    data class Failure(val cause: Throwable) : ScanUiState
}

/** Retains one inference job across rotation; no Activity or View is retained. */
class ScannerViewModel(private val processor: ScanRepository) : ViewModel() {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val mutableState = MutableLiveData<ScanUiState>(ScanUiState.Idle)
    val state: LiveData<ScanUiState> = mutableState
    private var generation = 0
    private var cleared = false

    fun scan(uri: Uri, scanId: String) {
        if (mutableState.value is ScanUiState.Processing || cleared) return
        val request = ++generation
        mutableState.value = ScanUiState.Processing(uri.toString(), scanId)
        executor.execute {
            val outcome = try {
                val bitmap = processor.load(uri)
                try {
                    ScanUiState.Success(uri.toString(), scanId, processor.classifyAndStore(bitmap, scanId))
                } finally {
                    bitmap.recycle()
                }
            } catch (error: Exception) {
                ScanUiState.Failure(error)
            } catch (error: LinkageError) {
                ScanUiState.Failure(error)
            }
            main.post {
                if (!cleared && request == generation) mutableState.value = outcome
            }
        }
    }

    fun reset() {
        generation++
        mutableState.value = ScanUiState.Idle
    }

    override fun onCleared() {
        cleared = true
        generation++
        main.removeCallbacksAndMessages(null)
        // Queue cleanup after any running inference; Interpreter is not thread-safe.
        executor.execute { processor.close() }
        executor.shutdown()
    }
}

/** Constructor injection without an Activity reference or a global interpreter singleton. */
class ScannerViewModelFactory(private val createRepository: () -> ScanRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == ScannerViewModel::class.java) { "Unsupported ViewModel: $modelClass" }
        return ScannerViewModel(createRepository()) as T
    }
}
