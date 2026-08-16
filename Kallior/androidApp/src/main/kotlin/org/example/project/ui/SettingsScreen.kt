package org.example.project.ui

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kallos.platform.PlatformDataFetcher
import kallos.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import org.example.project.health.AccelerometerStepForegroundService
import org.example.project.health.HealthDependencies
import org.example.project.health.SleepSchedule
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding

/**
 * Settings screen with a "Health & Sleep" section that exposes:
 * - Health Connect permission status + request
 * - Usage Stats permission status + request
 * - Sleep schedule editor (reuses [SleepScheduleDialog])
 */
@Composable
fun SettingsScreen(
    gameViewModel: GameViewModel,
) {
    val context = LocalContext.current
    val permissionHelper = remember { HealthDependencies.healthConnectPermissionHelper(context) }
    val permissionManager = remember { org.example.project.PermissionManager(context) }
    val scheduleStore = remember { HealthDependencies.sleepScheduleStore(context) }
    val scope = rememberCoroutineScope()

    var healthConnectAvailable by remember { mutableStateOf(false) }
    var healthPermissionsGranted by remember { mutableStateOf(false) }
    var currentSchedule by remember { mutableStateOf<SleepSchedule?>(null) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var hasUsageStats by remember { mutableStateOf(false) }
    var stepCounterAvailable by remember { mutableStateOf(false) }
    var stepDetectorAvailable by remember { mutableStateOf(false) }
    var accelerometerAvailable by remember { mutableStateOf(false) }
    var activityRecognitionGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) }

    // Launcher for Health Connect permission dialog.
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        healthPermissionsGranted = granted.containsAll(
            org.example.project.health.HealthConnectPermissionHelper.REQUIRED_PERMISSIONS,
        )
    }

    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        activityRecognitionGranted = granted
        if (granted) {
            AccelerometerStepForegroundService.startIfNeeded(context)
        }
    }

    // Re-check permission state each time the screen resumes.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    healthConnectAvailable = permissionHelper.isHealthConnectAvailable()
                    healthPermissionsGranted = permissionHelper.hasAllPermissions()
                    currentSchedule = scheduleStore.currentSchedule()
                    hasUsageStats = permissionManager.hasUsageStatsPermission()
                    stepCounterAvailable = hasSensor(context, Sensor.TYPE_STEP_COUNTER)
                    stepDetectorAvailable = hasSensor(context, Sensor.TYPE_STEP_DETECTOR)
                    accelerometerAvailable = hasSensor(context, Sensor.TYPE_ACCELEROMETER)
                    activityRecognitionGranted = hasActivityRecognitionPermission(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Initial load.
    LaunchedEffect(Unit) {
        healthConnectAvailable = permissionHelper.isHealthConnectAvailable()
        healthPermissionsGranted = permissionHelper.hasAllPermissions()
        currentSchedule = scheduleStore.currentSchedule()
        hasUsageStats = permissionManager.hasUsageStatsPermission()
        stepCounterAvailable = hasSensor(context, Sensor.TYPE_STEP_COUNTER)
        stepDetectorAvailable = hasSensor(context, Sensor.TYPE_STEP_DETECTOR)
        accelerometerAvailable = hasSensor(context, Sensor.TYPE_ACCELEROMETER)
        activityRecognitionGranted = hasActivityRecognitionPermission(context)
    }

    // Request the runtime permission in the health settings context. Android
    // Q+ gates motion-based step tracking behind ACTIVITY_RECOGNITION.
    LaunchedEffect(
        stepCounterAvailable,
        stepDetectorAvailable,
        accelerometerAvailable,
        activityRecognitionGranted,
    ) {
        val stepTrackingAvailable =
            stepCounterAvailable || stepDetectorAvailable || accelerometerAvailable
        if (
            stepTrackingAvailable &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !activityRecognitionGranted
        ) {
            activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else if (stepTrackingAvailable && activityRecognitionGranted) {
            AccelerometerStepForegroundService.startIfNeeded(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KalliorColors.SecondaryBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // Top bar spacing preserved
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = KalliorColors.NormalText,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // ── Health & Sleep section ─────────────────────────────────
            Text(
                text = "Health & Sleep",
                style = MaterialTheme.typography.headlineSmall,
                color = KalliorColors.NormalText,
            )

            val snapshot = gameViewModel.todaySnapshot
            val steps = snapshot?.steps ?: 0
            val sleepMin = snapshot?.minutesSlept ?: 0.0
            val sleepHours = (sleepMin / 60).toInt()
            val sleepRemainderMin = (sleepMin % 60).toInt()

            SettingsRow(
                label = "Today's Steps",
                status = "$steps steps",
                onClick = {}
            )

            SettingsRow(
                label = "Time Slept",
                status = "${sleepHours}h ${sleepRemainderMin}m",
                onClick = {}
            )

            // Health Connect status row
            SettingsRow(
                label = "Health Connect",
                status = when {
                    !healthConnectAvailable -> "⚠️ Not available"
                    healthPermissionsGranted -> "✅ Connected"
                    else -> "🔒 Permissions needed"
                },
                onClick = {
                    scope.launch {
                        if (healthConnectAvailable && !healthPermissionsGranted) {
                            healthPermissionLauncher.launch(
                                permissionHelper.requiredPermissionSet()
                            )
                        }
                    }
                },
            )

            SettingsRow(
                label = "Step sensor access",
                status = when {
                    !stepCounterAvailable && !stepDetectorAvailable && !accelerometerAvailable ->
                        "⚠️ Not supported"
                    !activityRecognitionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                        "🔒 Permission needed"
                    stepCounterAvailable -> "✅ Hardware counter"
                    stepDetectorAvailable -> "✅ Android step detector"
                    else -> "✅ Adaptive accelerometer"
                },
                onClick = {
                    if (
                        (stepCounterAvailable || stepDetectorAvailable || accelerometerAvailable) &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        !activityRecognitionGranted
                    ) {
                        activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                },
            )

            // Usage Stats status row
            SettingsRow(
                label = "Usage Access",
                status = if (hasUsageStats) "✅ Granted" else "🔒 Not granted",
                onClick = {
                    if (!hasUsageStats) {
                        permissionManager.requestUsageStatsPermission()
                    }
                },
            )

            // Sleep schedule row
            SettingsRow(
                label = "Sleep Schedule",
                status = currentSchedule?.let {
                    val h = it.sleepHour.toString().padStart(2, '0')
                    val m = it.sleepMinute.toString().padStart(2, '0')
                    val wh = it.wakeHour.toString().padStart(2, '0')
                    val wm = it.wakeMinute.toString().padStart(2, '0')
                    "🛏 $h:$m → $wh:$wm"
                } ?: "Not set",
                onClick = { showSleepDialog = true },
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSleepDialog) {
        SleepScheduleDialog(
            onDismiss = { showSleepDialog = false },
            onConfirm = { schedule ->
                scope.launch {
                    scheduleStore.saveSchedule(schedule)
                    currentSchedule = schedule
                }
                showSleepDialog = false
            },
        )
    }
}

private fun hasSensor(context: android.content.Context, sensorType: Int): Boolean =
    (context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager)
        ?.getDefaultSensor(sensorType) != null

private fun hasActivityRecognitionPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

@Composable
private fun SettingsRow(
    label: String,
    status: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KalliorColors.ForegroundCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = KalliorColors.NormalText,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = KalliorColors.MutedText,
            )
        }
    }
}
