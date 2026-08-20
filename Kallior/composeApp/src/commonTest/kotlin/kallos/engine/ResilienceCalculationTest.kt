package kallos.engine

import kallos.data.DailySnapshotRecorder
import kallos.domain.DailyMetricSnapshot
import kallos.domain.RadarScores
import kallos.domain.ShadowProfile
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResilienceCalculationTest {

    @Test
    fun testUserResilienceUpdatesWhenOtherFieldsChange() {
        // Initial state
        val s1 = DailyMetricSnapshot(
            date = LocalDate(2026, 6, 1),
            tasksCompleted = 5,
            tasksScheduled = 5,
            completionRate = 1.0,
            entertainmentMinutes = 0.0,
            totalScreenMinutes = 60.0,
            blockerAttempts = 0,
            blockerBypasses = 0,
            healthScoreRaw = 0.0,
            steps = 8000, // 100% health steps
            minutesSlept = 470.0, // 100% health sleep
            selfAppOpen = 1,
            yestAvg = 50.0,
        )
        val initialScores = RadarChartEngine.computeScores(listOf(s1))
        // consistency = 100, discipline = 100, focus = 100, health = 100 -> todayAvg = 100.0
        // resilience = 100 / (50 + 100) * 100 = 66.67
        assertEquals(100.0, initialScores.consistency)
        assertEquals(100.0, initialScores.discipline)
        assertEquals(100.0, initialScores.focus)
        assertEquals(100.0, initialScores.health)
        assertEquals(66.67, initialScores.resilience, 0.01)

        // When user incurs bypasses (focus drops), resilience updates in real time
        val s2 = s1.copy(blockerBypasses = 5) // focus drops by 40 -> 60.0
        val updatedScores = RadarChartEngine.computeScores(listOf(s2))
        assertEquals(60.0, updatedScores.focus)
        // todayAvg = (100 + 100 + 60 + 100) / 4.0 = 90.0
        // resilience = 90 / (50 + 90) * 100 = 64.2857
        assertEquals(64.29, updatedScores.resilience, 0.01)
    }

    @Test
    fun testDailySnapshotRecorderStoresBothTodayAvgAndYestAvg() {
        val yesterdayDate = LocalDate(2026, 6, 1)
        val scores = RadarScores(
            consistency = 80.0,
            discipline = 60.0,
            focus = 100.0,
            health = 80.0,
            resilience = 75.0,
        )
        // 4-axis avg = (80 + 60 + 100 + 80) / 4 = 80.0
        val snapshots = DailySnapshotRecorder.recordYesterday(emptyList(), yesterdayDate, scores)
        val recorded = snapshots.single()

        assertEquals(80.0, recorded.yestAvg)
        assertEquals(80.0, recorded.todayAvg)
    }

    @Test
    fun testShadowResilienceUpdatesRealTimeWithPreviousHistory() {
        val yesterday = DailyMetricSnapshot(
            date = LocalDate(2026, 6, 1),
            tasksCompleted = 0,
            tasksScheduled = 0,
            entertainmentMinutes = 0.0,
            totalScreenMinutes = 0.0,
            blockerAttempts = 0,
            blockerBypasses = 0,
            healthScoreRaw = 0.0,
            disciplineList = listOf(50.0),
            focusList = listOf(50.0),
            healthList = listOf(50.0),
        )
        val today = DailyMetricSnapshot(
            date = LocalDate(2026, 6, 2),
            tasksCompleted = 0,
            tasksScheduled = 0,
            entertainmentMinutes = 0.0,
            totalScreenMinutes = 0.0,
            blockerAttempts = 0,
            blockerBypasses = 0,
            healthScoreRaw = 0.0,
            disciplineList = listOf(50.0, 70.0),
            focusList = listOf(50.0, 70.0),
            healthList = listOf(50.0, 70.0),
        )

        val userScores = RadarScores(consistency = 50.0, discipline = 70.0, focus = 70.0, health = 70.0, resilience = 60.0)
        val shadowScores1 = ShadowRadarEngine.computeShadowScores(
            userScores = userScores,
            snapshots = listOf(yesterday, today),
            tasks = emptyList(),
            now = kotlin.time.Instant.fromEpochMilliseconds(0),
            shadow = ShadowProfile(),
        )

        // yestAvg: consistency=0, discipline=50, focus=50, health=50 -> (0+50+50+50)/4 = 37.5
        // todayAvg: consistency=0, discipline=57 (top 3 avg of [50, 50, 70]), focus=57, health=57 -> (0+57+57+57)/4 = 42.75
        // momentum: 42.75 / (37.5 + 42.75) * 100 = 53.27
        assertEquals(53.27, shadowScores1.resilience, 0.01)

        // When today's discipline increases in real time:
        val todayUpdated = today.copy(
            disciplineList = listOf(50.0, 90.0),
            focusList = listOf(50.0, 90.0),
            healthList = listOf(50.0, 90.0),
        )
        val shadowScores2 = ShadowRadarEngine.computeShadowScores(
            userScores = userScores.copy(discipline = 90.0, focus = 90.0, health = 90.0),
            snapshots = listOf(yesterday, todayUpdated),
            tasks = emptyList(),
            now = kotlin.time.Instant.fromEpochMilliseconds(0),
            shadow = ShadowProfile(),
        )
        // today's average of [50, 50, 90] = 63.33 -> roundToInt = 63.0 for discipline, focus, health
        // todayAvg = (0 + 63 + 63 + 63) / 4.0 = 47.25
        // resilience = 47.25 / (37.5 + 47.25) * 100 = 55.75
        assertEquals(55.75, shadowScores2.resilience, 0.01)
        assertTrue(shadowScores2.resilience > shadowScores1.resilience)
    }
}
