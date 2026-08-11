package kallos.engine

import kallos.domain.ShadowProfile
import kallos.model.Task
import kallos.model.TaskStatus
import kotlin.time.Duration.Companion.hours

enum class ShadowTaskState { PENDING, IN_PROGRESS, DONE }

object ShadowTaskEngine {
    val SHADOW_TASK_DURATION = 3.hours

    /**
     * Derives the shadow-side state of a task. A task is [DONE] when it
     * has been completed — either by the user (task status is
     * [TaskStatus.Completed]) or by the shadow (task ID is in
     * [ShadowProfile.shadowCompletedTaskIds]). It is [IN_PROGRESS] when
     * the shadow has picked it as its current target, and [PENDING]
     * otherwise.
     *
     * Shadow completions are tracked separately from the task's actual
     * status so the user can still complete the task themselves.
     */
    fun stateOf(task: Task, shadow: ShadowProfile): ShadowTaskState = when {
        task.status == TaskStatus.Completed ||
            task.id in shadow.shadowCompletedTaskIds -> ShadowTaskState.DONE
        shadow.currentShadowTaskId == task.id -> ShadowTaskState.IN_PROGRESS
        else -> ShadowTaskState.PENDING
    }

    /** Count of tasks the shadow considers completed (user + shadow completions). */
    fun shadowCompletedCount(tasks: List<Task>, shadow: ShadowProfile): Int =
        tasks.count {
            it.status == TaskStatus.Completed ||
            it.id in shadow.shadowCompletedTaskIds
        }
}
