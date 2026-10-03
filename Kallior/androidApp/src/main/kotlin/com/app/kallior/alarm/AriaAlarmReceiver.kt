package com.app.kallior.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AriaAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L

        val store = AriaAlarmStore(context)
        val alarm = if (alarmId >= 0L) store.find(alarmId) else null

        if (alarm == null) {
            // Stale PendingIntent: the legacy single alarm (no id) or a deleted alarm.
            if (alarmId < 0L) {
                AriaAlarmScheduler.cancelLegacy(context)
            }
            return
        }

        if (!alarm.enabled) return

        val serviceIntent = Intent(context, AriaAlarmService::class.java)
            .setAction(AriaAlarmService.ACTION_START_RINGING)
            .putExtra(AriaAlarmService.EXTRA_ALARM_ID, alarm.id)

        ContextCompat.startForegroundService(context, serviceIntent)

        // Daily recurrence: re-arm this alarm's next occurrence.
        AriaAlarmScheduler.schedule(context, alarm)
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}
