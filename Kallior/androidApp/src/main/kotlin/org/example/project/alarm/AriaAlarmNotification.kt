package org.example.project.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object AriaAlarmNotification {

    const val NOTIFICATION_ID = 4102
    private const val CHANNEL_ID = "aria_alarm_channel"
    private const val CHANNEL_NAME = "AriaAlarm"

    private const val REQUEST_FULL_SCREEN = 4201
    private const val REQUEST_CONTINUE = 4202
    private const val REQUEST_DISMISS = 4203

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AriaAlarm ringing notifications"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(true)
            }

            manager.createNotificationChannel(channel)
        }
    }

    fun build(context: Context, state: AriaAlarmPlayback.UiState): Notification {
        ensureChannel(context)

        val fullScreenIntent = Intent(context, AriaAlarmRingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_FULL_SCREEN,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val continueIntent = Intent(context, AriaAlarmService::class.java)
            .setAction(AriaAlarmService.ACTION_CONTINUE)

        val continuePendingIntent = PendingIntent.getService(
            context,
            REQUEST_CONTINUE,
            continueIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AriaAlarmService::class.java)
            .setAction(AriaAlarmService.ACTION_DISMISS)

        val dismissPendingIntent = PendingIntent.getService(
            context,
            REQUEST_DISMISS,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = when (state.phase) {
            AriaAlarmPlayback.Phase.IDLE -> "Alarm"
            AriaAlarmPlayback.Phase.PREVIEW -> "Previewing ${state.songName ?: "alarm sound"}"
            AriaAlarmPlayback.Phase.PAUSED_AFTER_PREVIEW -> "Preview complete. Continue to play the rest."
            AriaAlarmPlayback.Phase.FULL_PLAYBACK -> "Playing ${state.songName ?: "alarm sound"}"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("AriaAlarm")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, "Continue", continuePendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()
    }
}
