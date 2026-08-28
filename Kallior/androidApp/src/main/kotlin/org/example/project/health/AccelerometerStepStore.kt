package org.example.project.health

import android.content.Context
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

/** Persists steps detected by the accelerometer fallback across process restarts. */
internal class AccelerometerStepStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    private val pendingSteps = AtomicLong(0L)

    @Synchronized
    fun readToday(): Long {
        val today = LocalDate.now().toString()
        val persisted = if (preferences.getString(KEY_DATE, null) == today) {
            preferences.getLong(KEY_STEPS, 0L)
        } else {
            0L
        }
        return persisted + pendingSteps.get()
    }

    @Synchronized
    fun recordStep(): Long {
        val pending = pendingSteps.incrementAndGet()
        if (pending >= 10) {
            flush()
        }
        return readToday()
    }

    @Synchronized
    fun flush() {
        val pending = pendingSteps.getAndSet(0L)
        if (pending > 0) {
            val today = LocalDate.now().toString()
            val previous = if (preferences.getString(KEY_DATE, null) == today) {
                preferences.getLong(KEY_STEPS, 0L)
            } else {
                0L
            }
            preferences.edit()
                .putString(KEY_DATE, today)
                .putLong(KEY_STEPS, previous + pending)
                .apply()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "accelerometer_step_fallback"
        const val KEY_DATE = "date"
        const val KEY_STEPS = "steps"
    }
}
