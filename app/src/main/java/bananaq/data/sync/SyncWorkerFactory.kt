package bananaq.data.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import bananaq.BuildConfig

/** Resolves sync jobs persisted before the source package was renamed. */
class SyncWorkerFactory : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? = when (workerClassName) {
        "${BuildConfig.APPLICATION_ID}.data.sync.SupabaseSyncWorker",
        SupabaseSyncWorker::class.java.name -> SupabaseSyncWorker(appContext, workerParameters)
        else -> null
    }
}
