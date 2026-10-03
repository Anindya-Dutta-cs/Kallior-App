package com.app.kallior.ui

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import kallos.domain.ScreenTimeData
import kallos.platform.PlatformDataFetcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.app.kallior.AppBlockerControllerImpl
import com.app.kallior.BlockEventBus
import com.app.kallior.BlockerRepository
import com.app.kallior.BlockerStatsTracker
import com.app.kallior.InstalledAppsProvider
import com.app.kallior.PermissionManager
import com.app.kallior.SettingsRepository
import com.app.kallior.TimeWastingAppsRepository
import com.app.kallior.WebsiteBlockerRepository

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
    val installedAppsProvider = remember { InstalledAppsProvider(context) }
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
    val blockingEnabled by blockerRepository.isBlockingEnabledFlow.collectAsState(initial = false)
    val vpnEnabled by websiteRepository.isVpnEnabledFlow.collectAsState(initial = false)

    var showAlwaysOnNudge by remember { mutableStateOf(false) }
    var showWebsitesSheet by remember { mutableStateOf(false) }

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

    // The silo is fully protected only when both the app-blocker service and the
    // website VPN report enabled (the VPN flag clears itself on revoke).
    val protectionActive = blockingEnabled && vpnEnabled

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

    // The lotus reacts to real block events: app-open attempts recorded by the
    // blocking overlay and domain blocks emitted by the VPN service.
    val lotusEvents = remember { MutableSharedFlow<LotusEvent>(extraBufferCapacity = 16) }
    val blockerStats by BlockerStatsTracker.updates.collectAsState()
    var lastSeenAttempts by remember { mutableIntStateOf(BlockerStatsTracker.currentAttempts) }
    LaunchedEffect(blockerStats.attempts) {
        if (blockerStats.attempts > lastSeenAttempts) {
            lotusEvents.tryEmit(LotusEvent.AppBlocked)
        }
        lastSeenAttempts = blockerStats.attempts
    }
    LaunchedEffect(Unit) {
        BlockEventBus.blockEvents.collect { lotusEvents.tryEmit(LotusEvent.WebsiteBlocked) }
    }

    val reducedMotion = rememberReducedMotion()

    // Gentle two-phase entrance: hero first, then the supporting content.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    val heroAlpha = (entrance.value / 0.45f).coerceIn(0f, 1f)
    val contentAlpha = ((entrance.value - 0.3f) / 0.55f).coerceIn(0f, 1f)

    // Keep the existing scroll state and verticalScroll modifier: this is what enables
    // Android's overscroll stretch treatment for this screen.
    val scrollState = rememberScrollState()
    val titleScale by animateFloatAsState(
        targetValue = (1f - scrollState.value * 0.0005f).coerceIn(0.8f, 1f),
        label = "zenSiloTitleScale",
    )

    val permissionPrompts = buildList {
        if (!hasUsage) add(PermissionPrompt("Grant Usage Access") { permissionManager.requestUsageStatsPermission() })
        if (!hasOverlay) add(PermissionPrompt("Grant Overlay Permission") { permissionManager.requestOverlayPermission() })
        if (batteryOptimizationEnabled) add(PermissionPrompt("Disable Battery Optimization") { permissionManager.requestIgnoreBatteryOptimizations() })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KalliorColors.CanvasBackground)
            .verticalScroll(scrollState)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Zen Silo",
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = Philosopher,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
            ),
            color = KalliorColors.NormalText,
            modifier = Modifier.graphicsLayer { scaleY = titleScale },
        )
        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = heroAlpha
                    translationY = (1f - heroAlpha) * 24.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProtectionHero(
                active = protectionActive,
                attemptsToday = blockerStats.attempts,
                events = lotusEvents,
                reducedMotion = reducedMotion,
                permissionPrompts = permissionPrompts,
                onActivate = { refreshProtection() },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = contentAlpha
                    translationY = (1f - contentAlpha) * 20.dp.toPx()
                },
        ) {
            Spacer(Modifier.height(40.dp))

            ImpactSummaryRow(
                sinkSeconds = sinkSeconds,
                totalSeconds = totalSeconds,
                isLoading = screenTimeData == null,
            )

            Spacer(Modifier.height(36.dp))

            SectionLabel(text = "Restrictions")
            Spacer(Modifier.height(12.dp))
            RestrictionsSection(
                limitedApps = blockedApps.size,
                limitedWebsites = blockedWebsites.size,
                onAppsClick = { navController.navigate("addApp") },
                onWebsitesClick = { showWebsitesSheet = true },
            )

            Spacer(Modifier.height(36.dp))

            TopDistractionsSection(
                apps = sinkApps,
                hasUsageData = screenTimeData != null,
                loadIcon = installedAppsProvider::getApplicationIcon,
                onManage = { navController.navigate("addApp?mode=TIME_WASTING") },
            )

            Spacer(Modifier.height(36.dp))

            ValueAtRiskSection(
                hasUsageData = screenTimeData != null,
                totalSinkSeconds = sinkSeconds,
                apps = sinkApps,
                ratePerSecond = ratePerSecond,
                onRateChange = { rate -> scope.launch { settingsRepository.setRatePerSecond(rate) } },
                loadIcon = installedAppsProvider::getApplicationIcon,
            )
        }

        Spacer(Modifier.height(140.dp))
    }

    if (showWebsitesSheet) {
        LimitedWebsitesBottomSheet(
            websites = blockedWebsites,
            onDismiss = { showWebsitesSheet = false },
            onEditClick = {
                showWebsitesSheet = false
                navController.navigate("addWebsites")
            },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitedWebsitesBottomSheet(
    websites: List<String>,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KalliorColors.SecondaryBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(KalliorColors.RadarLine)
            )
        },
        shape = RoundedCornerShape(topStart = 15.3.dp, topEnd = 15.3.dp),
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
                ?: (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = 0xFF161616.toInt()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row: Title, Visibility Toggle, Edit Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Limited Websites",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = Philosopher,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    ),
                    color = KalliorColors.NormalText,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Visibility Toggle Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(KalliorColors.PrimaryLayer)
                            .clickable { isVisible = !isVisible },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isVisible) "Hide website names" else "Show website names",
                            tint = KalliorColors.NormalText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Edit Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(KalliorColors.AccentOrange)
                            .clickable { onEditClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit websites",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = KalliorColors.RadarLine,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Strictly Scrollable Website Cards List
            if (websites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No websites limited yet.",
                        color = KalliorColors.MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(websites) { site ->
                        val displayText = if (isVisible) site else "•".repeat(site.length.coerceAtLeast(10))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(KalliorColors.ForegroundCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayText,
                                color = KalliorColors.NormalText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

fun normalizeWebsite(input: String): String = input.trim().lowercase()
    .removePrefix("https://")
    .removePrefix("http://")
    .removePrefix("www.")
    .trimEnd('/')
    .substringBefore('/')
    .substringBefore('?')
    .substringBefore(':')
    .trimEnd('.')
