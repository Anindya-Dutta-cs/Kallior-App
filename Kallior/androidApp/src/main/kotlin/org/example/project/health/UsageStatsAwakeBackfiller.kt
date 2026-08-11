package org.example.project.health

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.Instant

/**
 * Recovers phone-usage intervals that were missed because the app process
 * was killed during a sleep window. Uses [UsageStatsManager.queryEvents] to
 * reconstruct foreground sessions and inserts them as
 * `"USAGE_STATS_BACKFILL"` intervals into [AwakeIntervalRepository].
 *
 * The existing [AwakeIntervalRepository.mergeAndSum] de-duplicates overlapping
 * `SCREEN_EVENTS` and `USAGE_STATS_BACKFILL` intervals automatically, so
 * calling this is always safe — it never double-counts.
 */
class UsageStatsAwakeBackfiller(
    private val context: Context,
    private val awakeIntervalRepository: AwakeIntervalRepository,
    private val sleepWindowCalculator: SleepWindowCalculator,
) {
    /**
     * Backfill awake intervals for the given [window] from UsageStats.
     * No-op if the PACKAGE_USAGE_STATS permission has not been granted.
     */
    suspend fun backfillForWindow(window: SleepWindow) {
        if (!hasUsageStatsPermission()) return

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return

        val events = try {
            usageStatsManager.queryEvents(
                window.start.toEpochMilli(),
                window.end.toEpochMilli(),
            )
        } catch (_: Exception) {
            return
        }

        val event = UsageEvents.Event()
        var foregroundStart: Long? = null
        val intervals = mutableListOf<Pair<Long, Long>>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (foregroundStart == null) {
                        foregroundStart = event.timeStamp
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    foregroundStart?.let { start ->
                        intervals.add(start to event.timeStamp)
                        foregroundStart = null
                    }
                }
            }
        }

        // Close any session that was still open, clipping to now (not the
        // future window end) so we never record awake time that hasn't happened.
        foregroundStart?.let { start ->
            val closeMs = minOf(System.currentTimeMillis(), window.end.toEpochMilli())
            if (closeMs > start) {
                intervals.add(start to closeMs)
            }
        }

        for ((startMs, endMs) in intervals) {
            val start = Instant.ofEpochMilli(startMs)
            val end = Instant.ofEpochMilli(endMs)

            val clippedStart = maxOf(start, window.start)
            val clippedEnd = minOf(end, window.end)
            if (!clippedEnd.isAfter(clippedStart)) continue

            val durationSeconds = (clippedEnd.toEpochMilli() - clippedStart.toEpochMilli()) / 1000

            awakeIntervalRepository.insert(
                AwakeIntervalInput(
                    anchorDate = window.anchorDate.toString(),
                    startTime = clippedStart.toEpochMilli(),
                    endTime = clippedEnd.toEpochMilli(),
                    durationSeconds = durationSeconds,
                    source = "USAGE_STATS_BACKFILL",
                )
            )
        }
    }

    /**
     * Convenience: backfill for the current or most-recent sleep window
     * derived from the user's [schedule].
     */
    suspend fun backfillForCurrentWindow(schedule: SleepSchedule) {
        val window = sleepWindowCalculator.currentOrMostRecentSleepWindow(
            Instant.now(),
            schedule,
        )
        backfillForWindow(window)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
