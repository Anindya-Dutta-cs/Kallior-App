package org.example.project.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules [AlarmItem]s. Every alarm gets its own PendingIntent keyed by
 * [AlarmItem.requestCode] and carrying the alarm id, so alarms can be
 * scheduled, cancelled and edited independently.
 */
object AriaAlarmScheduler {

    private const val REQUEST_CODE_SHOW = 4102
    private const val LEGACY_REQUEST_CODE_ALARM = 4101

    /**
     * Schedules (or replaces) the schedule for [alarm]. Daily recurrence.
     * Returns false when exact alarms are not permitted for this app.
     */
    fun schedule(context: Context, alarm: AlarmItem): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return false
        }

        val triggerTime = AriaAlarmTime.nextTriggerAt(alarm.hour, alarm.minute, alarm.repeatDays)

        val alarmIntent = Intent(context, AriaAlarmReceiver::class.java)
            .putExtra(AriaAlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        val alarmPendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.requestCode,
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

    fun cancel(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val alarmIntent = Intent(context, AriaAlarmReceiver::class.java)
        val alarmPendingIntent = PendingIntent.getBroadcast(
            context,
            (AlarmItem.REQUEST_CODE_BASE + alarmId).toInt(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(alarmPendingIntent)
    }

    /** Schedules every enabled alarm. Used after boot, package update or time changes. */
    fun rescheduleAll(context: Context) {
        AriaAlarmStore(context)
            .loadAll()
            .filter { it.enabled }
            .forEach { schedule(context, it) }
    }

    /** Cancels the pre-migration single-alarm PendingIntent so it cannot fire as a ghost. */
    fun cancelLegacy(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val alarmIntent = Intent(context, AriaAlarmReceiver::class.java)
        val legacyPendingIntent = PendingIntent.getBroadcast(
            context,
            LEGACY_REQUEST_CODE_ALARM,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(legacyPendingIntent)
    }
}
