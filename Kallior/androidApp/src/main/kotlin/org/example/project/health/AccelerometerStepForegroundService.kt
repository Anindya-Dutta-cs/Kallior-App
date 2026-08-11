package org.example.project.health

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder

/**
 * Continuous fallback for devices without TYPE_STEP_COUNTER. Android's
 * TYPE_STEP_DETECTOR is preferred when available; raw accelerometer fusion is
 * the final fallback. Foreground execution prevents background gaps.
 */
class AccelerometerStepForegroundService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var store: AccelerometerStepStore
    private lateinit var detector: AccelerometerStepDetector
    private var activeSensor: Sensor? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        store = AccelerometerStepStore(this)
        detector = AccelerometerStepDetector { store.recordStep() }
        activeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification())
        if (!isFallbackNeeded(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val sensor = activeSensor
        sensorManager.unregisterListener(this)
        val samplingPeriod = if (sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            SensorManager.SENSOR_DELAY_NORMAL
        } else {
            SensorManager.SENSOR_DELAY_GAME
        }
        if (sensor == null || !sensorManager.registerListener(this, sensor, samplingPeriod)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values.firstOrNull()?.let { it > 0f } == true) {
                    store.recordStep()
                }
            }
            Sensor.TYPE_ACCELEROMETER -> detector.onSensorChanged(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Step tracking",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle("Kallior step tracking")
            .setContentText("Using motion sensing because this phone has no step counter")
            .setOngoing(true)
            .build()
    }

    companion object {
        fun startIfNeeded(context: Context) {
            if (!isFallbackNeeded(context)) return
            try {
                context.startForegroundService(
                    Intent(context, AccelerometerStepForegroundService::class.java),
                )
            } catch (_: SecurityException) {
                // Permission may have been revoked between the check and start.
            } catch (_: IllegalStateException) {
                // A background-restricted launch can retry when the app resumes.
            }
        }

        fun isFallbackNeeded(context: Context): Boolean {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) !=
                    PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                ?: return false
            return manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) == null &&
                (
                    manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null ||
                        manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
                )
        }

        private const val CHANNEL_ID = "accelerometer_step_tracking"
        private const val NOTIFICATION_ID = 4_210
    }
}
