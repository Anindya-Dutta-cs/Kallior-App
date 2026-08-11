package org.example.project.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import kallos.domain.AppUsageData
import kallos.domain.ScreenTimeData
import kallos.platform.PlatformDataFetcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.example.project.AppBlockerControllerImpl
import org.example.project.BlockerRepository
import org.example.project.PermissionManager
import org.example.project.R
import org.example.project.SettingsRepository
import org.example.project.TimeWastingAppsRepository
import org.example.project.WebsiteBlockerRepository

@Composable
fun FocusFortressScreen(navController: NavHostController, onMenuClick: () -> Unit) {
    val context = LocalContext.current
    val dataFetcher = remember { PlatformDataFetcher() }
    val blockerRepository = remember { BlockerRepository(context) }
    val timeWastingRepository = remember { TimeWastingAppsRepository(context) }
    val websiteRepository = remember { WebsiteBlockerRepository(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    val controller = remember {
        AppBlockerControllerImpl(context, blockerRepository, websiteRepository)
    }
    val permissionManager = remember { PermissionManager(context) }
    val scope = rememberCoroutineScope()

    val screenTimeData by produceState<ScreenTimeData?>(initialValue = null) {
        while (isActive) {
            value = dataFetcher.getScreenTimeData()
            delay(10_000L)
        }
    }
    val blockedApps by blockerRepository.blockedAppsFlow.collectAsState(initial = emptySet())
    val timeWastingApps by timeWastingRepository.timeWastingAppsFlow.collectAsState(initial = emptySet())
    val blockedWebsites by websiteRepository.blockedWebsitesFlow.collectAsState(initial = emptyList())

    val ratePerSecond by settingsRepository.ratePerSecondFlow.collectAsState(initial = 0.005f)
    var showAlwaysOnNudge by remember { mutableStateOf(false) }
    var showWebsitesDialog by remember { mutableStateOf(false) }

    var hasUsage by remember { mutableStateOf(permissionManager.hasUsageStatsPermission()) }
    var hasOverlay by remember { mutableStateOf(permissionManager.hasOverlayPermission()) }
    var batteryOptimizationEnabled by remember { mutableStateOf(permissionManager.isBatteryOptimizationEnabled()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsage = permissionManager.hasUsageStatsPermission()
                hasOverlay = permissionManager.hasOverlayPermission()
                batteryOptimizationEnabled = permissionManager.isBatteryOptimizationEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun showAlwaysOnNudgeOnce() {
        scope.launch {
            if (websiteRepository.claimAlwaysOnNudge()) showAlwaysOnNudge = true
        }
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            controller.startWebsiteBlocking()
            showAlwaysOnNudgeOnce()
        }
    }

    fun refreshProtection() {
        if (!hasUsage || !hasOverlay) return
        controller.startBlocking()
        val vpnIntent = permissionManager.getVpnPermissionIntent()
        if (vpnIntent == null) {
            controller.startWebsiteBlocking()
            showAlwaysOnNudgeOnce()
        } else {
            vpnPermissionLauncher.launch(vpnIntent)
        }
    }

    // Protection is intentionally one-way from this screen. Start it whenever the
    // required permissions become available; subsequent taps only refresh it.
    LaunchedEffect(hasUsage, hasOverlay) {
        if (hasUsage && hasOverlay) refreshProtection()
    }

    // App usages are the precise source used by the Android backend for the total.
    // This includes selected time-sink apps without adding their time a second time.
    val totalSeconds = screenTimeData?.let { data ->
        data.appUsages.sumOf { it.timeInForegroundMs / 1000L }
            .takeIf { it > 0L }
            ?: data.totalMinutes.toLong() * 60L
    }
    val sinkApps = screenTimeData?.appUsages
        ?.filter { it.packageName in timeWastingApps }
        .orEmpty()
    val sinkSeconds = sinkApps.sumOf { it.timeInForegroundMs / 1000L }

    // Keep the existing scroll state and verticalScroll modifier: this is what enables
    // Android's overscroll stretch treatment for this screen.
    val scrollState = rememberScrollState()
    val titleScale by animateFloatAsState(
        targetValue = (1f - scrollState.value * 0.0005f).coerceIn(0.8f, 1f),
        label = "fortressScale",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KalliorColors.SecondaryBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 36.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        MenuButton(onClick = onMenuClick)
        Spacer(Modifier.height(40.dp))

        Text(
            text = "Focus Fortress",
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
            ),
            color = KalliorColors.NormalText,
            modifier = Modifier.graphicsLayer { scaleY = titleScale },
        )
        Spacer(Modifier.height(30.dp))

        val refreshRotation = remember { Animatable(0f) }
        ProtectionCard(
            rotation = refreshRotation.value,
            onClick = {
                scope.launch {
                    refreshRotation.animateTo(refreshRotation.value + 360f, animationSpec = tween(600))
                }
                refreshProtection()
            },
        )

        if (!hasUsage) {
            PermissionRow("Grant Usage Access") { permissionManager.requestUsageStatsPermission() }
        }
        if (!hasOverlay) {
            PermissionRow("Grant Overlay Permission") { permissionManager.requestOverlayPermission() }
        }
        if (batteryOptimizationEnabled) {
            PermissionRow("Disable Battery Optimization") { permissionManager.requestIgnoreBatteryOptimizations() }
        }
        if (hasUsage && hasOverlay) {
            PermissionRow("Enable Always-on VPN for 24/7 blocking") {
                context.startActivity(permissionManager.getAlwaysOnVpnSettingsIntent())
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = "Overview",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif, fontSize = 31.sp),
            color = KalliorColors.NormalText,
        )
        Spacer(Modifier.height(17.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            OverviewMetricCard(
                title = "Total Screen Time",
                value = totalSeconds?.let(::formatDuration) ?: "--",
                iconRes = R.drawable.totalscreentime,
                modifier = Modifier.weight(1f),
            )
            OverviewMetricCard(
                title = "Time Sink",
                value = if (screenTimeData == null) "--" else formatDuration(sinkSeconds),
                iconRes = R.drawable.time_sink,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(38.dp))
        FortressListsCard(
            timeWastingCount = timeWastingApps.size,
            blockedAppsCount = blockedApps.size,
            blockedWebsitesCount = blockedWebsites.size,
            onTimeWastersClick = { navController.navigate("addApp?mode=TIME_WASTING") },
            onAppsClick = { navController.navigate("addApp") },
            onWebsitesClick = { showWebsitesDialog = true },
        )

        Spacer(Modifier.height(38.dp))
        EarningsCard(
            apps = sinkApps,
            hasUsageData = screenTimeData != null,
            totalSinkSeconds = sinkSeconds,
            ratePerSecond = ratePerSecond,
            onRateChange = { rate -> scope.launch { settingsRepository.setRatePerSecond(rate) } },
        )
        Spacer(Modifier.height(42.dp))
    }

    if (showWebsitesDialog) {
        BlockedWebsitesDialog(
            websites = blockedWebsites,
            onDismiss = { showWebsitesDialog = false },
            onAdd = { website -> scope.launch { controller.addBlockedWebsite(website) } },
            onRemove = { website -> scope.launch { controller.removeBlockedWebsite(website) } },
        )
    }
    if (showAlwaysOnNudge) {
        AlertDialog(
            onDismissRequest = { showAlwaysOnNudge = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = KalliorColors.PrimaryLayer,
            title = { Text("Keep blocking always on", color = KalliorColors.NormalText) },
            text = {
                Text(
                    "For blocking to survive app restarts and background cleanup, tap the gear " +
                        "icon next to Kallior in this screen and enable Always-on VPN.",
                    color = KalliorColors.MutedText,
                )
            },
            confirmButton = {
                TextButton(onClick = { showAlwaysOnNudge = false }) {
                    Text("Got it", color = KalliorColors.AccentOrange)
                }
            },
        )
    }
}

@Composable
private fun MenuButton(onClick: () -> Unit) {
    Icon(
        painter = painterResource(R.drawable.side_bar_button),
        contentDescription = "Menu",
        tint = KalliorColors.NormalText,
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ProtectionCard(rotation: Float, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(KalliorColors.PrimaryLayer)
            .clickable(onClick = onClick)
            .padding(horizontal = 37.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Protection is active",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontSize = 18.sp),
            color = KalliorColors.NormalText,
        )
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(KalliorColors.AccentOrange),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.apps_blocked),
                contentDescription = "Refresh protection",
                tint = Color(0xFF321E0B),
                modifier = Modifier
                    .size(23.dp)
                    .graphicsLayer { rotationZ = rotation },
            )
        }
    }
}

@Composable
private fun OverviewMetricCard(title: String, value: String, iconRes: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(95.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(KalliorColors.PrimaryLayer)
            .padding(horizontal = 14.dp, vertical = 16.dp),
    ) {
        Text(
            text = title,
            color = KalliorColors.NormalText,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Serif, fontSize = 10.sp),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = KalliorColors.AccentOrange,
                modifier = Modifier.size(if (iconRes == R.drawable.time_sink) 47.dp else 43.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = value,
                color = KalliorColors.AccentOrange,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FortressListsCard(
    timeWastingCount: Int,
    blockedAppsCount: Int,
    blockedWebsitesCount: Int,
    onTimeWastersClick: () -> Unit,
    onAppsClick: () -> Unit,
    onWebsitesClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(21.dp))
            .background(KalliorColors.PrimaryLayer)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        FortressListRow("Apps that waste your time", "-- $timeWastingCount Apps Selected", R.drawable.apps_selected, onTimeWastersClick)
        HorizontalDivider(color = KalliorColors.RadarLine, thickness = 1.dp)
        FortressListRow("Limited Apps", "-- $blockedAppsCount Apps Blocked", R.drawable.apps_blocked, onAppsClick)
        HorizontalDivider(color = KalliorColors.RadarLine, thickness = 1.dp)
        FortressListRow("Limited Websites", "-- $blockedWebsitesCount Websites Blocked", R.drawable.websites_blocked, onWebsitesClick)
    }
}

@Composable
private fun FortressListRow(title: String, subtitle: String, iconRes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = KalliorColors.AccentOrange,
            modifier = Modifier.size(31.dp),
        )
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = KalliorColors.NormalText, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp))
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = KalliorColors.MutedText, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Serif, fontSize = 10.sp))
        }
        Box(
            modifier = Modifier.size(31.dp).clip(CircleShape).background(KalliorColors.AccentOrange),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open $title", tint = Color.Black, modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
private fun EarningsCard(
    apps: List<AppUsageData>,
    hasUsageData: Boolean,
    totalSinkSeconds: Long,
    ratePerSecond: Float,
    onRateChange: (Float) -> Unit,
) {
    val earnings = totalSinkSeconds * ratePerSecond
    var showRateDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(21.dp))
            .background(KalliorColors.PrimaryLayer)
            .padding(horizontal = 29.dp, vertical = 20.dp),
    ) {
        Text("You could've earned", color = KalliorColors.NormalText, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif, fontSize = 12.sp))
        Spacer(Modifier.height(5.dp))
        Text(
            text = if (hasUsageData) "$${String.format("%.2f", earnings)}" else "$ --",
            color = KalliorColors.AccentOrange,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "In the time you spent: [show 'Sink Time']",
            color = KalliorColors.NormalText,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif, fontSize = 12.sp),
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = KalliorColors.RadarLine, thickness = 1.dp)
        Spacer(Modifier.height(11.dp))

        if (apps.isEmpty()) {
            Text(
                text = "Nothing here yet!",
                color = KalliorColors.MutedText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            )
        } else {
            apps.sortedByDescending { it.timeInForegroundMs }.take(4).forEach { EarningsRow(it, ratePerSecond) }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = KalliorColors.RadarLine, thickness = 1.dp)
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Transparent)
                .clickable { showRateDialog = true }
                .padding(horizontal = 27.dp, vertical = 7.dp),
        ) {
            Text(
                "$${String.format("%.2f", ratePerSecond)} /sec",
                color = KalliorColors.NormalText,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
            )
        }
    }
    if (showRateDialog) {
        RateDialog(ratePerSecond, onDismiss = { showRateDialog = false }, onSave = {
            onRateChange(it)
            showRateDialog = false
        })
    }
}

