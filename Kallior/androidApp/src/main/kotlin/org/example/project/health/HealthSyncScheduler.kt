package org.example.project.health

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Enqueues the periodic [HealthSyncWorker] so health data is refreshed
 * in the background every 15 minutes.
 *
 * Safe to call multiple times — uses [ExistingPeriodicWorkPolicy.KEEP] so
 * the first schedule sticks and later calls are no-ops.
 */
fun scheduleHealthSync(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()

    val request = PeriodicWorkRequestBuilder<HealthSyncWorker>(
        15, TimeUnit.MINUTES,
    )
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "health_sync_worker",
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}
