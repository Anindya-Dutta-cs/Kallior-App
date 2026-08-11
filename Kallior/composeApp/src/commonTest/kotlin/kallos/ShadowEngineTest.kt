package kallos

import kallos.domain.DailyMetricSnapshot
import kallos.domain.PlayerProfile
import kallos.domain.RadarScores
import kallos.domain.ShadowProfile
import kallos.engine.GameClock
import kallos.engine.RadarChartEngine
import kallos.engine.ShadowEngine
import kallos.engine.ShadowRadarEngine
import kallos.engine.ShadowTaskEngine
import kallos.engine.ShadowTaskState
import kallos.engine.TaskActionResult
import kallos.engine.TaskLifecycleEngine
import kallos.model.Category
import kallos.model.Task
import kallos.model.TaskStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private class FixedClock(
    private var instant: Instant,
) : GameClock {
    override fun now(): Instant = instant
    override fun today(): LocalDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    fun advance(duration: kotlin.time.Duration) {
        instant += duration
    }
}

class ShadowEngineTest {

    @Test
    fun shelvedTaskActivatesAfterEstimateMinutes() {
        val clock = FixedClock(Instant.parse("2026-05-31T08:00:00Z"))
        val task = Task.create(Category.Exercise, estimateMinutes = 5, shelvedAt = clock.now())

        assertTrue(!TaskLifecycleEngine.canActivate(task, clock.now()))
        clock.advance(4.minutes)
        assertTrue(!TaskLifecycleEngine.canActivate(task, clock.now()))
        clock.advance(2.minutes)
        assertTrue(TaskLifecycleEngine.canActivate(task, clock.now()))

        val activated = TaskLifecycleEngine.activate(task, clock.now()) as TaskActionResult.Activated
        assertEquals(TaskStatus.Pending, activated.task.status)
        assertEquals(clock.now(), activated.task.activatedAt)
    }

    @Test
    fun processTasksHasNoExpiredTasks() {
        val clock = FixedClock(Instant.parse("2026-05-31T08:00:00Z"))
        val engine = ShadowEngine(clock)
        val player = PlayerProfile()
        val shadow = ShadowProfile()
        val pending = Task.create(Category.Work, estimateMinutes = 2, shelvedAt = clock.now())
            .copy(status = TaskStatus.Pending, activatedAt = clock.now())

        clock.advance(24.hours)
        val tick = engine.processTasks(listOf(pending), player, shadow)

        // The old "expire pending tasks from previous day" logic is gone.
        assertTrue(tick.activatedTasks.isEmpty())
    }

    @Test
    fun completeAndSkipTransitions() {
        val clock = FixedClock(Instant.parse("2026-05-31T08:00:00Z"))
        val pending = Task.create(Category.Diet, estimateMinutes = 2, shelvedAt = clock.now())
            .copy(status = TaskStatus.Pending, activatedAt = clock.now())

        val completed = TaskLifecycleEngine.complete(pending, clock.now()) as TaskActionResult.Completed
        assertEquals(TaskStatus.Completed, completed.task.status)

        val skipped = TaskLifecycleEngine.skip(pending, clock.now()) as TaskActionResult.ShadowClaimed
        assertEquals(TaskStatus.ShadowClaimed, skipped.task.status)
    }

