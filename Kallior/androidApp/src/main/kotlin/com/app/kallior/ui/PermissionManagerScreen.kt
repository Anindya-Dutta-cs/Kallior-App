package com.app.kallior.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.app.kallior.PermissionManager

/**
 * Describes a single permission row in the Permission Manager screen.
 */
private data class PermissionEntry(
    val label: String,
    val isGranted: (Context) -> Boolean,
    val openSettings: (Context) -> Unit,
)

/**
 * Full-screen Permission Manager that lists every permission the app
 * requests. Each row shows a ✓ tick when granted and an "Allow" button
 * when denied. Tapping "Allow" redirects the user to the relevant
 * Android system settings page.
 */
@Composable
fun PermissionManagerScreen() {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }

    // Re-check permission state each time the screen is resumed (user
    // may have toggled a permission in system settings and come back).
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val entries = remember(refreshKey) { buildPermissionEntries(permissionManager) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text = "Permissions",
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = Philosopher,
                fontWeight = FontWeight.Bold,
            ),
            color = Color.White,
        )

        Spacer(Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(KalliorColors.SurfaceCharcoal),
        ) {
            entries.forEach { entry ->
                val granted = entry.isGranted(context)
                PermissionRow(
                    label = entry.label,
                    isGranted = granted,
                    onAllowClick = { entry.openSettings(context) },
                )
            }
        }

        Spacer(
            Modifier
                .height(140.dp)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun PermissionRow(
    label: String,
    isGranted: Boolean,
    onAllowClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isGranted) Modifier.clickable(onClick = onAllowClick)
                else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )

        if (isGranted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Granted",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(KalliorColors.AccentOrange)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Allow",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Permission entry definitions
// ---------------------------------------------------------------------------

private fun buildPermissionEntries(pm: PermissionManager): List<PermissionEntry> {
    val entries = mutableListOf<PermissionEntry>()

    // 1. Notifications (POST_NOTIFICATIONS — runtime from API 33+)
    entries += PermissionEntry(
        label = "Notifications",
        isGranted = { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            } else true
        },
        openSettings = { ctx ->
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
        },
    )

    // 2. Activity Recognition
    entries += PermissionEntry(
        label = "Activity Recognition",
        isGranted = { ctx ->
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION) ==
                    PackageManager.PERMISSION_GRANTED
        },
        openSettings = { ctx -> openAppDetails(ctx) },
    )

    // 3. Fine Location
    entries += PermissionEntry(
        label = "Fine Location",
        isGranted = { ctx ->
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        },
        openSettings = { ctx -> openAppDetails(ctx) },
    )

    // 4. Coarse Location
    entries += PermissionEntry(
        label = "Coarse Location",
        isGranted = { ctx ->
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        },
        openSettings = { ctx -> openAppDetails(ctx) },
    )

    // 5. Exact Alarms (API 31+)
    entries += PermissionEntry(
        label = "Exact Alarms",
        isGranted = { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                am.canScheduleExactAlarms()
            } else true
        },
        openSettings = { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${ctx.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            }
        },
    )

    // 6. Usage Stats (special permission)
    entries += PermissionEntry(
        label = "Usage Stats",
        isGranted = { pm.hasUsageStatsPermission() },
        openSettings = { pm.requestUsageStatsPermission() },
    )

    // 7. Draw Over Other Apps (special permission)
    entries += PermissionEntry(
        label = "Draw Over Apps",
        isGranted = { pm.hasOverlayPermission() },
        openSettings = { pm.requestOverlayPermission() },
    )

    // 8. Ignore Battery Optimisation (special permission)
    entries += PermissionEntry(
        label = "Battery Optimization",
        isGranted = { !pm.isBatteryOptimizationEnabled() },
        openSettings = { pm.requestIgnoreBatteryOptimizations() },
    )

    return entries
}

private fun openAppDetails(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
