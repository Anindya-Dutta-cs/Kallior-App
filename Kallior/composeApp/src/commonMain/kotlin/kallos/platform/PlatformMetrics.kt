package kallos.platform

import kotlinx.coroutines.flow.Flow


data class PlatformMetrics(
    val entertainmentMinutes: Double = 0.0,
    val totalScreenMinutes: Double = 0.0,
    val blockerAttempts: Int = 0,
    val blockerBypasses: Int = 0,
    val healthScoreRaw: Double = 50.0,
    val steps: Int = 0,
    val minutesSlept: Double = 0.0,
    val selfAppOpen: Int = 0,
    // Raw sleep/awake components — stored separately for future recalculation.
    val scheduledSleepMinutes: Double = 0.0,
    val healthConnectSleepMinutes: Double = 0.0,
    val phoneAwakeMinutes: Long = 0,
    val sleepSource: String = "NONE", // "HEALTH_CONNECT", "USER_SCHEDULE", "NONE"
)

interface PlatformMetricsCollector {
    fun collect(): PlatformMetrics

    /** Emits when platform metrics may have changed. */
    val updates: Flow<Unit>
}

object NoopMetricsCollector : PlatformMetricsCollector {
    override fun collect(): PlatformMetrics = PlatformMetrics()
    override val updates: Flow<Unit> = kotlinx.coroutines.flow.emptyFlow()
}
