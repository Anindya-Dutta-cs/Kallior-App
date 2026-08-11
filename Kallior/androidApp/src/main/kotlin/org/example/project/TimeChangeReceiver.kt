package org.example.project

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * BroadcastReceiver that listens for time, timezone, and date changes to trigger
 * a refresh in the app, ensuring the "new day" reset logic fires promptly.
 */
class TimeChangeReceiver(private val onTimeChanged: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        onTimeChanged()
    }

    companion object {
        fun register(context: Context, onTimeChanged: () -> Unit): TimeChangeReceiver {
            val receiver = TimeChangeReceiver(onTimeChanged)
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_DATE_CHANGED)
            }
            context.registerReceiver(receiver, filter)
            return receiver
        }
    }
}
