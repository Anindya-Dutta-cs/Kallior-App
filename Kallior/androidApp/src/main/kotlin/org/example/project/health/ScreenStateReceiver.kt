package org.example.project.health

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Listens for screen on/off/unlock events and forwards them to the
 * [AwakeTimeTracker] so phone usage during sleep windows is captured.
 *
 * Tracking follows the device's interactive screen state: screen-on starts an
 * awake interval and screen-off closes it. The receiver is registered by the
 * foreground sleep service, so it remains active for the full sleep window.
 */
class ScreenStateReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val tracker = HealthDependencies.awakeTimeTracker(context)
        val scheduleStore = HealthDependencies.sleepScheduleStore(context)
        val now = Instant.now()

        val pendingResult = goAsync()
        scope.launch {
            try {
                val schedule = scheduleStore.currentSchedule()
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> tracker.onPhoneUsageStarted(now, schedule)
                    Intent.ACTION_SCREEN_OFF -> tracker.onPhoneUsageStopped(now, schedule)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        /** Register this receiver dynamically (required for SCREEN_ON/OFF). */
        fun register(context: Context): ScreenStateReceiver {
            val receiver = ScreenStateReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            context.registerReceiver(receiver, filter)
            return receiver
        }
    }
}
