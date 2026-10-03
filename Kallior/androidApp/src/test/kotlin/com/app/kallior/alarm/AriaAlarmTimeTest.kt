package com.app.kallior.alarm

import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals

class AriaAlarmTimeTest {

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun dayOfWeek(year: Int, month: Int, day: Int): Int =
        Calendar.getInstance().apply { set(year, month, day) }.get(Calendar.DAY_OF_WEEK)

    @Test
    fun `next occurrence is later today when the time has not passed`() {
        val now = millis(2026, Calendar.SEPTEMBER, 10, 7, 0)
        val next = AriaAlarmTime.nextTriggerAt(9, 30, nowMillis = now)
        assertEquals(millis(2026, Calendar.SEPTEMBER, 10, 9, 30), next)
    }

    @Test
    fun `next occurrence is tomorrow when the time has passed`() {
        val now = millis(2026, Calendar.SEPTEMBER, 10, 7, 0)
        val next = AriaAlarmTime.nextTriggerAt(6, 25, nowMillis = now)
        assertEquals(millis(2026, Calendar.SEPTEMBER, 11, 6, 25), next)
    }

    @Test
    fun `an exact match rolls to tomorrow`() {
        val now = millis(2026, Calendar.SEPTEMBER, 10, 7, 0)
        val next = AriaAlarmTime.nextTriggerAt(7, 0, nowMillis = now)
        assertEquals(millis(2026, Calendar.SEPTEMBER, 11, 7, 0), next)
    }

    @Test
    fun `empty repeat days behave like every day`() {
        val now = millis(2026, Calendar.SEPTEMBER, 10, 7, 0)
        val next = AriaAlarmTime.nextTriggerAt(6, 25, repeatDays = emptySet(), nowMillis = now)
        assertEquals(millis(2026, Calendar.SEPTEMBER, 11, 6, 25), next)
    }

    @Test
    fun `single-day alarm jumps to the next matching weekday`() {
        // 2026-09-10 is a Thursday; the next Monday is 2026-09-14.
        assertEquals(Calendar.THURSDAY, dayOfWeek(2026, Calendar.SEPTEMBER, 10))
        val now = millis(2026, Calendar.SEPTEMBER, 10, 7, 0)
        val next = AriaAlarmTime.nextTriggerAt(9, 30, setOf(Calendar.MONDAY), now)
        assertEquals(millis(2026, Calendar.SEPTEMBER, 14, 9, 30), next)
    }

    @Test
    fun `single-day alarm on its own day rolls one week when time has passed`() {
        // 2026-09-11 is a Friday; a Friday 06:25 alarm after 07:00 rings next Friday.
        assertEquals(Calendar.FRIDAY, dayOfWeek(2026, Calendar.SEPTEMBER, 11))
        val now = millis(2026, Calendar.SEPTEMBER, 11, 7, 0)
        val next = AriaAlarmTime.nextTriggerAt(6, 25, setOf(Calendar.FRIDAY), now)
        assertEquals(millis(2026, Calendar.SEPTEMBER, 18, 6, 25), next)
    }

    @Test
    fun `weekend alarm from midweek fires on the coming Saturday`() {
        // 2026-09-08 is a Tuesday; the coming Saturday is 2026-09-12.
        assertEquals(Calendar.TUESDAY, dayOfWeek(2026, Calendar.SEPTEMBER, 8))
        val now = millis(2026, Calendar.SEPTEMBER, 8, 12, 0)
        val next = AriaAlarmTime.nextTriggerAt(8, 0, AlarmItem.WEEKEND, now)
        assertEquals(millis(2026, Calendar.SEPTEMBER, 12, 8, 0), next)
    }

    @Test
    fun `countdown formats hours and minutes`() {
        assertEquals("in 8h 43m", AriaAlarmTime.formatCountdown((8 * 60 + 43) * 60_000L))
        assertEquals("in 8h", AriaAlarmTime.formatCountdown(8 * 60 * 60_000L))
        assertEquals("in 43m", AriaAlarmTime.formatCountdown(43 * 60_000L))
        assertEquals("in less than a minute", AriaAlarmTime.formatCountdown(59_000L))
    }

    @Test
    fun `repeat day labels follow the selection`() {
        assertEquals("Everyday", AriaAlarmTime.repeatDaysLabel(AlarmItem.ALL_DAYS))
        assertEquals("Everyday", AriaAlarmTime.repeatDaysLabel(emptySet()))
        assertEquals("Weekdays", AriaAlarmTime.repeatDaysLabel(AlarmItem.WEEKDAYS))
        assertEquals("Weekends", AriaAlarmTime.repeatDaysLabel(AlarmItem.WEEKEND))
        assertEquals("Custom Days", AriaAlarmTime.repeatDaysLabel(setOf(Calendar.MONDAY)))
        assertEquals(
            "Custom Days",
            AriaAlarmTime.repeatDaysLabel(setOf(Calendar.MONDAY, Calendar.SATURDAY))
        )
        assertEquals(
            "Custom Days",
            AriaAlarmTime.repeatDaysLabel(AlarmItem.WEEKDAYS + Calendar.SUNDAY)
        )
    }
}
