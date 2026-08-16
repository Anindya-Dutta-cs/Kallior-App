package org.example.project.ui

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import org.example.project.ui.Philosopher

@Composable
fun FocusFortressScreen(navController: NavHostController) {
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
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        // Padding preserved
        Box(modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(40.dp))

        Text(
            text = "Focus Fortress",
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = Philosopher,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
            ),
            color = KalliorColors.NormalText,
            modifier = Modifier.graphicsLayer { scaleY = titleScale },
        )
        Spacer(Modifier.height(40.dp))

        LotusIllustration(
            onClick = {
                refreshProtection()
            },
        )
        
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Protection is active",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontSize = 20.sp),
            color = KalliorColors.NormalText,
            modifier = Modifier.align(Alignment.CenterHorizontally)
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

        Spacer(Modifier.height(60.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricItem(
                title = "Time Sink",
                value = if (screenTimeData == null) "--" else formatDuration(sinkSeconds),
                modifier = Modifier.weight(1f)
            )
            MetricItem(
                title = "Total Screen Time",
                value = totalSeconds?.let(::formatDuration) ?: "--",
                modifier = Modifier.weight(1f),
                alignment = Alignment.End
            )
        }

        Spacer(Modifier.height(32.dp))
        
        TimeWastingAppsCard(
            count = timeWastingApps.size,
            onClick = { navController.navigate("addApp?mode=TIME_WASTING") }
        )
        
        Spacer(Modifier.height(20.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LimitedActionCard(
                title = "Limited Apps",
                subtitle = "${blockedApps.size} Apps Blocked",
                iconRes = R.drawable.apps_blocked,
                onClick = { navController.navigate("addApp") },
                modifier = Modifier.weight(1f)
            )
            LimitedActionCard(
                title = "Limited Websites",
                subtitle = "${blockedWebsites.size} Websites Blocked",
                iconRes = R.drawable.websites_blocked,
                onClick = { showWebsitesDialog = true },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(40.dp))
        
        RedesignedEarningsCard(
            apps = sinkApps,
            hasUsageData = screenTimeData != null,
            totalSinkSeconds = sinkSeconds,
            formattedSinkTime = if (screenTimeData == null) "--" else formatDuration(sinkSeconds),
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
            title = {
                val view = LocalView.current
                SideEffect {
                    val window = (view.parent as? DialogWindowProvider)?.window
                    if (window != null) {
                        window.navigationBarColor = 0xFF161616.toInt()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            window.isNavigationBarContrastEnforced = false
                        }
                    }
                }
                Text("Keep blocking always on", color = KalliorColors.NormalText)
            },
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
private fun LotusIllustration(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing), // Slower for a calm vibe
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Staggered ripples
        listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f).forEach { offset ->
            val progress = (rippleProgress + offset) % 1f
            // Starts small (icon size is ~120dp, base here is 100dp) and expands
            val scale = 0.8f + progress * 2.2f
            val alpha = (1f - progress) * 0.3f

            Box(
                modifier = Modifier
                    .size(120.dp * scale)
                    .graphicsLayer {
                        this.alpha = alpha
                    }
                    .clip(CircleShape)
                    .background(KalliorColors.AccentOrange.copy(alpha = 0.35f))
            )
        }

        // Core glow
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF884411).copy(alpha = 0.5f))
        )

        Icon(
            painter = painterResource(R.drawable.protection__in_active),
            contentDescription = null,
            tint = KalliorColors.AccentOrange,
            modifier = Modifier.size(120.dp)
        )
    }
}

@Composable
private fun MetricItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = KalliorColors.NormalText,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Philosopher, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = value,
                color = KalliorColors.AccentOrange,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Philosopher, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            )
        }
    }
}

@Composable
private fun TimeWastingAppsCard(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(1.dp, KalliorColors.RadarLine, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.apps_selected),
            contentDescription = null,
            tint = KalliorColors.AccentOrange,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Apps that waste your time",
                color = KalliorColors.NormalText,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
            )
            Text(
                "-- $count Apps Selected",
                color = KalliorColors.MutedText,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif)
            )
        }
        ShineActionButton(
            onClick = onClick,
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight
        )
    }
}

