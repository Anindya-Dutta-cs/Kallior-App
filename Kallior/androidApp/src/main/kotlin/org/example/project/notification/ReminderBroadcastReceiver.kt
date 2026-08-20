package org.example.project.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.example.project.health.HealthDependencies

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Reminder"
        val description = intent.getStringExtra("EXTRA_DESC")
        val frequencyMinutes = intent.getLongExtra("EXTRA_FREQUENCY", 0L)
        val reminderId = intent.getStringExtra("EXTRA_REMINDER_ID") ?: ""

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val isSleepTrackingActive = HealthDependencies.sleepScheduleStore(context).currentSchedule() != null
                if (!isSleepTrackingActive) {
                    // 1. Show the notification only if sleep tracking is NOT active
                    NotificationHelper.showNotification(context, title, description, reminderId.hashCode())
                }

                // 2. Reschedule for the next interval if it's a repeating reminder
                if (frequencyMinutes > 0) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val nextTriggerTime = System.currentTimeMillis() + (frequencyMinutes * 60 * 1000L)

                    val newIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                        putExtra("EXTRA_TITLE", title)
                        putExtra("EXTRA_DESC", description)
                        putExtra("EXTRA_FREQUENCY", frequencyMinutes)
                        putExtra("EXTRA_REMINDER_ID", reminderId)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        reminderId.hashCode(),
                        newIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
