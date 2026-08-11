package kallos.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class IosDisciplineScoreTest {

    @Test
    fun zeroMinutesReturnsFullScore() {
        assertEquals(100, calculateIosDisciplineScore(0))
    }

    @Test
    fun allowanceBoundaryReturnsFullScore() {
        // 90 minutes is exactly the daily allowance; no excess.
        assertEquals(100, calculateIosDisciplineScore(90))
    }

    @Test
    fun justBelowAllowanceReturnsFullScore() {
        assertEquals(100, calculateIosDisciplineScore(89))
    }

    @Test
    fun oneHourOverAllowance() {
        // 120 - 90 = 30 excess minutes => 4 points deducted
        assertEquals(96, calculateIosDisciplineScore(120))
    }

    @Test
    fun twoHoursOverAllowance() {
        // 150 - 90 = 60 excess minutes => 8 points deducted
        assertEquals(92, calculateIosDisciplineScore(150))
    }

    @Test
    fun threeHoursOverAllowance() {
        // 180 - 90 = 90 excess minutes => 12 points deducted
        assertEquals(88, calculateIosDisciplineScore(180))
    }

    @Test
    fun fullDayExcessFloorsAtZero() {
        // 1440 - 90 = 1350 excess minutes => deduction would be 180, floor at 0.
        assertEquals(0, calculateIosDisciplineScore(1440))
    }
}
