package org.example.project.health

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Keeps screen-state tracking alive only while an opt-in sleep window is active.
 *
 * It is foreground because a dynamically registered screen receiver belongs to
 * the app process and would otherwise stop receiving broadcasts if Android
 * reclaims that process overnight. START_STICKY restores the tracker after an
 * exceptional process kill; UsageStats remains a persisted recovery path.
 */
class SleepTrackingForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var receiver: ScreenStateReceiver? = null
    private var tickJob: Job? = null
    private var isInitializing = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification())
        startTrackingIfInsideSleepWindow()
        return START_STICKY
    }

    override fun onDestroy() {
        tickJob?.cancel()
        receiver?.let { unregisterReceiver(it) }
        receiver = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTrackingIfInsideSleepWindow() {
        if (receiver != null || isInitializing) return
        isInitializing = true
        scope.launch {
            try {
                val schedule = HealthDependencies.sleepScheduleStore(this@SleepTrackingForegroundService)
                    .currentSchedule()
                if (schedule == null) {
                    stopSelf()
                    return@launch
                }

                val tracker = HealthDependencies.awakeTimeTracker(this@SleepTrackingForegroundService)
                val calculator = HealthDependencies.sleepWindowCalculator()
                val now = Instant.now()
                val window = calculator.currentOrMostRecentSleepWindow(now, schedule)
                if (!window.contains(now)) {
                    stopSelf()
                    return@launch
                }

                receiver = ScreenStateReceiver.register(this@SleepTrackingForegroundService)
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (powerManager.isInteractive) {
                    tracker.onPhoneUsageStarted(now, schedule)
                }

                tickJob = scope.launch {
                    while (isActive) {
                        delay(TICK_MILLIS)
                        val latestSchedule = HealthDependencies
                            .sleepScheduleStore(this@SleepTrackingForegroundService)
                            .currentSchedule()
                        if (latestSchedule == null) {
                            stopSelf()
                            return@launch
                        }
                        tracker.onTick(Instant.now(), latestSchedule)
                        val latestWindow = calculator.currentOrMostRecentSleepWindow(
                            Instant.now(),
                            latestSchedule,
                        )
                        if (!latestWindow.contains(Instant.now())) {
                            stopSelf()
                            return@launch
                        }
                    }
                }
            } finally {
                isInitializing = false
            }
        }
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Sleep tracking",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Kallior sleep tracking")
            .setContentText("Tracking screen-on time during your sleep schedule")
            .setOngoing(true)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "sleep_tracking"
        const val NOTIFICATION_ID = 4_200
        const val TICK_MILLIS = 60_000L
    }
}
