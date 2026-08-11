package kallos.engine

import kallos.domain.DailyMetricSnapshot
import kallos.domain.RadarScores

object RadarChartEngine {
    private const val ENTERTAINMENT_ALLOWANCE_MINUTES = 45.0
    private const val SLEEP_TARGET_MINUTES = 470.0
    private const val STEP_TARGET = 8000.0
    private const val COMPLETION_THRESHOLD = 0.80
    private const val NORMALIZED_MAX = 100.0
    private const val NO_TASKS_PENALTY = 25

    fun computeScores(snapshots: List<DailyMetricSnapshot>): RadarScores {
        if (snapshots.isEmpty()) return RadarScores(0.0, 0.0, 0.0, 0.0, 0.0)
        val today = snapshots.last()
        val consistency = computeConsistency(today)
        val discipline = computeDiscipline(today)
        val focus = computeFocus(today)
        val health = computeHealth(today)
        val todayAvg = (consistency + discipline + focus + health) / 4.0
        val resilience = computeResilience(todayAvg, today.yestAvg)
        return RadarScores(
            consistency = consistency,
            discipline = discipline,
            focus = focus,
            health = health,
            resilience = resilience,
        )
    }

    private fun computeConsistency(snapshot: DailyMetricSnapshot): Double {
        // No credit when there is no engagement at all: the app wasn't opened and no task
        // was scheduled. Once a task exists, consistency reflects the completion rate even
        // if the platform hasn't reported an app-open signal (selfAppOpen stays 0 here).
        // completionRate is calculated in GameViewModel and passed via the snapshot.
        if (snapshot.selfAppOpen == 0 && snapshot.tasksScheduled == 0) return 0.0
        if (snapshot.tasksScheduled == 0) {
            return (NORMALIZED_MAX - NO_TASKS_PENALTY).coerceAtLeast(0.0)
        }
        val completionRate = snapshot.completionRate
        return if (completionRate >= COMPLETION_THRESHOLD) {
            NORMALIZED_MAX
        } else {
            (completionRate / COMPLETION_THRESHOLD) * NORMALIZED_MAX
        }
    }

    private fun computeDiscipline(snapshot: DailyMetricSnapshot): Double {
        if (snapshot.totalScreenMinutes <= 0.0) return 100.0
        val excess = snapshot.entertainmentMinutes - ENTERTAINMENT_ALLOWANCE_MINUTES
        if (excess < 0.0) return NORMALIZED_MAX
        return (NORMALIZED_MAX - (excess / snapshot.totalScreenMinutes) * NORMALIZED_MAX)
            .coerceIn(0.0, NORMALIZED_MAX)
    }

    private fun computeFocus(snapshot: DailyMetricSnapshot): Double {
        return (NORMALIZED_MAX - snapshot.blockerBypasses * 8).coerceAtLeast(0.0)
    }

    private fun computeHealth(snapshot: DailyMetricSnapshot): Double {
        val sleepScore = computeSleepScore(snapshot.minutesSlept)
        val stepScore = computeStepScore(snapshot.steps)
        return (sleepScore * 0.65) + (stepScore * 0.35)
    }

    fun computeStepScore(steps: Int): Double {
        return ((steps.toDouble() / STEP_TARGET) * NORMALIZED_MAX).coerceAtMost(NORMALIZED_MAX)
    }

    fun computeSleepScore(minutesSlept: Double): Double {
        return ((minutesSlept / SLEEP_TARGET_MINUTES) * NORMALIZED_MAX).coerceAtMost(NORMALIZED_MAX)
    }

    private fun computeResilience(todayAvg: Double, yestAvg: Double): Double {
        val denominator = yestAvg + todayAvg
        if (denominator <= 0.0) return 0.0
        return (todayAvg / denominator) * NORMALIZED_MAX
    }

    /**
     * Computes the area of the radar polygon defined by [scores].
     * The five axes are ordered sequentially around the chart and wrap
     * around (scores[5] is scores[0]). The area formula is:
     *
     *     area = 0.5 * sin(72°) * sum(r_i * r_{i+1})
     *     r_i = maxRadius * (scores[i] / 100)
     */
    fun computeArea(scores: RadarScores, maxRadius: Double = 100.0): Double {
        val sin72 = 0.9510565162951535
        val values = listOf(
            scores.consistency,
            scores.discipline,
            scores.focus,
            scores.health,
            scores.resilience,
        )
        var sumOfProducts = 0.0
        for (i in values.indices) {
            val nextIndex = (i + 1) % values.size
            val r1 = maxRadius * (values[i] / 100.0)
            val r2 = maxRadius * (values[nextIndex] / 100.0)
            sumOfProducts += r1 * r2
        }
        return 0.5 * sin72 * sumOfProducts
    }
}