@Composable
private fun EarningsRow(app: AppUsageData?, ratePerSecond: Float) {
    val seconds = app?.timeInForegroundMs?.div(1000L) ?: 0L
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(KalliorColors.AccentOrange))
        Spacer(Modifier.width(14.dp))
        Text(app?.appName ?: "App name", color = KalliorColors.NormalText, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif, fontSize = 12.sp), modifier = Modifier.weight(1f), maxLines = 1)
        Text(if (app == null) "--" else formatDuration(seconds), color = KalliorColors.NormalText, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif, fontSize = 12.sp), modifier = Modifier.width(72.dp), textAlign = TextAlign.Center, maxLines = 1)
        Text(if (app == null) "$--" else "$${String.format("%.2f", seconds * ratePerSecond)}", color = KalliorColors.AccentOrange, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif, fontSize = 12.sp), modifier = Modifier.width(35.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun RateDialog(currentRate: Float, onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var value by remember { mutableStateOf(String.format("%.3f", currentRate)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = KalliorColors.PrimaryLayer,
        title = { Text("Set earning rate", color = KalliorColors.NormalText) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Amount per second") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = dialogFieldColors(),
            )
        },
        confirmButton = {
            TextButton(onClick = { value.toFloatOrNull()?.let { onSave(it.coerceIn(0.001f, 1f)) } }) {
                Text("Save", color = KalliorColors.AccentOrange)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = KalliorColors.MutedText) } },
    )
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${totalSeconds}s"
    }
}

