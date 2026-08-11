package kallos.data

import kallos.domain.DailyMetricSnapshot
import kallos.domain.RadarScores
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailySnapshotRecorderTest {

    @Test
    fun trimsAt4() {
        val initial = DailyMetricSnapshot(
            date = LocalDate(2024, 1, 1),
            tasksCompleted = 0,
            tasksScheduled = 0,
            entertainmentMinutes = 0.0,
            totalScreenMinutes = 0.0,
            blockerAttempts = 0,
            blockerBypasses = 0,
            healthScoreRaw = 0.0,
            consistencyList = listOf(1.0, 2.0, 3.0, 4.0),
            disciplineList = listOf(1.0, 2.0, 3.0, 4.0),
            focusList = listOf(1.0, 2.0, 3.0, 4.0),
            healthList = listOf(1.0, 2.0, 3.0, 4.0),
            resilienceList = emptyList()
        )
        val scores = RadarScores(5.0, 5.0, 5.0, 5.0, 5.0)

        val result = DailySnapshotRecorder.recordYesterday(listOf(initial), LocalDate(2024, 1, 2), scores)
        val newSnap = result.last()

        assertEquals(4, newSnap.consistencyList.size)
        assertEquals(listOf(2.0, 3.0, 4.0, 5.0), newSnap.consistencyList)
    }

    @Test
    fun idempotentPerDay() {
        val scores = RadarScores(1.0, 1.0, 1.0, 1.0, 1.0)
        val r1 = DailySnapshotRecorder.recordYesterday(emptyList(), LocalDate(2024, 1, 1), scores)
        val r2 = DailySnapshotRecorder.recordYesterday(r1, LocalDate(2024, 1, 1), scores)

        assertEquals(1, r2.size)
        assertEquals(r1, r2)
    }

    @Test
    fun yestAvgMath() {
        val scores = RadarScores(consistency = 100.0, discipline = 50.0, focus = 100.0, health = 50.0, resilience = 90.0)
        val result = DailySnapshotRecorder.recordYesterday(emptyList(), LocalDate(2024, 1, 1), scores)

        val newSnap = result.last()
        assertEquals(75.0, newSnap.yestAvg)
    }
}
