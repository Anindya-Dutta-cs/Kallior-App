package kallos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kallos.data.DataRepository
import kallos.domain.DailyMetricSnapshot
import kallos.domain.DailyStats
import kallos.domain.RadarScores
import kallos.domain.ShadowGap
import kallos.engine.AvatarEngine
import kallos.engine.CurrencyEngine
import kallos.engine.GameClock
import kallos.engine.RadarChartEngine
import kallos.engine.ShadowEngine
import kallos.engine.ShadowRadarEngine
import kallos.engine.SystemGameClock
import kallos.engine.TaskActionResult
import kallos.engine.TaskLifecycleEngine
import kallos.model.Category
import kallos.model.CompanionContext
import kallos.model.CompanionPersonality
import kallos.model.Task
import kallos.model.TaskStatus
import kallos.platform.NoopMetricsCollector
import kallos.platform.PlatformMetricsCollector
import kallos.repository.GameRepository
import kallos.service.LlmCompanionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus

class GameViewModel(
    private val repository: GameRepository = GameRepository(),
    private val clock: GameClock = SystemGameClock(),
    private val shadowEngine: ShadowEngine = ShadowEngine(clock),
    private val metricsCollector: PlatformMetricsCollector = NoopMetricsCollector,
    private val dataRepository: DataRepository? = repository.dataRepository,
) : ViewModel() {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val player get() = repository.player
    val shadow get() = repository.shadow

    val tasks = mutableStateListOf<Task>()
    val reminders = mutableStateListOf<kallos.model.Remainder>()

    var userScores by mutableStateOf(RadarScores(0.0, 0.0, 0.0, 0.0, 0.0))
        private set
    var shadowScores by mutableStateOf(RadarScores(0.0, 0.0, 0.0, 0.0, 0.0))
        private set
    var shadowGap by mutableStateOf(ShadowGap(0.0, false, 0, null))
        private set

    val shadowHomeState: kotlinx.coroutines.flow.StateFlow<kallos.viewmodel.ShadowHomeState> =
        kotlinx.coroutines.flow.combine(
            androidx.compose.runtime.snapshotFlow { tasks.toList() },
            androidx.compose.runtime.snapshotFlow { repository.snapshots },
            androidx.compose.runtime.snapshotFlow { userScores },
            kotlinx.coroutines.flow.flow {
                while (true) {
                    val now = clock.now()
                    emit(now)
                    // Check for day change in background to trigger reset even without interaction
                    val today = clock.today()
                    if (player.lastActiveDate != null && player.lastActiveDate!! < today) {
                        syncAndRefresh()
                    }
                    kotlinx.coroutines.delay(60_000)
                }
            }
        ) { tasksList, snaps, user, now ->
            val claimedCount = repository.shadow.tasksClaimedCount
            val computedScores = kallos.engine.ShadowRadarEngine.computeShadowScores(user, snaps, tasksList, now, repository.shadow)
            val taskUis = tasksList.map {
                val state = kallos.engine.ShadowTaskEngine.stateOf(it, repository.shadow)
                TaskUi(it, state, state == kallos.engine.ShadowTaskState.DONE)
            }
            kallos.viewmodel.ShadowHomeState(
                scores = computedScores,
                tasks = taskUis,
                claimedCount = claimedCount,
                lastClaimTitle = repository.shadow.lastShadowTaskTitle
            )
        }.stateIn(ioScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), kallos.viewmodel.ShadowHomeState.EMPTY)

    // Lifetime task counters that feed the daily 'tasksScheduled' / 'tasksCompleted'
    // stats. They are adjusted explicitly on create / complete / delete (see addTask,
    // completeTask, deleteTask) instead of being recomputed from the current task list,
    // so a shelved or completed task keeps its contribution until the task is deleted.
    var totalTasks by mutableStateOf(0)
        private set
    var completedTasks by mutableStateOf(0)
        private set

    private var scheduledCount = 0
    private var completedCount = 0

    var statusMessage by mutableStateOf<String?>(null)

    var todaySnapshot by mutableStateOf<DailyMetricSnapshot?>(null)
        private set

    fun clearStatusMessage() {
        statusMessage = null
    }

    private val companionService = LlmCompanionService()

    init {
        // Metrics can require binder, sensor, and DataStore reads. Starting that work
        // from composition blocks Compose's first frame, so perform the initial sync
        // and future refreshes off the UI thread.
        ioScope.launch {
            repository.loadLocally()
            scheduledCount = repository.tasksScheduled
            completedCount = repository.tasksCompleted
            refreshAll()
            metricsCollector.updates.collect {
                syncAndRefresh(updateScoreHistory = false)
            }
        }
    }

    fun addTask(
        category: Category,
        description: String = "",
        customTitle: String? = null,
    ) {
        val task = Task.create(
            category,
            description = description,
            customTitle = customTitle,
            shelvedAt = clock.now(),
        )
        repository.addTask(task)
        scheduledCount += 1
        syncAndRefresh()
    }

    fun completeTask(taskId: String) {
        val task = repository.findTask(taskId) ?: return
        var current = task
        // A shelved task whose timer has elapsed can be activated on the fly so the
        // UI's tick button (which only enables after the countdown) can complete it.
        if (current.status == TaskStatus.Shelved && TaskLifecycleEngine.canActivate(current, clock.now())) {
            current = when (val activated = TaskLifecycleEngine.activate(current, clock.now())) {
                is TaskActionResult.Activated -> {
                    repository.updateTask(activated.task)
                    activated.task
                }
                else -> current
            }
        }
        when (val result = TaskLifecycleEngine.complete(current, clock.now())) {
            is TaskActionResult.Completed -> {
                repository.updateTask(result.task)
                AvatarEngine.onTaskCompleted(repository.player)
                completedCount += 1
                statusMessage = "Completed."
            }
            is TaskActionResult.Rejected -> statusMessage = result.reason
            else -> Unit
        }
        syncAndRefresh()
    }

    fun addReminder(reminder: kallos.model.Remainder) {
        repository.addRemainder(reminder)
        syncAndRefresh()
    }

    fun removeReminder(reminderId: String) {
        repository.removeRemainder(reminderId)
        syncAndRefresh()
    }

    fun skipTask(taskId: String) {
        val task = repository.findTask(taskId) ?: return
        if (task.status != TaskStatus.Pending && task.status != TaskStatus.Shelved) {
            statusMessage = "Only pending or shelved tasks can be skipped to shadow."
            return
        }
        val claimed = shadowEngine.claimTaskForShadow(
            task,
            repository.player,
            repository.shadow,
        )
        repository.updateTask(claimed)
        if (task.status == TaskStatus.Shelved) {
            scheduledCount = (scheduledCount - 1).coerceAtLeast(0)
        }
        statusMessage = "Shadow claimed the task."
        syncAndRefresh()
    }

    fun deleteTask(taskId: String) {
        val task = repository.findTask(taskId)
        if (task?.status != TaskStatus.Pending) {
            scheduledCount = (scheduledCount - 1).coerceAtLeast(0)
        }
        repository.removeTask(taskId)
        syncAndRefresh()
    }

    fun companionContext(): CompanionContext {
        val shadowClaimed = tasks.filter { it.status == TaskStatus.ShadowClaimed }
        val mostSkipped = shadowClaimed
            .groupingBy { it.category }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        return CompanionContext(
            shadowGap = shadowGap,
            mostSkippedCategory = mostSkipped,
            avatarVitality = player.avatar.vitality,
            pendingConfirmedTasks = tasks.count { it.status == TaskStatus.Pending },
            userResilience = userScores.resilience,
        )
    }

    suspend fun askCompanion(
        personality: CompanionPersonality,
        userMessage: String,
    ): String = companionService.respond(personality, companionContext(), userMessage)

    suspend fun mentorHint(): String = askCompanion(CompanionPersonality.Mentor, "")

    fun weeklyShadowReport(): ShadowReport = repository.snapshot().weeklyReport()

    private fun tickShadow() {
        val tick = shadowEngine.processTasks(
            repository.tasks,
            repository.player,
            repository.shadow,
        )
        tick.activatedTasks.forEach { repository.updateTask(it) }

        shadowEngine.processShadowTarget(
            repository.tasks,
            repository.shadow,
        )
    }

    private fun updateTodaySnapshot() {
        val today = clock.today()
        val tasksScheduled = scheduledCount
        val tasksCompleted = completedCount
        totalTasks = tasksScheduled
        completedTasks = tasksCompleted
        val metrics = metricsCollector.collect()
        val prevSnapshot = repository.snapshots.filter { it.date < today }.maxByOrNull { it.date }
        val yestAvg = prevSnapshot?.let { if (it.todayAvg > 0.0) it.todayAvg else it.yestAvg } ?: 0.0
        val sleepScore = RadarChartEngine.computeSleepScore(metrics.minutesSlept)
        val stepScore = RadarChartEngine.computeStepScore(metrics.steps)
        val excessEntertainment = metrics.entertainmentMinutes - 90.0
        val completionRate = if (tasksScheduled > 0) tasksCompleted.toDouble() / tasksScheduled else 0.0
        val existing = repository.snapshots.find { it.date == today }
        val snapshot = DailyMetricSnapshot(
            date = today,
            tasksCompleted = tasksCompleted,
            tasksScheduled = tasksScheduled,
            entertainmentMinutes = metrics.entertainmentMinutes,
            totalScreenMinutes = metrics.totalScreenMinutes,
            blockerAttempts = metrics.blockerAttempts,
            blockerBypasses = metrics.blockerBypasses,
            healthScoreRaw = metrics.healthScoreRaw,
            steps = metrics.steps,
            minutesSlept = metrics.minutesSlept,
            selfAppOpen = metrics.selfAppOpen,
            yestAvg = yestAvg,
            sleepScore = sleepScore,
            stepScore = stepScore,
            scheduledSleepMinutes = metrics.scheduledSleepMinutes,
            healthConnectSleepMinutes = metrics.healthConnectSleepMinutes,
            phoneAwakeMinutes = metrics.phoneAwakeMinutes,
            sleepSource = metrics.sleepSource,
            excessEntertainment = excessEntertainment,
            completionRate = completionRate,
            consistencyList = existing?.consistencyList ?: emptyList(),
            disciplineList = existing?.disciplineList ?: emptyList(),
            focusList = existing?.focusList ?: emptyList(),
            healthList = existing?.healthList ?: emptyList(),
            resilienceList = existing?.resilienceList ?: emptyList(),
        )
        repository.addOrUpdateSnapshot(snapshot)
        todaySnapshot = snapshot
    }

    private fun syncAndRefresh(updateScoreHistory: Boolean = true) {
        val today = clock.today()

        // Reset tasks on a new day, but keep reminders.
        if (player.lastActiveDate != null && player.lastActiveDate!! < today) {
            repository.tasks.forEach { repository.removeTask(it.id) }
            repository.tasksScheduled = 0
            repository.tasksCompleted = 0
            scheduledCount = 0
            completedCount = 0
            shadow.currentShadowTaskId = null
            shadow.currentShadowTaskStartedAt = null
            shadow.shadowCompletedTaskIds = emptyList()
        }
        player.lastActiveDate = today

        repository.tasksScheduled = scheduledCount
        repository.tasksCompleted = completedCount

        tickShadow()
        updateTodaySnapshot()
        tasks.clear()
        tasks.addAll(repository.tasks)
        reminders.clear()
        reminders.addAll(repository.remainders)
        val windowSnapshots = repository.snapshotWindow(today, 4)
        userScores = RadarChartEngine.computeScores(windowSnapshots)
        val todayAvg = (userScores.consistency + userScores.discipline +
            userScores.focus + userScores.health) / 4.0
        windowSnapshots.find { it.date == today }?.let { snapshot ->
            val updated = snapshot.copy(
                todayAvg = todayAvg,
                consistencyList = if (updateScoreHistory) {
                    DailyMetricSnapshot.updateScoreList(snapshot.consistencyList, userScores.consistency)
                } else snapshot.consistencyList,
                disciplineList = if (updateScoreHistory) {
                    DailyMetricSnapshot.updateScoreList(snapshot.disciplineList, userScores.discipline)
                } else snapshot.disciplineList,
                focusList = if (updateScoreHistory) {
                    DailyMetricSnapshot.updateScoreList(snapshot.focusList, userScores.focus)
                } else snapshot.focusList,
                healthList = if (updateScoreHistory) {
                    DailyMetricSnapshot.updateScoreList(snapshot.healthList, userScores.health)
                } else snapshot.healthList,
                resilienceList = if (updateScoreHistory) {
                    DailyMetricSnapshot.updateScoreList(snapshot.resilienceList, userScores.resilience)
                } else snapshot.resilienceList,
            )
            repository.addOrUpdateSnapshot(updated)
            todaySnapshot = updated
        }
        val shadowWindow = repository.snapshotWindow(clock.today(), 4)
        shadowScores = ShadowRadarEngine.computeShadowScores(
            userScores = userScores,
            snapshots = shadowWindow,
            tasks = repository.tasks,
            now = clock.now(),
            shadow = repository.shadow,
        )
        val userArea = RadarChartEngine.computeArea(userScores)
        val shadowArea = RadarChartEngine.computeArea(shadowScores)
        val earnedKp = CurrencyEngine.computeKp(userArea, shadowArea)
        repository.player.kp += earnedKp
        shadowGap = ShadowGap.compute(
            userScores = userScores,
            shadowScores = shadowScores,
            tasksMissed = shadow.tasksClaimedCount,
            divergenceMoment = shadow.divergenceMomentDescription,
        )
        persistDailyStats(today)
        ioScope.launch { repository.saveLocally() }
    }

    private fun persistDailyStats(today: kotlinx.datetime.LocalDate) {
        val snapshot = repository.snapshots.find { it.date == today } ?: return
        val stats = DailyStats(
            date = snapshot.date,
            tasksCompleted = snapshot.tasksCompleted,
            tasksScheduled = snapshot.tasksScheduled,
            entertainmentMinutes = snapshot.entertainmentMinutes,
            totalScreenMinutes = snapshot.totalScreenMinutes,
            blockerAttempts = snapshot.blockerAttempts,
            blockerBypasses = snapshot.blockerBypasses,
            healthScoreRaw = snapshot.healthScoreRaw,
            steps = snapshot.steps,
            minutesSlept = snapshot.minutesSlept,
            selfAppOpen = snapshot.selfAppOpen,
            todayAvg = snapshot.todayAvg,
            yestAvg = snapshot.yestAvg,
            sleepScore = snapshot.sleepScore,
            stepScore = snapshot.stepScore,
            excessEntertainment = snapshot.excessEntertainment,
            completionRate = snapshot.completionRate,
            kp = repository.player.kp,
            consistencyList = snapshot.consistencyList,
            disciplineList = snapshot.disciplineList,
            focusList = snapshot.focusList,
            healthList = snapshot.healthList,
            resilienceList = snapshot.resilienceList,
        )
        val repo = dataRepository ?: return
        ioScope.launch { repo.saveDailyStats(stats) }
    }

    private fun refreshAll() = syncAndRefresh()

    override fun onCleared() {
        ioScope.cancel()
        super.onCleared()
    }
}

data class TaskUi(
    val task: kallos.model.Task,
    val shadowState: kallos.engine.ShadowTaskState,
    val shadowCompleted: Boolean,
)

data class ShadowHomeState(
    val scores: kallos.domain.RadarScores,
    val tasks: List<TaskUi>,
    val claimedCount: Int,
    val lastClaimTitle: String?,
) {
    companion object {
        val EMPTY = ShadowHomeState(kallos.domain.RadarScores(0.0, 0.0, 0.0, 0.0, 0.0), emptyList(), 0, null)
    }
}
