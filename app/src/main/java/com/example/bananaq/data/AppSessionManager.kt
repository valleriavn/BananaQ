package com.example.bananaq.data

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.bananaq.data.sync.SupabaseSyncManager

class AppSessionManager(
    private val application: Application
) : Application.ActivityLifecycleCallbacks {

    private val database = BananaQDatabase.get(application)
    private var startedActivities = 0
    private var sessionId: String? = null

    fun currentSessionId(): String = synchronized(this) {
        sessionId ?: database.startSession().also {
            sessionId = it
            SupabaseSyncManager.request(application)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (startedActivities == 0) currentSessionId()
        startedActivities++
    }

    override fun onActivityResumed(activity: Activity) {
        database.touchSession(currentSessionId())
        SupabaseSyncManager.request(application)
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0 && !activity.isChangingConfigurations) {
            sessionId?.let(database::endSession)
            sessionId = null
            SupabaseSyncManager.request(application)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