    @Test
    fun shadowPicksRandomTaskAndCompletesAfter3Hours() {
        val clock = FixedClock(Instant.parse("2026-05-31T08:00:00Z"))
        val engine = ShadowEngine(clock)
        val shadow = ShadowProfile()
        val task = Task.create(Category.Work, estimateMinutes = 2, shelvedAt = clock.now())
            .copy(status = TaskStatus.Pending, activatedAt = clock.now())

        val tasks = listOf(task)
        // First call: shadow picks the task
        engine.processShadowTarget(tasks, shadow)
        assertEquals(task.id, shadow.currentShadowTaskId)
        assertEquals(clock.now(), shadow.currentShadowTaskStartedAt)

        // Before 3 hours: task is NOT shadow-completed, still Pending
        clock.advance(2.hours)
        engine.processShadowTarget(tasks, shadow)
        assertTrue(shadow.shadowCompletedTaskIds.isEmpty())
        assertEquals(TaskStatus.Pending, tasks.first().status)

        // After 3 hours: shadow completes the task (shadow-side only)
        clock.advance(1.hours)
        engine.processShadowTarget(tasks, shadow)

        // Task ID is in shadow's completed list
        assertEquals(listOf(task.id), shadow.shadowCompletedTaskIds)
        // Task's actual status is STILL Pending — the user can still complete it
        assertEquals(TaskStatus.Pending, tasks.first().status)
        assertEquals(1, shadow.tasksClaimedCount)
        assertEquals(task.title, shadow.lastShadowTaskTitle)
        assertEquals(null, shadow.currentShadowTaskId)
    }

    @Test
    fun shadowUserCompletesTargetCountsForShadow() {
        val clock = FixedClock(Instant.parse("2026-05-31T08:00:00Z"))
        val engine = ShadowEngine(clock)
        val shadow = ShadowProfile()
        val task = Task.create(Category.Work, estimateMinutes = 2, shelvedAt = clock.now())
            .copy(status = TaskStatus.Pending, activatedAt = clock.now())

        val tasks = listOf(task)
        // Shadow picks the task
        engine.processShadowTarget(tasks, shadow)
        assertEquals(task.id, shadow.currentShadowTaskId)

        // User completes the shadow's target before 3 hours
        clock.advance(1.hours)
        val completedTask = task.copy(status = TaskStatus.Completed, completedAt = clock.now())
        engine.processShadowTarget(listOf(completedTask), shadow)

        // Counts for shadow, shadow picks new target (none available)
        assertEquals(1, shadow.tasksClaimedCount)
        assertEquals(null, shadow.currentShadowTaskId)
    }

    @Test
    fun shadowCompletedTaskShowsDoneButUserCanStillComplete() {
        val clock = FixedClock(Instant.parse("2026-05-31T08:00:00Z"))
        val engine = ShadowEngine(clock)
        val shadow = ShadowProfile()
        val task = Task.create(Category.Work, estimateMinutes = 2, shelvedAt = clock.now())
            .copy(id = "task-A", status = TaskStatus.Pending, activatedAt = clock.now())

        val tasks = listOf(task)
        // Shadow picks and completes after 3 hours
        engine.processShadowTarget(tasks, shadow)
        clock.advance(3.hours)
        engine.processShadowTarget(tasks, shadow)

        // Shadow sees the task as DONE
        assertEquals(ShadowTaskState.DONE, ShadowTaskEngine.stateOf(task, shadow))
        // But the task is still Pending — user can still complete it
        assertEquals(TaskStatus.Pending, task.status)

        // Now the user completes the task
        val userCompleted = task.copy(status = TaskStatus.Completed, completedAt = clock.now())
        // Shadow state should still be DONE (now via Completed status)
        assertEquals(ShadowTaskState.DONE, ShadowTaskEngine.stateOf(userCompleted, shadow))
        // And the task IS completed for the user
        assertEquals(TaskStatus.Completed, userCompleted.status)
    }

    @Test
    fun shadowPrefersMeditationOverDietAndExercise() {
        val clock = FixedClock(Instant.parse("2026-05-31T08:00:00Z"))
        val engine = ShadowEngine(clock)
        val shadow = ShadowProfile()
        val meditation = Task.create(Category.Meditation, estimateMinutes = 2, shelvedAt = clock.now())
            .copy(id = "med", status = TaskStatus.Pending, activatedAt = clock.now())
        val diet = Task.create(Category.Diet, estimateMinutes = 2, shelvedAt = clock.now())
            .copy(id = "diet", status = TaskStatus.Pending, activatedAt = clock.now())
        val exercise = Task.create(Category.Exercise, estimateMinutes = 2, shelvedAt = clock.now())
            .copy(id = "ex", status = TaskStatus.Pending, activatedAt = clock.now())

        engine.processShadowTarget(listOf(meditation, diet, exercise), shadow)
        // Priority 1 = Meditation → shadow picks it first
        assertEquals("med", shadow.currentShadowTaskId)
    }

