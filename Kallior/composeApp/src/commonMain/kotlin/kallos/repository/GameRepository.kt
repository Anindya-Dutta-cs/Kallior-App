package kallos.repository

import kallos.data.DataRepository
import kallos.data.LocalPersistence
import kallos.domain.DailyMetricSnapshot
import kallos.domain.PlayerProfile
import kallos.domain.RadarScores
import kallos.domain.ShadowProfile
import kallos.engine.RadarChartEngine
import kallos.model.Task
import kallos.model.Remainder

class GameRepository(
    private val taskRepository: TaskRepository = TaskRepository(),
    private val snapshotRepository: MetricSnapshotRepository = MetricSnapshotRepository(),
    private val remainderRepository: RemainderRepository = RemainderRepository(),
    val dataRepository: DataRepository? = null,
    private val localPersistence: LocalPersistence? = null,
) {
    var player: PlayerProfile = PlayerProfile()
    var shadow: ShadowProfile = ShadowProfile()
    var tasksScheduled: Int = 0
    var tasksCompleted: Int = 0

    val tasks: List<Task> get() = taskRepository.tasks
    val snapshots: List<DailyMetricSnapshot> get() = snapshotRepository.snapshots
    val remainders: List<Remainder> get() = remainderRepository.remainders

    suspend fun saveLocally() {
        localPersistence?.saveGameState(snapshot())
    }

    suspend fun loadLocally() {
        localPersistence?.loadGameState()?.let { restore(it) }
    }

    fun addTask(task: Task) = taskRepository.addTask(task)
    fun updateTask(task: Task) = taskRepository.updateTask(task)
    fun removeTask(taskId: String) = taskRepository.removeTask(taskId)
    fun findTask(taskId: String): Task? = taskRepository.findById(taskId)

    fun addRemainder(remainder: Remainder) = remainderRepository.addRemainder(remainder)
    fun updateRemainder(remainder: Remainder) = remainderRepository.editRemainder(remainder)
    fun removeRemainder(remainderId: String) = remainderRepository.deleteRemainder(remainderId)

    fun addOrUpdateSnapshot(snapshot: DailyMetricSnapshot) =
        snapshotRepository.addOrUpdate(snapshot)

    fun snapshotWindow(today: kotlinx.datetime.LocalDate, days: Int = 4): List<DailyMetricSnapshot> =
        snapshotRepository.window(today, days)

    fun snapshot(): GameState = GameState(player, shadow, tasks, snapshots, remainders, tasksScheduled, tasksCompleted)

    fun restore(state: GameState) {
        player = state.player
        shadow = state.shadow
        tasksScheduled = state.tasksScheduled
        tasksCompleted = state.tasksCompleted
        taskRepository.replaceAll(state.tasks)
        snapshotRepository.replaceAll(state.snapshots)
        remainderRepository.replaceAll(state.remainders)
    }
}
