package org.example.project

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppBlockerForegroundService : Service() {

    private val CHANNEL_ID = "AppBlockerServiceChannel"
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var overlayManager: OverlayManager
    private lateinit var blockerRepository: BlockerRepository
    private lateinit var appBlockerController: AppBlockerControllerImpl
    private var blockedApps = setOf<String>()
    private var allowUntil = mapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        blockerRepository = BlockerRepository(this)
        appBlockerController = AppBlockerControllerImpl(
            this,
            blockerRepository,
            WebsiteBlockerRepository(this),
        )
        overlayManager = OverlayManager(this, blockerRepository, appBlockerController)
        createNotificationChannel()

        scope.launch {
            blockerRepository.blockedAppsFlow.collect { apps ->
                blockedApps = apps
            }
        }
        scope.launch {
            blockerRepository.allowUntilFlow.collect { map ->
                allowUntil = map
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Kallior Focus Fortress")
            .setContentText("Monitoring for blocked apps")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()

        startForeground(1, notification)
        startMonitoring()

        return START_STICKY
    }

    private fun startMonitoring() {
        serviceJob?.cancel()
        serviceJob = scope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            while (isActive) {
                val endTime = System.currentTimeMillis()
                val startTime = endTime - 1000 * 10 // check last 10 seconds

                val events = usageStatsManager.queryEvents(startTime, endTime)
                val event = UsageEvents.Event()
                var latestResumedPackage: String? = null

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        latestResumedPackage = event.packageName
                    }
                }

                if (latestResumedPackage != null) {
                    launch(Dispatchers.Main) {
                        overlayManager.onAppSwitched(latestResumedPackage)

                        val temporarilyAllowed = allowUntil[latestResumedPackage]
                            ?.let { it > System.currentTimeMillis() } ?: false

                        if (blockedApps.contains(latestResumedPackage) && !temporarilyAllowed) {
                            if (!overlayManager.showOverlay(latestResumedPackage)) {
                                // Only give up permanently if the permission itself is gone.
                                // A one-off BadTokenException/SecurityException should not kill
                                // monitoring for the rest of the session.
                                val permManager = PermissionManager(this@AppBlockerForegroundService)
                                if (!permManager.hasOverlayPermission()) {
                                    Log.w("BlockerDebug", "Overlay permission revoked — stopping service")
                                    stopSelf()
                                } else {
                                    Log.w("BlockerDebug", "Overlay show failed transiently — will retry next cycle")
                                }
                            }
                        }
                    }
                }

                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        overlayManager.hideOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Focus Fortress Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }
}