    @Test
    fun shadowPrefersDietWhenNoMeditationAvailable() {
        val clock = FixedClock(Instant.parse("2026-05-31T08:00:00Z"))
        val engine = ShadowEngine(clock)
        val shadow = ShadowProfile()
        val diet = Task.create(Category.Diet, estimateMinutes = 2, shelvedAt = clock.now())
            .copy(id = "diet", status = TaskStatus.Pending, activatedAt = clock.now())
        val exercise = Task.create(Category.Exercise, estimateMinutes = 2, shelvedAt = clock.now())
            .copy(id = "ex", status = TaskStatus.Pending, activatedAt = clock.now())

        engine.processShadowTarget(listOf(diet, exercise), shadow)
        // Priority 2 = Diet → shadow picks it over Exercise
        assertEquals("diet", shadow.currentShadowTaskId)
    }

    @Test
    fun shadowPicksRandomlyWithinSamePriorityTier() {
        val clock = FixedClock(Instant.parse("2026-05-31T08:00:00Z"))
        val engine = ShadowEngine(clock)
        val shadow = ShadowProfile()
        val exercise = Task.create(Category.Exercise, estimateMinutes = 2, shelvedAt = clock.now())
            .copy(id = "ex", status = TaskStatus.Pending, activatedAt = clock.now())
        val work = Task.create(Category.Work, estimateMinutes = 2, shelvedAt = clock.now())
            .copy(id = "work", status = TaskStatus.Pending, activatedAt = clock.now())
        val other = Task.create(Category.Other, estimateMinutes = 2, shelvedAt = clock.now())
            .copy(id = "other", status = TaskStatus.Pending, activatedAt = clock.now())

        // All three are priority 3 → any of them is valid
        engine.processShadowTarget(listOf(exercise, work, other), shadow)
        assertTrue(shadow.currentShadowTaskId in listOf("ex", "work", "other"))
    }
}

class RadarChartEngineTest {

    private fun snapshot(
        date: LocalDate = LocalDate(2026, 5, 31),
        completed: Int = 0,
        scheduled: Int = 0,
        entertainment: Double = 0.0,
        screen: Double = 0.0,
        attempts: Int = 0,
        bypasses: Int = 0,
        health: Double = 50.0,
        steps: Int = 0,
        minutesSlept: Double = 0.0,
        selfAppOpen: Int = 0,
        yestAvg: Double = 0.0,
        todayAvg: Double = 0.0,
    ) = DailyMetricSnapshot(
        date = date,
        tasksCompleted = completed,
        tasksScheduled = scheduled,
        entertainmentMinutes = entertainment,
        totalScreenMinutes = screen,
        blockerAttempts = attempts,
        blockerBypasses = bypasses,
        healthScoreRaw = health,
        steps = steps,
        minutesSlept = minutesSlept,
        selfAppOpen = selfAppOpen,
        yestAvg = yestAvg,
        todayAvg = todayAvg,
        sleepScore = RadarChartEngine.computeSleepScore(minutesSlept),
        stepScore = RadarChartEngine.computeStepScore(steps),
        excessEntertainment = entertainment - 90.0,
        completionRate = if (scheduled > 0) completed.toDouble() / scheduled else 0.0,
    )

    @Test
    fun emptySnapshotsReturnZeros() {
        val scores = RadarChartEngine.computeScores(emptyList())
        assertEquals(0.0, scores.consistency)
        assertEquals(0.0, scores.discipline)
        assertEquals(0.0, scores.focus)
        assertEquals(0.0, scores.health)
        assertEquals(0.0, scores.resilience)
    }

    @Test
    fun consistencyAppClosed() {
        val s = snapshot(completed = 5, scheduled = 5, selfAppOpen = 0)
        val scores = RadarChartEngine.computeScores(listOf(s))
        assertEquals(0.0, scores.consistency)
    }

