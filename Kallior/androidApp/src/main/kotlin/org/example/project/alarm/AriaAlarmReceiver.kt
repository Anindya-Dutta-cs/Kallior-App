package org.example.project.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AriaAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val serviceIntent = Intent(context, AriaAlarmService::class.java)
            .setAction(AriaAlarmService.ACTION_START_RINGING)

        ContextCompat.startForegroundService(context, serviceIntent)

        val prefs = AriaAlarmPreferences(context)
        if (prefs.enabled) {
            AriaAlarmScheduler.schedule(context)
        }
    }
}
