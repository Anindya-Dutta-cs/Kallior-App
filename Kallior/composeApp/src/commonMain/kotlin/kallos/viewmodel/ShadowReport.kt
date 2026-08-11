package kallos.viewmodel

import kallos.repository.GameState

data class ShadowReport(
    val tasksMissed: Int = 0,
    val divergenceMoment: String? = null,
)

fun GameState.weeklyReport(): ShadowReport = ShadowReport(
    tasksMissed = shadow.tasksClaimedCount,
    divergenceMoment = shadow.divergenceMomentDescription,
)
