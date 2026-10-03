package com.app.kallior.health

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.app.kallior.notification.NotificationHelper

/** Receives scheduled sleep-window boundaries while the app process is absent. */
class SleepTrackingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val schedule = HealthDependencies.sleepScheduleStore(context).currentSchedule()
                    ?: return@launch
                when (intent.action) {
                    ACTION_BEDTIME_REMINDER -> {
                        NotificationHelper.showNotification(
                            context = context,
                            title = "Bedtime Reminder",
                            description = "Your sleep hours start in 5 minutes. Get ready for bedtime.",
                            notificationId = BEDTIME_NOTIFICATION_ID,
                        )
                    }
                    ACTION_START -> {
                        SleepTrackingScheduler.startService(context.applicationContext)
                        SleepTrackingScheduler.schedule(context, schedule)
                    }
                    ACTION_STOP -> SleepTrackingScheduler.stopAndScheduleNext(context, schedule)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_START = "com.app.kallior.health.action.START_SLEEP_TRACKING"
        const val ACTION_STOP = "com.app.kallior.health.action.STOP_SLEEP_TRACKING"
        const val ACTION_BEDTIME_REMINDER = "com.app.kallior.health.action.BEDTIME_REMINDER"
        const val BEDTIME_NOTIFICATION_ID = 4_205
    }
}
