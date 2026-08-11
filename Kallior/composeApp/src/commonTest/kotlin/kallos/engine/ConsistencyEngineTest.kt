package kallos.engine

import kallos.domain.ShadowProfile
import kallos.model.Category
import kallos.model.Task
import kallos.model.TaskStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class ConsistencyEngineTest {

    @Test
    fun shadowConsistencyOnlyCountsTodayTasks() {
        val now = Instant.fromEpochMilliseconds(1000) + 10.days
        // Task from yesterday
        val t1 = Task.create(Category.Work, shelvedAt = now - 1.days)
            .copy(status = TaskStatus.Completed, completedAt = now - 1.days)
        // Task from today
        val t2 = Task.create(Category.Work, shelvedAt = now)
            .copy(status = TaskStatus.Completed, completedAt = now)
        // Another task from today (incomplete)
        val t3 = Task.create(Category.Work, shelvedAt = now)
        
        val tasks = listOf(t1, t2, t3)
        val shadow = ShadowProfile()

        // Shadow should only see t2 and t3. t2 is completed, t3 is not. 1/2 = 50%
        assertEquals(50.0, ConsistencyEngine.shadowConsistency(tasks, shadow, now))
    }

    @Test
    fun sameTaskSetThroughBothLenses() {
        val now = Instant.fromEpochMilliseconds(1000) + 10.days
        val t1 = Task.create(Category.Work, shelvedAt = now)
            .copy(status = TaskStatus.Completed, completedAt = now)
        val t2 = Task.create(Category.Work, shelvedAt = now) // incomplete
        val tasks = listOf(t1, t2)

        val userScore = ConsistencyEngine.userConsistency(tasks, now)
        val shadowScore = ConsistencyEngine.shadowConsistency(tasks, ShadowProfile(), now)

        // User: t1 is due and completed; t2 is due and not completed. 50%
        assertEquals(50.0, userScore)
        // Shadow: counts Completed across today's tasks. 1/2 = 50%
        assertEquals(50.0, shadowScore, 0.01)
    }

    @Test
    fun emptyTasksReturnsZeroForShadow() {
        val now = Instant.fromEpochMilliseconds(1000)
        assertEquals(0.0, ConsistencyEngine.shadowConsistency(emptyList(), ShadowProfile(), now))
    }

    @Test
    fun allCompletedReturns100ForShadow() {
        val now = Instant.fromEpochMilliseconds(1000)
        val t1 = Task.create(Category.Work, shelvedAt = now)
            .copy(status = TaskStatus.Completed, completedAt = now)
        val t2 = Task.create(Category.Work, shelvedAt = now)
            .copy(status = TaskStatus.Completed, completedAt = now)
        assertEquals(100.0, ConsistencyEngine.shadowConsistency(listOf(t1, t2), ShadowProfile(), now))
    }

    @Test
    fun shadowCompletedTaskIdsCountEvenIfTaskStillPending() {
        val now = Instant.fromEpochMilliseconds(1000)
        val t1 = Task.create(Category.Work, shelvedAt = now)
            .copy(id = "shadow-done")
        val t2 = Task.create(Category.Work, shelvedAt = now) // pending
        val shadow = ShadowProfile(shadowCompletedTaskIds = listOf("shadow-done"))

        // t1 is still Pending in the task list, but the shadow completed it.
        // Shadow sees 1 completed out of 2 → 50%
        assertEquals(50.0, ConsistencyEngine.shadowConsistency(listOf(t1, t2), shadow, now))
    }

    @Test
    fun windowMath() {
        val now = Instant.fromEpochMilliseconds(1000) + 10.days
        val oldTask = Task.create(Category.Work, shelvedAt = now - 8.days) // outside 4-day window
        val score = ConsistencyEngine.userConsistency(listOf(oldTask), now)
        assertEquals(100.0, score) // due is empty, returns 100.0
    }
}
