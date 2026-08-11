package kallos.engine

import kallos.domain.PlayerProfile
import kallos.domain.ShadowProfile
import kallos.model.Category
import kallos.model.Task
import kallos.model.TaskStatus
import kotlin.time.Instant

/**
 * Result of one shadow tick.
 *
 * @property activatedTasks shelved tasks that became pending this tick.
 */
data class ShadowTick(
    val activatedTasks: List<Task> = emptyList(),
)

/**
 * Engine that drives the shadow side of the game and the automatic task
 * transitions: shelved tasks activate once their estimate elapses, and
 * the shadow picks one random task at a time, completing it after
 * [ShadowTaskEngine.SHADOW_TASK_DURATION] has passed (recording the
 * completion on the [ShadowProfile]).
 *
 * Shadow completions are tracked **separately** from the task's actual
 * status — the task stays [TaskStatus.Pending] for the user, who can
 * still complete it themselves. The shadow records its completions in
 * [ShadowProfile.shadowCompletedTaskIds].
 */
class ShadowEngine(private val clock: GameClock) {

    /**
     * Marks [task] as claimed by the shadow (when the user explicitly
     * skips it), increments the shadow's `tasksClaimedCount`, records a
     * divergence moment on [shadow] and returns the updated task.
     */
    fun claimTaskForShadow(
        task: Task,
        player: PlayerProfile,
        shadow: ShadowProfile,
    ): Task {
        shadow.tasksClaimedCount += 1
        shadow.divergenceMomentDescription =
            "Shadow claimed '${task.title}' on ${clock.today()}"
        shadow.lastShadowTaskTitle = task.title
        return task.copy(status = TaskStatus.ShadowClaimed)
    }

    /**
     * Advances every task that is due: shelved tasks whose estimate has
     * elapsed become pending.
     */
    fun processTasks(
        tasks: List<Task>,
        player: PlayerProfile,
        shadow: ShadowProfile,
    ): ShadowTick {
        val now = clock.now()

        val activated = tasks.mapNotNull { task ->
            if (task.status == TaskStatus.Shelved && TaskLifecycleEngine.canActivate(task, now)) {
                (TaskLifecycleEngine.activate(task, now) as? TaskActionResult.Activated)?.task
            } else {
                null
            }
        }

        return ShadowTick(activatedTasks = activated)
    }

    /**
     * The shadow picks one random uncompleted task at a time. After
     * [ShadowTaskEngine.SHADOW_TASK_DURATION] has passed, the shadow
     * records the completion in [ShadowProfile.shadowCompletedTaskIds]
     * — **without changing the task's actual status** — so the user
     * can still complete it themselves. If the user completes the
     * shadow's target first, it counts for the shadow and a new target
     * is picked. One task at a time.
     */
    fun processShadowTarget(
        tasks: List<Task>,
        shadow: ShadowProfile,
    ) {
        val now = clock.now()
        val currentId = shadow.currentShadowTaskId
        val currentTask = currentId?.let { id -> tasks.find { it.id == id } }

        when {
            // Target was deleted — clear and pick a new one
            currentId != null && currentTask == null -> {
                clearTarget(shadow)
                pickNewTarget(tasks, shadow, now)
            }

            // Target was completed by the user, skipped, or already
            // shadow-completed — counts for shadow, pick next
            currentTask != null && (
                currentTask.status == TaskStatus.Completed ||
                currentTask.status == TaskStatus.ShadowClaimed ||
                currentTask.id in shadow.shadowCompletedTaskIds
            ) -> {
                if (currentTask.status == TaskStatus.Completed) {
                    shadow.tasksClaimedCount += 1
                    shadow.divergenceMomentDescription =
                        "Shadow completed '${currentTask.title}' on ${clock.today()}"
                    shadow.lastShadowTaskTitle = currentTask.title
                }
                clearTarget(shadow)
                pickNewTarget(tasks, shadow, now)
            }

            // 3 hours have passed — shadow completes the task (shadow-side only)
            currentTask != null && shadow.currentShadowTaskStartedAt != null &&
                now >= shadow.currentShadowTaskStartedAt!! +
                    ShadowTaskEngine.SHADOW_TASK_DURATION -> {

                shadow.shadowCompletedTaskIds =
                    shadow.shadowCompletedTaskIds + currentTask.id
                shadow.tasksClaimedCount += 1
                shadow.divergenceMomentDescription =
                    "Shadow completed '${currentTask.title}' on ${clock.today()}"
                shadow.lastShadowTaskTitle = currentTask.title
                clearTarget(shadow)
                pickNewTarget(tasks, shadow, now)
            }

            // No current target — pick one
            currentId == null -> {
                pickNewTarget(tasks, shadow, now)
            }
            // else: target is still in progress, nothing to do
        }
    }

    private fun clearTarget(shadow: ShadowProfile) {
        shadow.currentShadowTaskId = null
        shadow.currentShadowTaskStartedAt = null
    }

    private fun pickNewTarget(tasks: List<Task>, shadow: ShadowProfile, now: Instant) {
        val eligible = tasks.filter {
            it.status != TaskStatus.Completed &&
            it.status != TaskStatus.ShadowClaimed &&
            it.id !in shadow.shadowCompletedTaskIds
        }
        if (eligible.isEmpty()) return

        // Shadow prefers tasks by category priority:
        // 1 - Meditation, 2 - Diet, 3 - Exercise/Work/Other.
        // Picks randomly among tasks within the highest-priority tier available.
        val minPriority = eligible.minOf { categoryPriority(it.category) }
        val tier = eligible.filter { categoryPriority(it.category) == minPriority }
        val picked = tier.random()
        shadow.currentShadowTaskId = picked.id
        shadow.currentShadowTaskStartedAt = now
    }

    private fun categoryPriority(category: Category): Int = when (category) {
        Category.Meditation -> 1
        Category.Diet -> 2
        Category.Exercise, Category.Work, Category.Other -> 3
    }
}
