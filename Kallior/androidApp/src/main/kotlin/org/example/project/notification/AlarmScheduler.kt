package org.example.project.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kallos.model.Remainder

class AlarmScheduler(private val context: Context) {

    fun schedule(reminder: Remainder) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_TITLE", reminder.title)
            putExtra("EXTRA_DESC", reminder.description)
            putExtra("EXTRA_FREQUENCY", reminder.frequencyMinutes)
        }

        val requestCode = reminder.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var timeMillis = reminder.time.toEpochMilliseconds()

        // FIX: If the initial time is in the past, calculate the next valid interval
        if (timeMillis <= System.currentTimeMillis()) {
            if (reminder.frequencyMinutes > 0) {
                val intervalMs = reminder.frequencyMinutes * 60 * 1000L
                while (timeMillis <= System.currentTimeMillis()) {
                    timeMillis += intervalMs
                }
            } else {
                // If it's a one-time reminder and in the past, fire it in 1 second so the user sees it
                timeMillis = System.currentTimeMillis() + 1000L
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        }
    }

    fun cancel(reminderId: String) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val requestCode = reminderId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
