package org.example.project.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AriaAlarmScheduler {

    private const val REQUEST_CODE_ALARM = 4101
    private const val REQUEST_CODE_SHOW = 4102

    fun schedule(context: Context): Boolean {
        val prefs = AriaAlarmPreferences(context)
        return schedule(context, prefs.hour, prefs.minute)
    }

    fun schedule(context: Context, hour: Int, minute: Int): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return false
        }

        val triggerTime = nextTriggerTime(hour, minute)

        val alarmIntent = Intent(context, AriaAlarmReceiver::class.java)
        val alarmPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, AriaAlarmRingActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_SHOW,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)

        alarmManager.setAlarmClock(alarmClockInfo, alarmPendingIntent)

        return true
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val alarmIntent = Intent(context, AriaAlarmReceiver::class.java)
        val alarmPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(alarmPendingIntent)
    }

    fun nextTriggerTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!calendar.after(now)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }
}
