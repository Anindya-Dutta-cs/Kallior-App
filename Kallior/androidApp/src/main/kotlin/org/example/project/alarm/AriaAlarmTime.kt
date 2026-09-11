package org.example.project.alarm

import java.util.Calendar

/**
 * Pure alarm-time helpers (no Android dependencies) so occurrence, repeat-day
 * and countdown logic can be unit-tested on the JVM.
 */
object AriaAlarmTime {

    /**
     * Next occurrence of the given daily time restricted to [repeatDays]
     * (Calendar.DAY_OF_WEEK constants; empty or all-seven means every day),
     * strictly after [nowMillis]. A time equal to "now" rolls forward,
     * matching the legacy scheduler.
     */
    fun nextTriggerAt(
        hour: Int,
        minute: Int,
        repeatDays: Set<Int> = AlarmItem.ALL_DAYS,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val target = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (repeatDays.isEmpty() || repeatDays.size >= 7) {
            return target.timeInMillis
        }

        // The first matching weekday is guaranteed within the next 7 days.
        var candidate = target
        repeat(7) {
            if (candidate.get(Calendar.DAY_OF_WEEK) in repeatDays) {
                return candidate.timeInMillis
            }
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis
    }

    /**
     * Human label for a repeat-day selection: Everyday, Weekdays, Weekends
     * or Custom Days (any other combination, including a single day).
     */
    fun repeatDaysLabel(days: Set<Int>): String = when {
        days.isEmpty() || days.size >= 7 -> "Everyday"
        days == AlarmItem.WEEKDAYS -> "Weekdays"
        days == AlarmItem.WEEKEND -> "Weekends"
        else -> "Custom Days"
    }

    /** Human countdown, e.g. "in 8h 43m". */
    fun formatCountdown(deltaMillis: Long): String {
        if (deltaMillis <= 60_000L) return "in less than a minute"

        val totalMinutes = deltaMillis / 60_000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "in ${hours}h ${minutes}m"
            hours > 0 -> "in ${hours}h"
            else -> "in ${minutes}m"
        }
    }
}
