package org.example.project

import android.app.Application
import kallos.platform.ApplicationContextHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.content.ComponentCallbacks2
import org.example.project.health.SleepTrackingScheduler
import org.example.project.health.scheduleHealthSync

class KalliorApplication : Application() {
    lateinit var exitOverlay: ExitOverlayAnimator
        private set

    private lateinit var backgroundWatcher: AppBackgroundWatcher
    private var exitRecordedForForegroundSession = false

    override fun onCreate() {
        super.onCreate()
        ApplicationContextHolder.applicationContext = applicationContext
        exitOverlay = ExitOverlayAnimator(this).also { it.preload() }
        backgroundWatcher = AppBackgroundWatcher(
            application = this,
            onBackground = {
                if (shouldShowExitAnimationForCurrentExit()) exitOverlay.showIfAllowed()
            },
            onForeground = {
                exitRecordedForForegroundSession = false
                exitOverlay.cancel()
            },
        )
        // Schedule periodic background health sync.
        scheduleHealthSync(this)
        // Restore the next sleep-window boundary after process creation. The
        // foreground service itself owns the dynamic screen-state receiver.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            SleepTrackingScheduler.rescheduleFromStoredSchedule(this@KalliorApplication)
        }
    }

    /**
     * Counts only exits after a continuous 30-second foreground session. Both the
     * early Activity hint and delayed lifecycle callback call this method, so the
     * per-session flag prevents one exit from being counted twice.
     */
    fun shouldShowExitAnimationForCurrentExit(): Boolean {
        if (
            exitRecordedForForegroundSession ||
            !backgroundWatcher.hasBeenInForegroundFor(EXIT_ANIMATION_MIN_FOREGROUND_MS)
        ) {
            return false
        }

        exitRecordedForForegroundSession = true
        val preferences = getSharedPreferences(EXIT_ANIMATION_PREFERENCES, MODE_PRIVATE)
        val qualifyingExitCount = preferences.getInt(QUALIFYING_EXIT_COUNT_KEY, 0) + 1
        preferences.edit().putInt(QUALIFYING_EXIT_COUNT_KEY, qualifyingExitCount).apply()
        return qualifyingExitCount % EXITS_PER_ANIMATION == 0
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level == ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            InstalledAppsProvider.trimMemory(level)
        }
    }

    private companion object {
        const val EXIT_ANIMATION_MIN_FOREGROUND_MS = 30_000L
        const val EXITS_PER_ANIMATION = 2
        const val EXIT_ANIMATION_PREFERENCES = "exit_animation"
        const val QUALIFYING_EXIT_COUNT_KEY = "qualifying_exit_count"
    }

    override fun onLowMemory() {
        super.onLowMemory()
        onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }
}
