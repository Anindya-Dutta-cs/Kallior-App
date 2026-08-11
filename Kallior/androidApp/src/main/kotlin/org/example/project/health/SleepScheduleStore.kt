package org.example.project.health

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sleepScheduleDataStore by preferencesDataStore(
    name = "sleep_schedule"
)

/**
 * User-provided "I usually sleep at X and wake at Y" schedule.
 * Used as a fallback when Health Connect sleep data is unavailable.
 */
data class SleepSchedule(
    val sleepHour: Int,
    val sleepMinute: Int,
    val wakeHour: Int,
    val wakeMinute: Int,
) {
    /** Minutes since midnight for the sleep time. */
    val sleepMinutesOfDay: Int get() = sleepHour * 60 + sleepMinute

    /** Minutes since midnight for the wake time. */
    val wakeMinutesOfDay: Int get() = wakeHour * 60 + wakeMinute

    /** Expected sleep duration in hours (handles midnight-crossing). */
    fun durationHours(): Double {
        val sleepMin = sleepMinutesOfDay
        val wakeMin = wakeMinutesOfDay
        val totalMinutes = if (wakeMin > sleepMin) {
            wakeMin - sleepMin
        } else {
            (24 * 60 - sleepMin) + wakeMin
        }
        return totalMinutes / 60.0
    }
}

/**
 * DataStore-backed persistence for the user's self-reported sleep schedule.
 * This is the fallback when Health Connect sleep sessions are not available.
 */
class SleepScheduleStore(private val context: Context) {

    private val hasScheduleKey = booleanPreferencesKey("has_sleep_schedule")
    private val sleepHourKey = intPreferencesKey("sleep_hour")
    private val sleepMinuteKey = intPreferencesKey("sleep_minute")
    private val wakeHourKey = intPreferencesKey("wake_hour")
    private val wakeMinuteKey = intPreferencesKey("wake_minute")

    val scheduleFlow: Flow<SleepSchedule?> =
        context.sleepScheduleDataStore.data.map { prefs ->
            val hasSchedule = prefs[hasScheduleKey] ?: false
            if (!hasSchedule) return@map null
            SleepSchedule(
                sleepHour = prefs[sleepHourKey] ?: 23,
                sleepMinute = prefs[sleepMinuteKey] ?: 0,
                wakeHour = prefs[wakeHourKey] ?: 7,
                wakeMinute = prefs[wakeMinuteKey] ?: 0,
            )
        }

    suspend fun currentSchedule(): SleepSchedule? = scheduleFlow.first()

    suspend fun saveSchedule(schedule: SleepSchedule) {
        context.sleepScheduleDataStore.edit { prefs ->
            prefs[hasScheduleKey] = true
            prefs[sleepHourKey] = schedule.sleepHour
            prefs[sleepMinuteKey] = schedule.sleepMinute
            prefs[wakeHourKey] = schedule.wakeHour
            prefs[wakeMinuteKey] = schedule.wakeMinute
        }
        SleepTrackingScheduler.schedule(context, schedule)
    }

    suspend fun hasSchedule(): Boolean = scheduleFlow.first() != null
}
