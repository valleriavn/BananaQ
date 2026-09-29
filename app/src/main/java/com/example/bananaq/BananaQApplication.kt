package com.example.bananaq

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.bananaq.data.AppSessionManager
import com.example.bananaq.data.BananaQDatabase
import kotlin.concurrent.thread

class BananaQApplication : Application() {
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
