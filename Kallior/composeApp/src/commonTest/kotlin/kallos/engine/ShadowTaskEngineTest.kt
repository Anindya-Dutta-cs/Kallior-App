package kallos.engine

import kallos.domain.ShadowProfile
import kallos.model.Category
import kallos.model.Task
import kallos.model.TaskStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class ShadowTaskEngineTest {

    @Test
    fun pendingWhenNotTargeted() {
        val now = Instant.fromEpochMilliseconds(1000)
        val task = Task.create(Category.Work, shelvedAt = now)
        val shadow = ShadowProfile()
        assertEquals(ShadowTaskState.PENDING, ShadowTaskEngine.stateOf(task, shadow))
    }

    @Test
    fun inProgressWhenTargeted() {
        val now = Instant.fromEpochMilliseconds(1000)
        val task = Task.create(Category.Work, shelvedAt = now)
            .copy(id = "target-task")
        val shadow = ShadowProfile(currentShadowTaskId = "target-task")
        assertEquals(ShadowTaskState.IN_PROGRESS, ShadowTaskEngine.stateOf(task, shadow))
    }

    @Test
    fun doneWhenCompletedByUser() {
        val now = Instant.fromEpochMilliseconds(1000)
        val task = Task.create(Category.Work, shelvedAt = now)
            .copy(status = TaskStatus.Completed, completedAt = now + 3.hours)
        val shadow = ShadowProfile()
        assertEquals(ShadowTaskState.DONE, ShadowTaskEngine.stateOf(task, shadow))
    }

    @Test
    fun doneWhenShadowCompletedButTaskStillPending() {
        val now = Instant.fromEpochMilliseconds(1000)
        val task = Task.create(Category.Work, shelvedAt = now)
            .copy(id = "shadow-completed", status = TaskStatus.Pending)
        val shadow = ShadowProfile(shadowCompletedTaskIds = listOf("shadow-completed"))
        // The task is still Pending for the user, but the shadow completed it → DONE
        assertEquals(ShadowTaskState.DONE, ShadowTaskEngine.stateOf(task, shadow))
    }

    @Test
    fun shadowDurationIs3Hours() {
        assertEquals(3.hours, ShadowTaskEngine.SHADOW_TASK_DURATION)
    }

    @Test
    fun shadowCompletedCountCountsUserAndShadowCompletions() {
        val now = Instant.fromEpochMilliseconds(1000)
        val t1 = Task.create(Category.Work, shelvedAt = now)
            .copy(id = "t1", status = TaskStatus.Completed, completedAt = now)
        val t2 = Task.create(Category.Work, shelvedAt = now)
            .copy(id = "t2") // pending
        val t3 = Task.create(Category.Work, shelvedAt = now)
            .copy(id = "t3", status = TaskStatus.Pending) // shadow completed
        val shadow = ShadowProfile(shadowCompletedTaskIds = listOf("t3"))
        // t1: user completed, t3: shadow completed, t2: pending
        assertEquals(2, ShadowTaskEngine.shadowCompletedCount(listOf(t1, t2, t3), shadow))
    }
}
