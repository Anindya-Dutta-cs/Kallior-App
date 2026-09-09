package org.example.project.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Scheduler that works with [AlarmItem]s, each getting its own [PendingIntent]
 * keyed by [AlarmItem.requestCode]. The legacy [AriaAlarmScheduler] is left
 * intact so existing callers (receiver, boot) keep compiling.
 */
object AriaMultiAlarmScheduler {

    fun schedule(context: Context, alarm: AlarmItem): Boolean {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            return false
        }

        val triggerTime = AriaAlarmScheduler.nextTriggerTime(alarm.hour, alarm.minute)

        val alarmIntent = Intent(context, AriaAlarmReceiver::class.java)
        val alarmPI = PendingIntent.getBroadcast(
            context,
            alarm.requestCode,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, AriaAlarmRingActivity::class.java)
        val showPI = PendingIntent.getActivity(
            context,
            alarm.requestCode + 1,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerTime, showPI),
            alarmPI
        )
        return true
    }

    fun cancel(context: Context, alarm: AlarmItem) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val alarmIntent = Intent(context, AriaAlarmReceiver::class.java)
        val alarmPI = PendingIntent.getBroadcast(
            context,
            alarm.requestCode,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(alarmPI)
    }

    /** Schedule every enabled alarm. Useful after boot or package update. */
    fun rescheduleAll(context: Context) {
        val store = AriaAlarmStore(context)
        store.loadAll().filter { it.enabled }.forEach { schedule(context, it) }
    }
}
