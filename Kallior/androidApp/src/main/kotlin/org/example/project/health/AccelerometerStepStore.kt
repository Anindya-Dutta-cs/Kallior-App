package org.example.project.health

import android.content.Context
import java.time.LocalDate

/** Persists steps detected by the accelerometer fallback across process restarts. */
internal class AccelerometerStepStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun readToday(): Long {
        val today = LocalDate.now().toString()
        return if (preferences.getString(KEY_DATE, null) == today) {
            preferences.getLong(KEY_STEPS, 0L)
        } else {
            0L
        }
    }

    @Synchronized
    fun recordStep(): Long {
        val today = LocalDate.now().toString()
        val previous = if (preferences.getString(KEY_DATE, null) == today) {
            preferences.getLong(KEY_STEPS, 0L)
        } else {
            0L
        }
        val updated = previous + 1L
        preferences.edit()
            .putString(KEY_DATE, today)
            .putLong(KEY_STEPS, updated)
            .apply()
        return updated
    }

    private companion object {
        const val PREFERENCES_NAME = "accelerometer_step_fallback"
        const val KEY_DATE = "date"
        const val KEY_STEPS = "steps"
    }
}
