package org.example.project.health

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant

/**
 * Schedules the boundaries of the opt-in sleep tracker. Exact alarms are used
 * when the user has granted exact-alarm access; an allow-while-idle alarm plus
 * UsageStats backfill remains as the fallback when exact alarms are unavailable.
 */
object SleepTrackingScheduler {
    private const val START_REQUEST_CODE = 4_201
    private const val STOP_REQUEST_CODE = 4_202
    private const val BEDTIME_REMINDER_REQUEST_CODE = 4_203

    suspend fun rescheduleFromStoredSchedule(context: Context) {
        val schedule = HealthDependencies.sleepScheduleStore(context).currentSchedule()
        if (schedule == null) {
            cancel(context)
        } else {
            schedule(context, schedule)
        }
    }

    fun schedule(context: Context, schedule: SleepSchedule, now: Instant = Instant.now()) {
        val appContext = context.applicationContext
        val calculator = HealthDependencies.sleepWindowCalculator()
        val window = calculator.currentOrNextSleepWindow(now, schedule)

        if (window.contains(now)) {
            startService(appContext)
            scheduleAlarm(appContext, SleepTrackingAlarmReceiver.ACTION_STOP, window.end)
            val next = calculator.currentOrNextSleepWindow(window.end.plusMillis(1), schedule)
            scheduleAlarm(appContext, SleepTrackingAlarmReceiver.ACTION_START, next.start)
            val nextBedtime = next.start.minus(java.time.Duration.ofMinutes(5))
            if (nextBedtime.isAfter(now)) {
                scheduleAlarm(appContext, SleepTrackingAlarmReceiver.ACTION_BEDTIME_REMINDER, nextBedtime)
            }
        } else {
            scheduleAlarm(appContext, SleepTrackingAlarmReceiver.ACTION_START, window.start)
            val bedtime = window.start.minus(java.time.Duration.ofMinutes(5))
            if (bedtime.isAfter(now)) {
                scheduleAlarm(appContext, SleepTrackingAlarmReceiver.ACTION_BEDTIME_REMINDER, bedtime)
            }
        }
    }

    fun stopAndScheduleNext(context: Context, schedule: SleepSchedule) {
        context.applicationContext.stopService(Intent(context, SleepTrackingForegroundService::class.java))
        schedule(context, schedule)
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        manager.cancel(pendingIntent(context, SleepTrackingAlarmReceiver.ACTION_START))
        manager.cancel(pendingIntent(context, SleepTrackingAlarmReceiver.ACTION_STOP))
        manager.cancel(pendingIntent(context, SleepTrackingAlarmReceiver.ACTION_BEDTIME_REMINDER))
        context.stopService(Intent(context, SleepTrackingForegroundService::class.java))
    }

    fun startService(context: Context) {
        try {
            context.startForegroundService(
                Intent(context, SleepTrackingForegroundService::class.java),
            )
        } catch (_: SecurityException) {
            // If the platform refuses a background FGS launch, UsageStats
            // backfill still recovers the interval on the next scheduled run.
        } catch (_: IllegalStateException) {
            // Android can reject starts from a background-restricted process.
        }
    }

    private fun scheduleAlarm(context: Context, action: String, at: Instant) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAtMillis = at.toEpochMilli()
        val pendingIntent = pendingIntent(context, action)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun pendingIntent(context: Context, action: String): PendingIntent {
        val requestCode = when (action) {
            SleepTrackingAlarmReceiver.ACTION_START -> START_REQUEST_CODE
            SleepTrackingAlarmReceiver.ACTION_STOP -> STOP_REQUEST_CODE
            SleepTrackingAlarmReceiver.ACTION_BEDTIME_REMINDER -> BEDTIME_REMINDER_REQUEST_CODE
            else -> START_REQUEST_CODE
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, SleepTrackingAlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
