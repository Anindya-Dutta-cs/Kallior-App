package org.example.project.health

import android.hardware.SensorEvent
import kotlin.math.sqrt

/**
 * Adaptive orientation-independent peak detector used only when Android exposes
 * neither TYPE_STEP_COUNTER nor TYPE_STEP_DETECTOR.
 *
 * A slow per-axis low-pass filter estimates gravity. Acceleration is projected
 * onto that gravity vector so each gait cycle has one positive vertical peak;
 * adaptive thresholding, hysteresis, and cadence limits reject noise.
 */
internal class AccelerometerStepDetector(
    private val onStep: () -> Unit,
) {
    private val gravity = FloatArray(3)
    private var initialized = false
    private var filteredAcceleration = 0f
    private var olderSample = 0f
    private var previousSample = 0f
    private var previousTimestampMillis = 0L
    private var adaptiveThreshold = INITIAL_THRESHOLD
    private var armed = true
    private var lastStepTimestampMillis = 0L

    fun onSensorChanged(event: SensorEvent) {
        onAcceleration(
            x = event.values[0],
            y = event.values[1],
            z = event.values[2],
            timestampNanos = event.timestamp,
        )
    }

    internal fun onAcceleration(x: Float, y: Float, z: Float, timestampNanos: Long) {
        if (!initialized) {
            gravity[0] = x
            gravity[1] = y
            gravity[2] = z
            initialized = true
            return
        }

        gravity[0] = GRAVITY_FILTER_ALPHA * gravity[0] + (1f - GRAVITY_FILTER_ALPHA) * x
        gravity[1] = GRAVITY_FILTER_ALPHA * gravity[1] + (1f - GRAVITY_FILTER_ALPHA) * y
        gravity[2] = GRAVITY_FILTER_ALPHA * gravity[2] + (1f - GRAVITY_FILTER_ALPHA) * z

        val linearX = x - gravity[0]
        val linearY = y - gravity[1]
        val linearZ = z - gravity[2]
        val gravityNorm = sqrt(
            gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2],
        ).coerceAtLeast(MINIMUM_GRAVITY_NORM)
        val verticalAcceleration =
            (linearX * gravity[0] + linearY * gravity[1] + linearZ * gravity[2]) / gravityNorm
        filteredAcceleration =
            SMOOTHING_ALPHA * filteredAcceleration +
                (1f - SMOOTHING_ALPHA) * verticalAcceleration

        val timestampMillis = timestampNanos / NANOS_PER_MILLISECOND
        val peak = previousSample > olderSample && previousSample >= filteredAcceleration
        if (peak && previousSample >= MINIMUM_PEAK_TO_ADAPT) {
            adaptiveThreshold = (
                THRESHOLD_ADAPTATION * adaptiveThreshold +
                    (1f - THRESHOLD_ADAPTATION) * previousSample * PEAK_THRESHOLD_RATIO
                ).coerceIn(MINIMUM_THRESHOLD, MAXIMUM_THRESHOLD)

            if (
                armed &&
                previousSample >= adaptiveThreshold &&
                previousTimestampMillis - lastStepTimestampMillis >= MIN_STEP_INTERVAL_MILLIS
            ) {
                lastStepTimestampMillis = previousTimestampMillis
                armed = false
                onStep()
            }
        }

        if (filteredAcceleration <= adaptiveThreshold * REARM_RATIO) {
            armed = true
        }

        olderSample = previousSample
        previousSample = filteredAcceleration
        previousTimestampMillis = timestampMillis
    }

    private companion object {
        // A long gravity time constant preserves the faster 1–3 Hz walking signal.
        const val GRAVITY_FILTER_ALPHA = 0.965f
        const val SMOOTHING_ALPHA = 0.25f
        const val INITIAL_THRESHOLD = 0.40f
        const val MINIMUM_THRESHOLD = 0.24f
        const val MAXIMUM_THRESHOLD = 1.40f
        const val MINIMUM_PEAK_TO_ADAPT = 0.18f
        const val THRESHOLD_ADAPTATION = 0.82f
        const val PEAK_THRESHOLD_RATIO = 0.52f
        const val REARM_RATIO = 0.72f
        const val MIN_STEP_INTERVAL_MILLIS = 260L
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val MINIMUM_GRAVITY_NORM = 0.1f
    }
}
