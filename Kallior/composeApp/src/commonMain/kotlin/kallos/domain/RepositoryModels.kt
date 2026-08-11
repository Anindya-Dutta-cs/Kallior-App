package kallos.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyStats(
    @SerialName("date") val date: LocalDate,
    @SerialName("tasks_completed") val tasksCompleted: Int = 0,
    @SerialName("tasks_scheduled") val tasksScheduled: Int = 0,
    @SerialName("entertainment_minutes") val entertainmentMinutes: Double = 0.0,
    @SerialName("total_screen_minutes") val totalScreenMinutes: Double = 0.0,
    @SerialName("blocker_attempts") val blockerAttempts: Int = 0,
    @SerialName("blocker_bypasses") val blockerBypasses: Int = 0,
    @SerialName("health_score_raw") val healthScoreRaw: Double = 0.0,
    @SerialName("steps") val steps: Int = 0,
    @SerialName("minutes_slept") val minutesSlept: Double = 0.0,
    @SerialName("self_app_open") val selfAppOpen: Int = 0,
    @SerialName("today_avg") val todayAvg: Double = 0.0,
    @SerialName("yest_avg") val yestAvg: Double = 0.0,
    @SerialName("sleep_score") val sleepScore: Double = 0.0,
    @SerialName("step_score") val stepScore: Double = 0.0,
    @SerialName("excess_entertainment") val excessEntertainment: Double = 0.0,
    @SerialName("completion_rate") val completionRate: Double = 0.0,
    @SerialName("kp") val kp: Int = 0,
    @SerialName("consistency_list") val consistencyList: List<Double> = emptyList(),
    @SerialName("discipline_list") val disciplineList: List<Double> = emptyList(),
    @SerialName("focus_list") val focusList: List<Double> = emptyList(),
    @SerialName("health_list") val healthList: List<Double> = emptyList(),
    @SerialName("resilience_list") val resilienceList: List<Double> = emptyList(),
)

@Serializable
data class ShadowStats(
    @SerialName("date") val date: LocalDate
    // more stats will be added
)
