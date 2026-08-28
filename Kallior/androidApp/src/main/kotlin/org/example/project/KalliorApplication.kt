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
    override fun onCreate() {
        super.onCreate()
        ApplicationContextHolder.applicationContext = applicationContext
        // Schedule periodic background health sync.
        scheduleHealthSync(this)
        // Restore the next sleep-window boundary after process creation. The
        // foreground service itself owns the dynamic screen-state receiver.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            SleepTrackingScheduler.rescheduleFromStoredSchedule(this@KalliorApplication)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level == ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            InstalledAppsProvider.trimMemory(level)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }
}
