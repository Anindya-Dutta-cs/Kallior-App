package kallos.engine

import kallos.domain.DailyMetricSnapshot
import kallos.domain.RadarScores
import kallos.domain.ShadowProfile
import kotlin.math.roundToInt

/**
 * Computes the shadow's radar scores from the user's scores, the rolling
 * 4-day history kept on [DailyMetricSnapshot] and the [ShadowProfile].
 */
object ShadowRadarEngine {

    /** Maximum value any shadow axis can take. */
    const val CEILING = 99.0

    private const val TOP_N_FOR_AVERAGE = 3

    fun computeShadowScores(
        userScores: RadarScores,
        snapshots: List<DailyMetricSnapshot>,
        tasks: List<kallos.model.Task>,
        now: kotlin.time.Instant,
        shadow: ShadowProfile = ShadowProfile(),
    ): RadarScores {
        val consistency = ConsistencyEngine.shadowConsistency(tasks, shadow, now).coerceAtMost(CEILING)
        val discipline = baseShadowValue(
            snapshots = snapshots,
            selector = { it.disciplineList },
            fallback = userScores.discipline,
        ).coerceAtMost(CEILING)
        val focus = baseShadowValue(
            snapshots = snapshots,
            selector = { it.focusList },
            fallback = userScores.focus,
        ).coerceAtMost(CEILING)
        val health = baseShadowValue(
            snapshots = snapshots,
            selector = { it.healthList },
            fallback = userScores.health,
        ).coerceAtMost(CEILING)

        // Momentum ratio, mirroring the user's resilience (see RadarChartEngine):
        // today's four-axis average vs yesterday's. No previous history yet means
        // nothing to decline from, so the ratio resolves to 100 (capped at CEILING).
        val todayAvg = (consistency + discipline + focus + health) / 4.0
        val yestAvg = yesterdayAverage(consistency, snapshots, userScores)
        val resilience = momentumRatio(todayAvg, yestAvg).coerceAtMost(CEILING)

        return RadarScores(
            consistency = consistency,
            discipline = discipline,
            focus = focus,
            health = health,
            resilience = resilience,
        )
    }

    /**
     * The shadow's four-axis average as of the previous snapshot. Discipline,
     * focus and health use the same top-3 history rule against that snapshot's
     * lists. Consistency is task-based and no historical task state is kept,
     * so today's value stands in for yesterday's.
     */
    private fun yesterdayAverage(
        consistency: Double,
        snapshots: List<DailyMetricSnapshot>,
        userScores: RadarScores,
    ): Double {
        val previous = snapshots.dropLast(1).lastOrNull() ?: return 0.0
        val discipline = baseShadowValue(listOf(previous), { it.disciplineList }, userScores.discipline)
        val focus = baseShadowValue(listOf(previous), { it.focusList }, userScores.focus)
        val health = baseShadowValue(listOf(previous), { it.healthList }, userScores.health)
        return (consistency + discipline + focus + health) / 4.0
    }

    /** The user's resilience formula: todayAvg / (yestAvg + todayAvg) * 100. */
    private fun momentumRatio(todayAvg: Double, yestAvg: Double): Double {
        val denominator = yestAvg + todayAvg
        if (denominator <= 0.0) return 0.0
        return (todayAvg / denominator) * 100.0
    }

    private fun baseShadowValue(
        snapshots: List<DailyMetricSnapshot>,
        selector: (DailyMetricSnapshot) -> List<Double>,
        fallback: Double,
    ): Double {
        val list = snapshots.lastOrNull()?.let(selector).orEmpty()
        if (list.isEmpty()) return fallback
        val top = list.sortedDescending().take(TOP_N_FOR_AVERAGE)
        val average = top.average()
        return average.roundToInt().toDouble()
    }
}
