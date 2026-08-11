package org.example.project.health

import android.content.Context

/**
 * Simple service-locator for the health / awake-time subsystem.
 * All instances are lazy-initialised and scoped to the application context.
 *
 * This keeps the health layer isolated from the Shadow system (per plan §1).
 */
object HealthDependencies {

    @Volatile private var sleepScheduleStore: SleepScheduleStore? = null
    @Volatile private var awakeIntervalRepository: AwakeIntervalRepository? = null
    @Volatile private var sleepWindowCalculator: SleepWindowCalculator? = null
    @Volatile private var awakeTimeTracker: AwakeTimeTracker? = null
    @Volatile private var usageStatsBackfiller: UsageStatsAwakeBackfiller? = null
    @Volatile private var healthConnectPermissionHelper: HealthConnectPermissionHelper? = null

    fun sleepScheduleStore(context: Context): SleepScheduleStore =
        sleepScheduleStore ?: synchronized(this) {
            sleepScheduleStore ?: SleepScheduleStore(context.applicationContext).also {
                sleepScheduleStore = it
            }
        }

    fun awakeIntervalRepository(context: Context): AwakeIntervalRepository =
        awakeIntervalRepository ?: synchronized(this) {
            awakeIntervalRepository ?: AwakeIntervalRepository(context.applicationContext).also {
                awakeIntervalRepository = it
            }
        }

    fun sleepWindowCalculator(): SleepWindowCalculator =
        sleepWindowCalculator ?: synchronized(this) {
            sleepWindowCalculator ?: SleepWindowCalculator().also {
                sleepWindowCalculator = it
            }
        }

    fun awakeTimeTracker(context: Context): AwakeTimeTracker =
        awakeTimeTracker ?: synchronized(this) {
            awakeTimeTracker ?: AwakeTimeTracker(
                sleepWindowCalculator(),
                awakeIntervalRepository(context),
            ).also {
                awakeTimeTracker = it
            }
        }

    fun usageStatsBackfiller(context: Context): UsageStatsAwakeBackfiller =
        usageStatsBackfiller ?: synchronized(this) {
            usageStatsBackfiller ?: UsageStatsAwakeBackfiller(
                context.applicationContext,
                awakeIntervalRepository(context),
                sleepWindowCalculator(),
            ).also {
                usageStatsBackfiller = it
            }
        }

    fun healthConnectPermissionHelper(context: Context): HealthConnectPermissionHelper =
        healthConnectPermissionHelper ?: synchronized(this) {
            healthConnectPermissionHelper ?: HealthConnectPermissionHelper(
                context.applicationContext,
            ).also {
                healthConnectPermissionHelper = it
            }
        }
}
