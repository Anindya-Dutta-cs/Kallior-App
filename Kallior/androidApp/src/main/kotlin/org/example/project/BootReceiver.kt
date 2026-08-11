package org.example.project

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.example.project.health.AccelerometerStepForegroundService
import org.example.project.health.HealthDependencies
import org.example.project.health.SleepTrackingScheduler
import org.example.project.health.scheduleHealthSync

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val blockerRepository = BlockerRepository(context)
        val websiteRepository = WebsiteBlockerRepository(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // App blocker and VPN are independent: one can be on while the
                // other is off, so we restore each from its own persisted flag.
                if (blockerRepository.isBlockingEnabledFlow.first()) {
                    context.startForegroundService(
                        Intent(context, AppBlockerForegroundService::class.java),
                    )
                }
                if (websiteRepository.isVpnEnabledFlow.first()) {
                    // prepare() returns null when the VPN permission is already granted.
                    if (android.net.VpnService.prepare(context) == null) {
                        context.startForegroundService(
                            Intent(context, WebsiteBlockerVpnService::class.java),
                        )
                    }
                }
                SleepTrackingScheduler.rescheduleFromStoredSchedule(context)
                AccelerometerStepForegroundService.startIfNeeded(context)
            } catch (_: Exception) {
                // Never let a boot-time failure crash the receiver.
            } finally {
                pendingResult.finish()
            }
        }

        // Schedule periodic health sync — wakes up every ~15 min to refresh
        // step/sleep data and backfill UsageStats intervals.
        scheduleHealthSync(context)

        // Backfill awake intervals for the sleep window that may have been
        // missed while the device was off or the process was dead.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching {
                val schedule = HealthDependencies.sleepScheduleStore(context).currentSchedule()
                if (schedule != null) {
                    HealthDependencies.usageStatsBackfiller(context)
                        .backfillForCurrentWindow(schedule)
                }
            }
        }
    }
}
