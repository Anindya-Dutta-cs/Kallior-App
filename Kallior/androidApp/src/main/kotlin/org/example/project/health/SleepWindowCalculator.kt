package org.example.project.health

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * A concrete sleep window: the time span the user is expected to be asleep.
 * The [anchorDate] is the date the user wakes up on (the "sleep date").
 */
data class SleepWindow(
    val anchorDate: LocalDate,
    val start: Instant,
    val end: Instant,
) {
    fun contains(instant: Instant): Boolean =
        !instant.isBefore(start) && instant.isBefore(end)

    fun durationHours(): Double =
        Duration.between(start, end).seconds / 3600.0
}

/**
 * Given a [SleepSchedule], computes the concrete [SleepWindow] that is
 * current or most recently ended relative to [now].
 *
 * Handles the common case where the sleep time is in the evening and the
 * wake time is the next morning (i.e. the window crosses midnight).
 */
class SleepWindowCalculator(
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    /**
     * Returns the sleep window that [now] falls inside, or the most recent
     * past window if [now] is outside any sleep window.
     */
    fun currentOrMostRecentSleepWindow(
        now: Instant,
        schedule: SleepSchedule,
    ): SleepWindow {
        val sleepTime = LocalTime.of(schedule.sleepHour, schedule.sleepMinute)
        val wakeTime = LocalTime.of(schedule.wakeHour, schedule.wakeMinute)
        val today = now.atZone(zone).toLocalDate()
        val crossesMidnight = sleepTime >= wakeTime

        return if (crossesMidnight) {
            // e.g. Sleep 23:00, Wake 07:00
            // Two candidate windows relative to today:
            //   Window A: starts yesterday at sleepTime, ends today at wakeTime
            //   Window B: starts today at sleepTime, ends tomorrow at wakeTime
            val windowAStart = today.minusDays(1).atTime(sleepTime).atZone(zone).toInstant()
            val windowAEnd = today.atTime(wakeTime).atZone(zone).toInstant()
            val windowBStart = today.atTime(sleepTime).atZone(zone).toInstant()
            val windowBEnd = today.plusDays(1).atTime(wakeTime).atZone(zone).toInstant()

            when {
                now.isBefore(windowAEnd) -> SleepWindow(today, windowAStart, windowAEnd)
                now.isBefore(windowBStart) -> SleepWindow(today, windowAStart, windowAEnd)
                else -> SleepWindow(today.plusDays(1), windowBStart, windowBEnd)
            }
        } else {
            // Same-day window, e.g. Sleep 01:00, Wake 09:00
            val startToday = today.atTime(sleepTime).atZone(zone).toInstant()
            val endToday = today.atTime(wakeTime).atZone(zone).toInstant()

            if (now.isBefore(startToday)) {
                // Before today's window — return yesterday's
                val yesterday = today.minusDays(1)
                SleepWindow(
                    yesterday,
                    yesterday.atTime(sleepTime).atZone(zone).toInstant(),
                    yesterday.atTime(wakeTime).atZone(zone).toInstant(),
                )
            } else {
                SleepWindow(today, startToday, endToday)
            }
        }
    }

    /** Returns the active window, or the next upcoming sleep window. */
    fun currentOrNextSleepWindow(now: Instant, schedule: SleepSchedule): SleepWindow {
        val currentOrRecent = currentOrMostRecentSleepWindow(now, schedule)
        if (currentOrRecent.contains(now) || currentOrRecent.start.isAfter(now)) {
            return currentOrRecent
        }

        val sleepTime = LocalTime.of(schedule.sleepHour, schedule.sleepMinute)
        val wakeTime = LocalTime.of(schedule.wakeHour, schedule.wakeMinute)
        val today = now.atZone(zone).toLocalDate()
        val startToday = today.atTime(sleepTime).atZone(zone).toInstant()
        val startDate = if (startToday.isAfter(now)) today else today.plusDays(1)
        val crossesMidnight = sleepTime >= wakeTime
        val endDate = if (crossesMidnight) startDate.plusDays(1) else startDate
        return SleepWindow(
            anchorDate = endDate,
            start = startDate.atTime(sleepTime).atZone(zone).toInstant(),
            end = endDate.atTime(wakeTime).atZone(zone).toInstant(),
        )
    }
}
