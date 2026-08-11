package kallos.viewmodel

import kallos.engine.GameClock
import kallos.model.Category
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Exercises the lifetime task counters on [GameViewModel] (tasksScheduled ->
 * [GameViewModel.totalTasks], tasksCompleted -> [GameViewModel.completedTasks]).
 *
 * A custom [GameClock] lets us advance time so a freshly created (Shelved) task can
 * be activated and completed without sleeping. The default shelving window is
 * [kallos.model.GameConstants.DEFAULT_ESTIMATE_MINUTES] (= 2) minutes.
 */
private class CounterTestClock(var instant: Instant) : GameClock {
    override fun now(): Instant = instant
    override fun today(): LocalDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
}

class GameViewModelCounterTest {

    @Test
    fun creatingTaskIncrementsScheduledCount() {
        val vm = GameViewModel()
        assertEquals(0, vm.totalTasks)

        vm.addTask(Category.Work)
        assertEquals(1, vm.totalTasks)
        assertEquals(0, vm.completedTasks)
    }

    @Test
    fun completingTaskIncrementsCompletedCount() {
        val clock = CounterTestClock(Instant.parse("2026-05-31T08:00:00Z"))
        val vm = GameViewModel(clock = clock)
        vm.addTask(Category.Work)

        val taskId = vm.tasks.first().id
        // Advance past the shelving window so the task can be activated and completed.
        clock.instant += 5.minutes
        vm.completeTask(taskId)

        assertEquals(1, vm.completedTasks)
        // Completing must not change the scheduled count.
        assertEquals(1, vm.totalTasks)
    }

    @Test
    fun deletingTaskDecrementsScheduledCount() {
        val vm = GameViewModel()
        vm.addTask(Category.Work)
        val taskId = vm.tasks.first().id

        vm.deleteTask(taskId)
        assertEquals(0, vm.totalTasks)
    }

    @Test
    fun completedTaskStillCountsAfterDelete() {
        val clock = CounterTestClock(Instant.parse("2026-05-31T08:00:00Z"))
        val vm = GameViewModel(clock = clock)
        vm.addTask(Category.Work)
        val taskId = vm.tasks.first().id
        clock.instant += 5.minutes
        vm.completeTask(taskId)

        vm.deleteTask(taskId)
        // The scheduled count drops by one, but the completion still counts.
        assertEquals(0, vm.totalTasks)
        assertEquals(1, vm.completedTasks)
    }
}
