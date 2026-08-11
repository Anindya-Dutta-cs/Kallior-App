package kallos.engine

import kallos.domain.ShadowProfile
import kallos.model.Task
import kallos.model.TaskStatus
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object ConsistencyEngine {

    private fun Task.createdAt(): Instant {
        return activatedAt ?: shelvedAt ?: Instant.fromEpochMilliseconds(0)
    }

    /** [completedAtOf] is the lens: user passes task.completedAt. */
    fun score(tasks: List<Task>, now: Instant, completedAtOf: (Task, Instant) -> Instant?): Double {
        val window = 4.days
        val threshold = now - window
        val due = tasks.filter { it.createdAt() >= threshold }
        val done = due.count { completedAtOf(it, now) != null }
        return if (due.isEmpty()) 100.0 else (100.0 * done) / due.size.toDouble()
    }

    fun userConsistency(tasks: List<Task>, now: Instant): Double =
        score(tasks, now) { t, _ -> t.completedAt }

    /**
     * Shadow consistency = completed tasks / total tasks * 100.
     * A task counts as "completed" for the shadow if the user completed
     * it ([TaskStatus.Completed]) OR the shadow completed it on its own
     * side (tracked in [ShadowProfile.shadowCompletedTaskIds]).
     *
     * Returns 0 when no tasks exist.
     */
    fun shadowConsistency(
        tasks: List<Task>,
        shadow: ShadowProfile,
        now: Instant,
    ): Double {
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todaysTasks = tasks.filter {
            it.createdAt().toLocalDateTime(TimeZone.currentSystemDefault()).date == today
        }
        if (todaysTasks.isEmpty()) return 0.0
        val completed = todaysTasks.count {
            it.status == TaskStatus.Completed ||
            it.id in shadow.shadowCompletedTaskIds
        }
        return (100.0 * completed) / todaysTasks.size.toDouble()
    }
}