@Composable
private fun PermissionRow(text: String, onClick: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(KalliorColors.ForegroundCard).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
    ) { Text(text, color = KalliorColors.AccentOrange, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
fun BlockedWebsitesDialog(
    websites: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var rawInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = KalliorColors.PrimaryLayer,
        title = { Text("Limited Websites", color = KalliorColors.NormalText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (websites.isEmpty()) {
                    Text("No websites limited yet.", color = KalliorColors.MutedText)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(websites) { site ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(KalliorColors.ForegroundCard).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(site, color = KalliorColors.NormalText, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onRemove(site) }) { Icon(Icons.Default.Delete, "Remove $site", tint = KalliorColors.AccentOrange) }
                            }
                        }
                    }
                }
                OutlinedTextField(value = rawInput, onValueChange = { rawInput = it; showError = false }, label = { Text("Add a website") }, placeholder = { Text("e.g. facebook.com") }, singleLine = true, isError = showError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), colors = dialogFieldColors(), modifier = Modifier.fillMaxWidth())
                if (showError) Text("Enter a valid website", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val normalized = normalizeWebsite(rawInput)
                if (normalized.isBlank()) showError = true else {
                    onAdd(normalized)
                    rawInput = ""
                }
            }) { Text("Add", color = KalliorColors.AccentOrange) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done", color = KalliorColors.MutedText) } },
    )
}

private fun normalizeWebsite(input: String): String = input.trim().lowercase()
    .removePrefix("https://")
    .removePrefix("http://")
    .removePrefix("www.")
    .trimEnd('/')
    .substringBefore('/')
    .substringBefore('?')
    .substringBefore(':')
    .trimEnd('.')
