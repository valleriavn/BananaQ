package com.example.bananaq.data.sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class SupabaseSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : Worker(appContext, workerParameters) {
    override fun doWork(): Result = if (SupabaseSyncManager.syncPending(applicationContext)) {
        Result.success()
    } else {
        Result.retry()
    }
}