    @Test
    fun consistencyPerfectScore() {
        val s = snapshot(completed = 5, scheduled = 5, selfAppOpen = 1)
        val scores = RadarChartEngine.computeScores(listOf(s))
        assertEquals(100.0, scores.consistency)
    }

    @Test
    fun consistencyBelowThreshold() {
        val s = snapshot(completed = 4, scheduled = 10, selfAppOpen = 1)
        val scores = RadarChartEngine.computeScores(listOf(s))
        assertEquals(50.0, scores.consistency, 0.01)
    }

    @Test
    fun consistencyNoTasksScheduled() {
        val s = snapshot(completed = 0, scheduled = 0, selfAppOpen = 1)
        val scores = RadarChartEngine.computeScores(listOf(s))
        assertEquals(90.0, scores.consistency)
    }

    @Test
    fun disciplineWithExcessEntertainment() {
        val s = snapshot(entertainment = 300.0, screen = 500.0, selfAppOpen = 1)
        val scores = RadarChartEngine.computeScores(listOf(s))
        assertEquals(49.0, scores.discipline, 0.01)
    }

    @Test
    fun disciplineAtAllowance() {
        val s = snapshot(entertainment = 45.0, screen = 500.0, selfAppOpen = 1)
        val scores = RadarChartEngine.computeScores(listOf(s))
        assertEquals(100.0, scores.discipline, 0.01)
    }

    @Test
    fun disciplineBelowAllowance() {
        val s = snapshot(entertainment = 30.0, screen = 500.0, selfAppOpen = 1)
        val scores = RadarChartEngine.computeScores(listOf(s))
        assertEquals(100.0, scores.discipline, 0.01)
    }

    @Test
    fun focusNoBypasses() {
        val s = snapshot(bypasses = 0, selfAppOpen = 1)
        val scores = RadarChartEngine.computeScores(listOf(s))
        assertEquals(100.0, scores.focus)
    }

    @Test
    fun focusWithBypasses() {
        val s = snapshot(bypasses = 5, selfAppOpen = 1)
        val scores = RadarChartEngine.computeScores(listOf(s))
        assertEquals(60.0, scores.focus)
    }

    @Test
    fun focusCappedAtZero() {
        val s = snapshot(bypasses = 150, selfAppOpen = 1)
        val scores = RadarChartEngine.computeScores(listOf(s))
        assertEquals(0.0, scores.focus)
    }

    @Test
    fun healthFromSleepAndSteps() {
        val s = snapshot(steps = 8000, minutesSlept = 470.0, selfAppOpen = 1)
        val scores = RadarChartEngine.computeScores(listOf(s))
        assertEquals(100.0, scores.health, 0.01)
    }

    @Test
    fun healthPartialSleepAndSteps() {
        val s = snapshot(steps = 4000, minutesSlept = 235.0, selfAppOpen = 1)
        val scores = RadarChartEngine.computeScores(listOf(s))
        assertEquals(50.0, scores.health, 0.01)
    }

    @Test
    fun resilienceFirstDay() {
        val s = snapshot(completed = 5, scheduled = 5, selfAppOpen = 1, yestAvg = 0.0)
        val scores = RadarChartEngine.computeScores(listOf(s))
        assertEquals(100.0, scores.resilience, 0.01)
    }

    @Test
    fun resilienceWithYesterdayAverage() {
        val s = snapshot(
            completed = 5,
            scheduled = 5,
            selfAppOpen = 1,
            yestAvg = 50.0,
        )
        val scores = RadarChartEngine.computeScores(listOf(s))
        // todayAvg = 75 → 75 / (50 + 75) * 100
        assertEquals(60.0, scores.resilience, 0.01)
    }

    @Test
    fun resilienceFirstDayWithoutYesterdayAverage() {
        val s = snapshot(selfAppOpen = 0, bypasses = 100, yestAvg = 0.0)
        val scores = RadarChartEngine.computeScores(listOf(s))
        // todayAvg = 25, yestAvg = 0 → no decline to weigh in → 100
        assertEquals(100.0, scores.resilience)
    }
}

