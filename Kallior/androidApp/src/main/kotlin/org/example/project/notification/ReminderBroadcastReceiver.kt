package org.example.project.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Reminder"
        val description = intent.getStringExtra("EXTRA_DESC")
        val frequencyMinutes = intent.getLongExtra("EXTRA_FREQUENCY", 0L)
        val reminderId = intent.getStringExtra("EXTRA_REMINDER_ID") ?: ""

        // 1. Show the notification
        NotificationHelper.showNotification(context, title, description, reminderId.hashCode())

        // 2. Reschedule for the next interval if it's a repeating reminder
        if (frequencyMinutes > 0) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val nextTriggerTime = System.currentTimeMillis() + (frequencyMinutes * 60 * 1000L)

            // Create a new intent with the same extras to avoid reusing the received broadcast intent
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
    }
}
