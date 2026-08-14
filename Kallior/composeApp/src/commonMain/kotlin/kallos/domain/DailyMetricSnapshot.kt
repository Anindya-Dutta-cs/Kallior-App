package kallos.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class DailyMetricSnapshot(
    val date: LocalDate,
    val tasksCompleted: Int,
    val tasksScheduled: Int,
    val entertainmentMinutes: Double,
    val totalScreenMinutes: Double,
    val blockerAttempts: Int,
    val blockerBypasses: Int,
    val healthScoreRaw: Double,
    val steps: Int = 0,
    val minutesSlept: Double = 0.0,
    val selfAppOpen: Int = 0,
    val todayAvg: Double = 0.0,
    val yestAvg: Double = 0.0,
    val sleepScore: Double = 0.0,
    val stepScore: Double = 0.0,
    // Raw sleep/awake components — stored separately for future recalculation.
    val scheduledSleepMinutes: Double = 0.0,
    val healthConnectSleepMinutes: Double = 0.0,
    val phoneAwakeMinutes: Long = 0,
    val sleepSource: String = "NONE", // "HEALTH_CONNECT", "USER_SCHEDULE", "NONE"
    val excessEntertainment: Double = 0.0,
    val completionRate: Double = 0.0,
    val consistencyList: List<Double> = emptyList(),
    val disciplineList: List<Double> = emptyList(),
    val focusList: List<Double> = emptyList(),
    val healthList: List<Double> = emptyList(),
    val resilienceList: List<Double> = emptyList(),
) {
    companion object {
        private const val MAX_LIST_LENGTH = 4

        /**
         * Stores the day's score in the field's rolling history. Each list
         * holds one score per day: a later sync on the same day replaces
         * that day's entry instead of appending a duplicate, so the list
         * always spans up to [MAX_LIST_LENGTH] distinct days. Once full,
         * the oldest (first-in) day is dropped.
         */
        fun updateScoreList(list: List<Double>, score: Double?): List<Double> {
            val newScore = score ?: 0.0
            return if (list.isEmpty()) {
                listOf(newScore)
            } else {
                (list.dropLast(1) + newScore).takeLast(MAX_LIST_LENGTH)
            }
        }
    }
}

data class RadarScores(
    val consistency: Double,
    val discipline: Double,
    val focus: Double,
    val health: Double,
    val resilience: Double,
) {
    val axes: List<RadarAxis>
        get() = listOf(
            RadarAxis("Consistency", consistency),
            RadarAxis("Discipline", discipline),
            RadarAxis("Focus", focus),
            RadarAxis("Health", health),
            RadarAxis("Resilience", resilience),
        )
}

data class RadarAxis(
    val label: String,
    val value: Double,
)
