package org.example.project

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlin.math.max

/** Reports transitions between an app with visible activities and the background. */
class AppBackgroundWatcher(
    application: Application,
    private val onBackground: () -> Unit,
    private val onForeground: () -> Unit,
) : Application.ActivityLifecycleCallbacks {
    private val handler = Handler(Looper.getMainLooper())
    private var startedActivities = 0
    private var foregroundStartedAt: Long? = null

    private val backgroundRunnable = Runnable {
        if (startedActivities == 0) onBackground()
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        if (startedActivities == 0) {
            foregroundStartedAt = SystemClock.elapsedRealtime()
            onForeground()
        }
        startedActivities++
        handler.removeCallbacks(backgroundRunnable)
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities = max(0, startedActivities - 1)
        if (
            startedActivities == 0 &&
            !activity.isChangingConfigurations &&
            !activity.isFinishing
        ) {
            handler.postDelayed(backgroundRunnable, BACKGROUND_DELAY_MS)
        }
    }

    fun hasBeenInForegroundFor(minimumDurationMs: Long): Boolean =
        foregroundStartedAt?.let { startedAt ->
            SystemClock.elapsedRealtime() - startedAt >= minimumDurationMs
        } ?: false

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    private companion object {
        const val BACKGROUND_DELAY_MS = 150L
    }
}
