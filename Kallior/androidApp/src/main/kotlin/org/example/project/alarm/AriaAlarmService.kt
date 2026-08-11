package org.example.project.alarm

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import java.io.File

class AriaAlarmService : Service() {

    companion object {
        const val ACTION_START_RINGING = "ACTION_START_RINGING"
        const val ACTION_CONTINUE = "ACTION_CONTINUE"
        const val ACTION_DISMISS = "ACTION_DISMISS"

        private const val PREVIEW_DURATION_MS = 30_000L
        private const val AUTO_STOP_AFTER_PAUSE_MS = 10 * 60 * 1000L
        private const val WAKE_LOCK_TIMEOUT_MS = 15 * 60 * 1000L
    }

    private val repository by lazy { AriaMusicRepository(this) }
    private val handler = Handler(Looper.getMainLooper())

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var usingFallbackSound = false
    private var shortSong = false

    private val previewRunnable = Runnable { pausePreview() }
    private val autoStopRunnable = Runnable { stopRinging() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RINGING -> startRinging()
            ACTION_CONTINUE -> continuePlayback()
            ACTION_DISMISS -> stopRinging()
        }

        return START_REDELIVER_INTENT
    }

    private fun startRinging() {
        acquireWakeLock()

        AriaAlarmPlayback.setPreview(null)
        startForegroundWithCurrentState()

        handler.removeCallbacksAndMessages(null)

        val song = repository.randomSong()

        if (song == null) {
            usingFallbackSound = true
            shortSong = false
            AriaAlarmPlayback.setPreview("System alarm sound")
            prepareFallbackAlarm()
        } else {
            usingFallbackSound = false
            AriaAlarmPlayback.setPreview(song.name)
            prepareSong(song)
        }

        startForegroundWithCurrentState()

        handler.postDelayed(previewRunnable, PREVIEW_DURATION_MS)
        handler.postDelayed(autoStopRunnable, PREVIEW_DURATION_MS + AUTO_STOP_AFTER_PAUSE_MS)
    }

    private fun prepareSong(song: File) {
        releasePlayer()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            setDataSource(song.absolutePath)
            isLooping = false

            setOnPreparedListener { player ->
                player.start()
                shortSong = player.duration in 1 until PREVIEW_DURATION_MS.toInt()
            }

            setOnCompletionListener { player ->
                val state = AriaAlarmPlayback.uiState.value

                if (state.phase == AriaAlarmPlayback.Phase.PREVIEW) {
                    player.seekTo(0)
                    pausePreview()
                } else {
                    stopRinging()
                }
            }

            setOnErrorListener { _, _, _ ->
                stopRinging()
                true
            }

            prepareAsync()
        }
    }

    private fun prepareFallbackAlarm() {
        releasePlayer()

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )

            setDataSource(this@AriaAlarmService, alarmUri)
            isLooping = true

            setOnPreparedListener { player ->
                player.start()
            }

            setOnErrorListener { _, _, _ ->
                stopRinging()
                true
            }

            prepareAsync()
        }
    }

    private fun pausePreview() {
        handler.removeCallbacks(previewRunnable)

        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
        }

        AriaAlarmPlayback.setPausedAfterPreview()
        startForegroundWithCurrentState()
    }

    private fun continuePlayback() {
        handler.removeCallbacks(previewRunnable)
        handler.removeCallbacks(autoStopRunnable)

        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                if (player.duration > 0 && player.currentPosition >= player.duration) {
                    player.seekTo(0)
                }
                player.start()
            }

            if (shortSong || usingFallbackSound) {
                player.isLooping = true
            }
        }

        AriaAlarmPlayback.setFullPlayback()
        startForegroundWithCurrentState()

        handler.postDelayed(autoStopRunnable, AUTO_STOP_AFTER_PAUSE_MS)
    }

    private fun stopRinging() {
        handler.removeCallbacksAndMessages(null)

        releasePlayer()
        AriaAlarmPlayback.reset()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        releaseWakeLock()
    }

    private fun startForegroundWithCurrentState() {
        val notification = AriaAlarmNotification.build(this, AriaAlarmPlayback.uiState.value)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                AriaAlarmNotification.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            ServiceCompat.startForeground(
                this,
                AriaAlarmNotification.NOTIFICATION_ID,
                notification,
                0
            )
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return

        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AriaAlarm::RingWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun releasePlayer() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            } catch (_: Exception) {
            }
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        releasePlayer()
        releaseWakeLock()
        AriaAlarmPlayback.reset()
        super.onDestroy()
    }
}