@Composable
private fun ShineActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "buttonShine")
    val shineProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 8000
                -1f at 0
                2f at 1500
                2f at 8000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "shineProgress"
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(KalliorColors.AccentOrange)
            .drawWithContent {
                drawContent()
                // Shine gradient
                val shineWidth = size.width * 0.4f
                val centerX = size.width * shineProgress
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        0f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.4f),
                        1f to Color.Transparent,
                        start = Offset(centerX - shineWidth, 0f),
                        end = Offset(centerX + shineWidth, size.height)
                    ),
                    size = size
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            null,
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun LimitedActionCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.height(180.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp)
                .border(1.dp, KalliorColors.RadarLine, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .clickable(onClick = onClick)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = KalliorColors.AccentOrange,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                color = KalliorColors.NormalText,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )
            Text(
                subtitle,
                color = KalliorColors.MutedText,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                textAlign = TextAlign.Center
            )
        }
        ShineActionButton(
            onClick = onClick,
            icon = Icons.Default.KeyboardArrowUp,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun RedesignedEarningsCard(
    apps: List<AppUsageData>,
    hasUsageData: Boolean,
    totalSinkSeconds: Long,
    formattedSinkTime: String,
    ratePerSecond: Float,
    onRateChange: (Float) -> Unit,
) {
    val earnings = totalSinkSeconds * ratePerSecond
    var showRateDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KalliorColors.RadarLine, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .padding(24.dp),
    ) {
        Text(
            "You could've earned",
            color = KalliorColors.MutedText,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Philosopher)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (hasUsageData) "$ ${String.format("%.2f", earnings)}" else "$ --",
            color = KalliorColors.AccentOrange,
            style = MaterialTheme.typography.displayMedium.copy(fontFamily = Philosopher, fontWeight = FontWeight.Bold, fontSize = 36.sp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = buildAnnotatedString {
                append("In the time you spent: ")
                withStyle(style = SpanStyle(color = KalliorColors.AccentOrange)) {
                    append(formattedSinkTime)
                }
            },
            color = KalliorColors.MutedText,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Philosopher),
        )
        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = KalliorColors.RadarLine, thickness = 1.dp)
        Spacer(Modifier.height(16.dp))

        if (apps.isEmpty()) {
            Text(
                text = "Nothing here yet!",
                color = KalliorColors.MutedText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            )
        } else {
            apps.sortedByDescending { it.timeInForegroundMs }.take(4).forEach { RedesignedEarningsRow(it, ratePerSecond) }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = KalliorColors.RadarLine, thickness = 1.dp)
        Spacer(Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Transparent)
                .clickable { showRateDialog = true }
                .padding(horizontal = 32.dp, vertical = 12.dp),
        ) {
            Text(
                "$ ${String.format("%.3f", ratePerSecond)} /sec",
                color = KalliorColors.NormalText,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Philosopher),
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
private fun RedesignedEarningsRow(app: AppUsageData, ratePerSecond: Float) {
    val seconds = app.timeInForegroundMs / 1000L
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(KalliorColors.AccentOrange))
        Spacer(Modifier.width(16.dp))
        Text(
            app.appName,
            color = KalliorColors.NormalText,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            formatDuration(seconds),
            color = KalliorColors.NormalText,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.Center
        )
        Text(
            "$ ${String.format("%.2f", seconds * ratePerSecond)}",
            color = KalliorColors.AccentOrange,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun RateDialog(currentRate: Float, onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var value by remember { mutableStateOf(String.format("%.3f", currentRate)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = KalliorColors.PrimaryLayer,
        title = {
            val view = LocalView.current
            SideEffect {
                val window = (view.parent as? DialogWindowProvider)?.window
                if (window != null) {
                    window.navigationBarColor = 0xFF161616.toInt()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                }
            }
            Text("Set earning rate", color = KalliorColors.NormalText)
        },
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
        title = {
            val view = LocalView.current
            SideEffect {
                val window = (view.parent as? DialogWindowProvider)?.window
                if (window != null) {
                    window.navigationBarColor = 0xFF161616.toInt()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                }
            }
            Text("Limited Websites", color = KalliorColors.NormalText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        },
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
