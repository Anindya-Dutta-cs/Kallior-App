package org.example.project.health

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kallos.platform.ApplicationContextHolder
import kallos.platform.PlatformDataFetcher
import java.time.LocalDate

/**
 * Periodic health-sync worker that runs in the background (every ~15 min).
 *
 * Responsibilities:
 * 1. Re-reads step and sleep data from Health Connect.
 * 2. Backfills missed awake intervals from UsageStats (if permission granted).
 * 3. The results are consumed on the next [kallos.platform.PlatformMetricsCollector.collect]
 *    call, so no additional persistence is needed here — the awake intervals
 *    are already stored by the backfiller and the screen-event tracker.
 */
class HealthSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Warm up Health Connect data (the next collect() call will read it).
            val fetcher = PlatformDataFetcher()
            fetcher.getStepData()
            fetcher.getSleepData()

            // 2. Backfill awake intervals for the current sleep window.
            val scheduleStore = HealthDependencies.sleepScheduleStore(applicationContext)
            val schedule = scheduleStore.currentSchedule()
            if (schedule != null) {
                val backfiller = HealthDependencies.usageStatsBackfiller(applicationContext)
                backfiller.backfillForCurrentWindow(schedule)
            }

            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