class ShadowRadarEngineTest {

    private fun snapshot(
        date: LocalDate,
        yestAvg: Double = 0.0,
        consistencyList: List<Double> = emptyList(),
        disciplineList: List<Double> = emptyList(),
        focusList: List<Double> = emptyList(),
        healthList: List<Double> = emptyList(),
        resilienceList: List<Double> = emptyList(),
    ) = DailyMetricSnapshot(
        date = date,
        tasksCompleted = 0,
        tasksScheduled = 0,
        entertainmentMinutes = 0.0,
        totalScreenMinutes = 0.0,
        blockerAttempts = 0,
        blockerBypasses = 0,
        healthScoreRaw = 0.0,
        yestAvg = yestAvg,
        consistencyList = consistencyList,
        disciplineList = disciplineList,
        focusList = focusList,
        healthList = healthList,
        resilienceList = resilienceList,
    )

    @Test
    fun emptyListFallsBackToUserScores() {
        val userScores = RadarScores(70.0, 60.0, 80.0, 75.0, 65.0)
        val shadowScores = ShadowRadarEngine.computeShadowScores(
            userScores = userScores,
            snapshots = emptyList(),
            tasks = emptyList(),
            now = Instant.fromEpochMilliseconds(0),
            shadow = ShadowProfile(),
        )
        assertEquals(0.0, shadowScores.consistency)
        assertEquals(60.0, shadowScores.discipline)
        assertEquals(80.0, shadowScores.focus)
        assertEquals(75.0, shadowScores.health)
        // todayAvg = 53.75, no previous history → momentum 100, capped at CEILING
        assertEquals(99.0, shadowScores.resilience, 0.01)
    }

    @Test
    fun topThreeAverageRoundedForPopulatedList() {
        val s = snapshot(
            date = LocalDate(2026, 5, 31),
            disciplineList = listOf(50.0, 60.0, 70.0, 80.0),
        )
        val shadowScores = ShadowRadarEngine.computeShadowScores(
            userScores = RadarScores(0.0, 0.0, 0.0, 0.0, 0.0),
            snapshots = listOf(s),
            tasks = emptyList(),
            now = Instant.fromEpochMilliseconds(0),
            shadow = ShadowProfile(),
        )
        assertEquals(70.0, shadowScores.discipline)
    }

    @Test
    fun shadowConsistencyZeroWithUncompletedTasks() {
        val now = Instant.fromEpochMilliseconds(1000)
        val task = Task.create(Category.Work, shelvedAt = now - 10.hours)
        val shadowScores = ShadowRadarEngine.computeShadowScores(
            userScores = RadarScores(0.0, 0.0, 0.0, 0.0, 0.0),
            snapshots = emptyList(),
            tasks = listOf(task),
            now = now,
            shadow = ShadowProfile(),
        )
        assertEquals(0.0, shadowScores.consistency)
    }

    @Test
    fun resilienceFullWhenNoPreviousHistory() {
        val s = snapshot(
            date = LocalDate(2026, 5, 31),
            disciplineList = listOf(80.0),
            focusList = listOf(60.0),
            healthList = listOf(40.0),
        )
        val shadowScores = ShadowRadarEngine.computeShadowScores(
            userScores = RadarScores(0.0, 0.0, 0.0, 0.0, 0.0),
            snapshots = listOf(s),
            tasks = emptyList(),
            now = Instant.fromEpochMilliseconds(0),
            shadow = ShadowProfile(),
        )
        // todayAvg = 45, no yesterday → momentum 100, capped at CEILING
        assertEquals(99.0, shadowScores.resilience, 0.01)
    }

