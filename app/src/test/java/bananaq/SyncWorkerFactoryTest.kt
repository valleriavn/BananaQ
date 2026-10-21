package bananaq

import android.app.Application
import androidx.work.Data
import androidx.work.ForegroundUpdater
import androidx.work.ProgressUpdater
import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import bananaq.data.sync.SupabaseSyncWorker
import bananaq.data.sync.SyncWorkerFactory
import java.util.UUID
import java.util.concurrent.Executor
import kotlin.coroutines.EmptyCoroutineContext
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SyncWorkerFactoryTest {
    private val factory = SyncWorkerFactory()
    private val executor = Executor { it.run() }
    private val parameters = WorkerParameters(
        UUID.randomUUID(), Data.Builder().putString("record", "retained").build(),
        listOf("sync"), WorkerParameters.RuntimeExtras(), 2, 0,
        executor, EmptyCoroutineContext, WorkManagerTaskExecutor(executor), factory,
        ProgressUpdater { _, _, _ -> error("Progress updates are not used during construction") },
        ForegroundUpdater { _, _, _ -> error("Foreground updates are not used during construction") }
    )

    @Test fun persistedAndRenamedWorkersRetainTheirJobParameters() {
        for (name in listOf(
            "${BuildConfig.APPLICATION_ID}.data.sync.SupabaseSyncWorker",
            SupabaseSyncWorker::class.java.name
        )) {
            val worker = factory.createWorker(RuntimeEnvironment.getApplication(), name, parameters)
            assertTrue(worker is SupabaseSyncWorker)
            requireNotNull(worker)
            assertEquals(parameters.id, worker.id)
            assertEquals("retained", worker.inputData.getString("record"))
            assertEquals(2, worker.runAttemptCount)
        }
    }

    @Test fun unrelatedWorkersUseWorkManagersDefaultFactory() {
        assertNull(factory.createWorker(RuntimeEnvironment.getApplication(), "other.Worker", parameters))
    }

    @Test fun applicationProvidesTheMigrationFactory() {
        assertTrue(BananaQApplication().workManagerConfiguration.workerFactory is SyncWorkerFactory)
    }
}
