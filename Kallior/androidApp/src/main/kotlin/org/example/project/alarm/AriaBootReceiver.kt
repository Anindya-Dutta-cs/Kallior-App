package org.example.project.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AriaBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        val relevantActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )

        if (action !in relevantActions) return

        val prefs = AriaAlarmPreferences(context)
        if (prefs.enabled) {
            AriaAlarmScheduler.schedule(context)
        }
    }
}