    @Test
    fun resilienceImprovingBeatsYesterday() {
        val yesterday = snapshot(
            date = LocalDate(2026, 5, 30),
            disciplineList = listOf(40.0),
            focusList = listOf(40.0),
            healthList = listOf(40.0),
        )
        val today = snapshot(
            date = LocalDate(2026, 5, 31),
            disciplineList = listOf(80.0),
            focusList = listOf(80.0),
            healthList = listOf(80.0),
        )
        val shadowScores = ShadowRadarEngine.computeShadowScores(
            userScores = RadarScores(0.0, 0.0, 0.0, 0.0, 0.0),
            snapshots = listOf(yesterday, today),
            tasks = emptyList(),
            now = Instant.fromEpochMilliseconds(0),
            shadow = ShadowProfile(),
        )
        // todayAvg = 60, yestAvg = 30 → 60 / (30 + 60) * 100
        assertEquals(66.67, shadowScores.resilience, 0.01)
    }

    @Test
    fun resilienceDecliningLagsYesterday() {
        val yesterday = snapshot(
            date = LocalDate(2026, 5, 30),
            disciplineList = listOf(80.0),
            focusList = listOf(80.0),
            healthList = listOf(80.0),
        )
        val today = snapshot(
            date = LocalDate(2026, 5, 31),
            disciplineList = listOf(40.0),
            focusList = listOf(40.0),
            healthList = listOf(40.0),
        )
        val shadowScores = ShadowRadarEngine.computeShadowScores(
            userScores = RadarScores(0.0, 0.0, 0.0, 0.0, 0.0),
            snapshots = listOf(yesterday, today),
            tasks = emptyList(),
            now = Instant.fromEpochMilliseconds(0),
            shadow = ShadowProfile(),
        )
        // todayAvg = 30, yestAvg = 60 → 30 / (60 + 30) * 100
        assertEquals(33.33, shadowScores.resilience, 0.01)
    }

    @Test
    fun resilienceFlatDaysIsFifty() {
        val yesterday = snapshot(
            date = LocalDate(2026, 5, 30),
            disciplineList = listOf(60.0),
            focusList = listOf(60.0),
            healthList = listOf(60.0),
        )
        val today = snapshot(
            date = LocalDate(2026, 5, 31),
            disciplineList = listOf(60.0),
            focusList = listOf(60.0),
            healthList = listOf(60.0),
        )
        val shadowScores = ShadowRadarEngine.computeShadowScores(
            userScores = RadarScores(0.0, 0.0, 0.0, 0.0, 0.0),
            snapshots = listOf(yesterday, today),
            tasks = emptyList(),
            now = Instant.fromEpochMilliseconds(0),
            shadow = ShadowProfile(),
        )
        // todayAvg = 45, yestAvg = 45 → 45 / (45 + 45) * 100
        assertEquals(50.0, shadowScores.resilience, 0.01)
    }

    @Test
    fun resilienceZeroWhenTodayAndYesterdayAreZero() {
        val yesterday = snapshot(date = LocalDate(2026, 5, 30))
        val today = snapshot(date = LocalDate(2026, 5, 31))
        val shadowScores = ShadowRadarEngine.computeShadowScores(
            userScores = RadarScores(0.0, 0.0, 0.0, 0.0, 0.0),
            snapshots = listOf(yesterday, today),
            tasks = emptyList(),
            now = Instant.fromEpochMilliseconds(0),
            shadow = ShadowProfile(),
        )
        // todayAvg = 0 and yestAvg = 0 → denominator 0 → 0
        assertEquals(0.0, shadowScores.resilience)
    }

    @Test
    fun shadowConsistencyCountsShadowCompletedTasks() {
        val now = Instant.fromEpochMilliseconds(1000)
        // Task is still Pending for the user, but shadow completed it
        val task = Task.create(Category.Work, shelvedAt = now)
            .copy(id = "shadow-done", status = TaskStatus.Pending)
        val shadow = ShadowProfile(shadowCompletedTaskIds = listOf("shadow-done"))
        val shadowScores = ShadowRadarEngine.computeShadowScores(
            userScores = RadarScores(0.0, 0.0, 0.0, 0.0, 0.0),
            snapshots = emptyList(),
            tasks = listOf(task),
            now = now,
            shadow = shadow,
        )
        // 1 of 1 tasks completed (by shadow) → 100%
        assertEquals(100.0, shadowScores.consistency)
    }
}
