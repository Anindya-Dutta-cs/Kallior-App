package kallos.engine

import kallos.model.Task
import kallos.model.TaskStatus
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

sealed class TaskActionResult {
    data class Activated(val task: Task) : TaskActionResult()
    data class Completed(val task: Task) : TaskActionResult()
    data class ShadowClaimed(val task: Task) : TaskActionResult()
    data class Rejected(val reason: String) : TaskActionResult()
}

object TaskLifecycleEngine {
    /** Shelved -> Pending once [Task.estimateMinutes] have elapsed since shelving. */
    fun canActivate(task: Task, now: Instant): Boolean {
        if (task.status != TaskStatus.Shelved) return false
        val shelvedAt = task.shelvedAt ?: return false
        return now >= shelvedAt + task.estimateMinutes.minutes
    }

    fun activate(task: Task, now: Instant): TaskActionResult {
        if (task.status != TaskStatus.Shelved) {
            return TaskActionResult.Rejected("Only shelved tasks can be activated.")
        }
        val shelvedAt = task.shelvedAt ?: return TaskActionResult.Rejected("Missing shelved timestamp.")
        if (now < shelvedAt + task.estimateMinutes.minutes) {
            return TaskActionResult.Rejected("Still in the shelving window.")
        }
        return TaskActionResult.Activated(
            task.copy(status = TaskStatus.Pending, activatedAt = now),
        )
    }

    /** Pending -> Completed once the user finishes the task. */
    fun complete(task: Task, now: Instant): TaskActionResult {
        if (task.status != TaskStatus.Pending) {
            return TaskActionResult.Rejected("Only pending tasks can be completed.")
        }
        return TaskActionResult.Completed(
            task.copy(status = TaskStatus.Completed, completedAt = now),
        )
    }

    /** Pending -> ShadowClaimed when the user skips the task. */
    fun skip(task: Task, now: Instant): TaskActionResult {
        if (task.status != TaskStatus.Pending) {
            return TaskActionResult.Rejected("Only pending tasks can be skipped.")
        }
        return TaskActionResult.ShadowClaimed(
            task.copy(status = TaskStatus.ShadowClaimed, completedAt = now),
        )
    }

    /** A pending task still open on a later calendar day is claimed by the shadow. */
    fun isExpiredEndOfDay(task: Task, today: LocalDate): Boolean {
        if (task.status != TaskStatus.Pending) return false
        val activatedAt = task.activatedAt ?: return false
        return activatedAt.toLocalDate() < today
    }

    private fun Instant.toLocalDate(): LocalDate =
        toLocalDateTime(TimeZone.currentSystemDefault()).date
}
