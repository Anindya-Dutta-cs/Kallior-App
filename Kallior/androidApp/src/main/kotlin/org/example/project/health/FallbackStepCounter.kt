package org.example.project.health

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import kotlin.coroutines.resume

private val Context.fallbackStepDataStore by preferencesDataStore(name = "step_counter_fallback")

/**
 * Fallback step reader that prefers [Sensor.TYPE_STEP_COUNTER] and reads the
 * persisted step-detector/accelerometer total when the counter is absent.
 *
 * This sensor is available on most Android devices but has important caveats:
 * - It returns **cumulative** steps since the last device reboot.
 * - A baseline must be persisted and subtracted each time.
 * - The sensor resets on reboot.
 * - It may not be present on all devices.
 *
 * This fallback is only used when Health Connect is unavailable or returns
 * zero steps for the current day.
 */
class FallbackStepCounter(private val context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    /**
     * Read today's step count from dedicated hardware or the persisted
     * continuous fallback, or `null` when permission is unavailable.
     *
     * Persists a baseline (sensor value at first read of the day) so that
     * subsequent readings return the delta since the start of the day.
     */
    suspend fun readStepsForToday(): Long? {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            ?: return AccelerometerStepStore(context).readToday()

        // A sensor registration can fail or never deliver an event (for example on
        // emulators and devices without activity-recognition access). Never let that
        // stall a caller waiting to render the app.
        val cumulative = withTimeoutOrNull(SENSOR_READ_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine<Float?> { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sensorManager.unregisterListener(this)
                        if (continuation.isActive) {
                            continuation.resume(event.values[0])
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
                }

                val registered = sensorManager.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                ) == true
                if (!registered && continuation.isActive) {
                    continuation.resume(null)
                }
                continuation.invokeOnCancellation {
                    sensorManager.unregisterListener(listener)
                }
            }
        } ?: return null

        val rawSteps = cumulative.toLong()
        val todayKey = LocalDate.now().toString()

        val prefs = context.fallbackStepDataStore.data.first()
        val savedDate = prefs[stringPreferencesKey("date")] ?: ""
        val savedBaseline = prefs[longPreferencesKey("baseline")] ?: 0L

        // The counter resets to zero after a reboot. Treat a lower raw value as
        // a new baseline; otherwise the fallback would remain at zero until the
        // device accumulated enough post-reboot steps to exceed the old value.
        if (savedDate == todayKey && savedBaseline > 0L && rawSteps >= savedBaseline) {
            return rawSteps - savedBaseline
        }

        // A step counter has no history, so on its first observation of a day
        // the only correct baseline is the current cumulative sensor value.
        context.fallbackStepDataStore.edit { p ->
            p[stringPreferencesKey("date")] = todayKey
            p[longPreferencesKey("baseline")] = rawSteps
        }
        return 0L
    }

    private companion object {
        const val SENSOR_READ_TIMEOUT_MILLIS = 2_000L
    }
}
