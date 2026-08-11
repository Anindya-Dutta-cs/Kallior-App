package kallos.platform

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.example.project.BlockerStatsTracker
import org.example.project.health.HealthDependencies
import java.time.Instant

class AndroidPlatformMetricsCollector : PlatformMetricsCollector {
    override fun collect(): PlatformMetrics {
        val fetcher = PlatformDataFetcher()
        val screenTime = runBlocking { fetcher.getScreenTimeData() }
        val stepData = runBlocking { fetcher.getStepData() }
        val sleepData = runBlocking { fetcher.getSleepData() }

        // Compute awake-time correction for sleep.
        // If Health Connect provided actual sleep stages, use that directly.
        // Otherwise, use the user's schedule-based sleep hours minus phone awake time.
        val context = ApplicationContextHolder.applicationContext
        val sleepMinutes: Double
        val awakeMinutes: Long
        val scheduledSleepMin: Double
        val hcSleepMin: Double
        val sleepSrc: String

        if (context != null) {
            val scheduleStore = HealthDependencies.sleepScheduleStore(context)
            val schedule = runBlocking { scheduleStore.currentSchedule() }
            val awakeRepo = HealthDependencies.awakeIntervalRepository(context)
            val window = schedule?.let {
                HealthDependencies.sleepWindowCalculator()
                    .currentOrMostRecentSleepWindow(Instant.now(), it)
            }
            awakeMinutes = window?.let {
                runBlocking { awakeRepo.totalMergedSecondsForDate(it.anchorDate.toString()) } / 60
            } ?: 0

            hcSleepMin = sleepData.value
            scheduledSleepMin = schedule?.durationHours()?.times(60.0) ?: 0.0

            if (sleepData.value > 0.0) {
                // Health Connect returned actual sleep data — use that directly
                // without subtracting phone awake time (per plan §2.2 + answers).
                sleepMinutes = sleepData.value
                sleepSrc = "HEALTH_CONNECT"
            } else if (schedule != null && window != null) {
                // A schedule can be in progress. Count only the elapsed portion,
                // then subtract screen-on minutes recorded for that same sleep
                // window (whose anchor date may be tomorrow after bedtime).
                val elapsedMillis =
                    (minOf(Instant.now(), window.end).toEpochMilli() - window.start.toEpochMilli())
                        .coerceAtLeast(0L)
                val elapsedMinutes = elapsedMillis.toDouble() / 60_000.0
                sleepMinutes = (elapsedMinutes - awakeMinutes).coerceAtLeast(0.0)
                sleepSrc = "USER_SCHEDULE"
            } else {
                sleepMinutes = 0.0
                sleepSrc = "NONE"
            }
        } else {
            sleepMinutes = sleepData.value
            awakeMinutes = 0
            hcSleepMin = sleepData.value
            scheduledSleepMin = 0.0
            sleepSrc = if (sleepData.value > 0.0) "HEALTH_CONNECT" else "NONE"
        }

        return PlatformMetrics(
            entertainmentMinutes = screenTime.entertainmentMinutes.toDouble(),
            totalScreenMinutes = screenTime.totalMinutes.toDouble(),
            blockerAttempts = BlockerStatsTracker.currentAttempts,
            blockerBypasses = BlockerStatsTracker.currentBypasses,
            steps = stepData.value.toInt(),
            minutesSlept = sleepMinutes,
            scheduledSleepMinutes = scheduledSleepMin,
            healthConnectSleepMinutes = hcSleepMin,
            phoneAwakeMinutes = awakeMinutes,
            sleepSource = sleepSrc,
        )
    }

    override val updates: Flow<Unit> = merge(
        BlockerStatsTracker.updates.map { Unit },
        flow {
            while (currentCoroutineContext().isActive) {
                emit(Unit)
                delay(SCREEN_TIME_REFRESH_MILLIS)
            }
        },
    )

    private companion object {
        const val SCREEN_TIME_REFRESH_MILLIS = 10_000L
    }
}
