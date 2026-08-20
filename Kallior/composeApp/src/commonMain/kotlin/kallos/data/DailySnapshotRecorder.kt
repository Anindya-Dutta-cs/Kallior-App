package kallos.data

import kallos.domain.DailyMetricSnapshot
import kallos.domain.RadarScores
import kotlinx.datetime.LocalDate

object DailySnapshotRecorder {

    fun recordYesterday(
        snapshots: List<DailyMetricSnapshot>,
        yesterdayDate: LocalDate,
        yesterdayScores: RadarScores
    ): List<DailyMetricSnapshot> {
        val existing = snapshots.find { it.date == yesterdayDate }
        if (existing != null) {
            // Already recorded for yesterday, return as is (idempotent)
            return snapshots
        }

        val lastSnapshot = snapshots.lastOrNull()
        val consistencyList = updateList(lastSnapshot?.consistencyList, yesterdayScores.consistency)
        val disciplineList = updateList(lastSnapshot?.disciplineList, yesterdayScores.discipline)
        val focusList = updateList(lastSnapshot?.focusList, yesterdayScores.focus)
        val healthList = updateList(lastSnapshot?.healthList, yesterdayScores.health)
        val resilienceList = updateList(lastSnapshot?.resilienceList, yesterdayScores.resilience)

        val yestAvg = (yesterdayScores.consistency + yesterdayScores.discipline + yesterdayScores.focus + yesterdayScores.health) / 4.0

        val newSnapshot = DailyMetricSnapshot(
            date = yesterdayDate,
            tasksCompleted = 0,
            tasksScheduled = 0,
            entertainmentMinutes = 0.0,
            totalScreenMinutes = 0.0,
            blockerAttempts = 0,
            blockerBypasses = 0,
            healthScoreRaw = 0.0,
            consistencyList = consistencyList,
            disciplineList = disciplineList,
            focusList = focusList,
            healthList = healthList,
            resilienceList = resilienceList,
            todayAvg = yestAvg,
            yestAvg = yestAvg
        )

        return snapshots + newSnapshot
    }

    private fun updateList(list: List<Double>?, newValue: Double): List<Double> {
        val current = list ?: emptyList()
        val updated = current + newValue
        return if (updated.size > 4) updated.takeLast(4) else updated
    }
}
