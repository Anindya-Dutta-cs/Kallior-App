package org.example.project.health

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccelerometerStepDetectorTest {
    @Test
    fun countsAStableWalkingCadence() {
        var steps = 0
        val detector = AccelerometerStepDetector { steps += 1 }
        val sampleRateHz = 50
        val durationSeconds = 120
        val cadenceHz = 2.0

        for (sample in 0..durationSeconds * sampleRateHz) {
            val seconds = sample.toDouble() / sampleRateHz
            val verticalMotion = (1.4 * sin(2.0 * PI * cadenceHz * seconds)).toFloat()
            detector.onAcceleration(
                x = 0f,
                y = 0f,
                z = 9.81f + verticalMotion,
                timestampNanos = sample * 20_000_000L,
            )
        }

        assertTrue(steps in 235..241, "Expected about 240 steps, counted $steps")
    }

    @Test
    fun ignoresStationarySensorNoise() {
        var steps = 0
        val detector = AccelerometerStepDetector { steps += 1 }

        for (sample in 0..3_000) {
            val seconds = sample / 50.0
            val noise = (0.06 * sin(2.0 * PI * 3.0 * seconds)).toFloat()
            detector.onAcceleration(
                x = noise,
                y = 0f,
                z = 9.81f,
                timestampNanos = sample * 20_000_000L,
            )
        }

        assertEquals(0, steps)
    }
}
