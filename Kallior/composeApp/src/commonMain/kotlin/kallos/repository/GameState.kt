package kallos.repository

import kallos.domain.DailyMetricSnapshot
import kallos.domain.PlayerProfile
import kallos.domain.RadarScores
import kallos.domain.ShadowProfile
import kallos.engine.RadarChartEngine
import kallos.model.Remainder
import kallos.model.Task
import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val player: PlayerProfile = PlayerProfile(),
    val shadow: ShadowProfile = ShadowProfile(),
    val tasks: List<Task> = emptyList(),
    val snapshots: List<DailyMetricSnapshot> = emptyList(),
    val remainders: List<Remainder> = emptyList(),
    val tasksScheduled: Int = 0,
    val tasksCompleted: Int = 0,
) {
    val userScores: RadarScores
        get() = RadarChartEngine.computeScores(snapshots)
}
