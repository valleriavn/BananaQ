package bananaq

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.Configuration
import bananaq.data.AppSessionManager
import bananaq.data.BananaQDatabase
import bananaq.data.sync.SyncWorkerFactory
import kotlin.concurrent.thread

class BananaQApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(SyncWorkerFactory()).build()

    val scannerViewModelFactory by lazy {
        ScannerViewModelFactory { bananaq.data.ScanProcessor(this) }
    }
    lateinit var sessionManager: AppSessionManager
        private set

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        sessionManager = AppSessionManager(this)
        registerActivityLifecycleCallbacks(sessionManager)
        thread(name = "bananaq-file-maintenance", isDaemon = true) {
            BananaQDatabase.get(this).cleanupOrphanedPhotos()
        }
    }

    fun currentSessionId(): String = sessionManager.currentSessionId()
}
