package com.app.kallior.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kallos.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import com.app.kallior.UserPreferencesStore
import com.app.kallior.health.HealthConnectPermissionHelper
import com.app.kallior.health.HealthDependencies
import com.app.kallior.health.SleepSchedule

/**
 * Settings screen with functional options:
 * - Account & Security (no-op)
 * - Permission Manager → navigates to permissions screen
 * - Health Connect → shows availability status
 * - Sleep Schedule → opens bottom sheet with wheel pickers
 * - Activity Level → dropdown that updates RadarChartEngine.stepTarget
 * - Screen Use → dropdown that updates RadarChartEngine.entertainmentAllowanceMinutes
 * - Report Bug (no-op)
 * - About Us → navigates to placeholder screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    gameViewModel: GameViewModel,
    navController: NavController,
    onLogout: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Preferences Store ---
    val prefsStore = remember { UserPreferencesStore(context) }
    val activityLevel by prefsStore.activityLevelFlow.collectAsState(initial = "Casual")
    val screenUse by prefsStore.screenUseFlow.collectAsState(initial = "1-2 hours")

    // Restore persisted preferences into engine on first composition
    LaunchedEffect(Unit) { prefsStore.restoreIntoEngine() }

    // --- Health Connect availability ---
    val healthHelper = remember { HealthDependencies.healthConnectPermissionHelper(context) }
    val isHealthConnectAvailable = remember { healthHelper.isHealthConnectAvailable() }

    // --- Sleep schedule ---
    val sleepStore = remember { HealthDependencies.sleepScheduleStore(context) }
    val currentSchedule by sleepStore.scheduleFlow.collectAsState(initial = null)
    var showSleepSheet by remember { mutableStateOf(false) }

    // --- Activity Level dropdown ---
    var showActivityDropdown by remember { mutableStateOf(false) }

    // --- Screen Use dropdown ---
    var showScreenUseDropdown by remember { mutableStateOf(false) }

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
            text = "Settings",
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = Philosopher,
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )

        Spacer(Modifier.height(48.dp))

        // --- General ---
        SettingsHeader("General")
        SettingsCard {
            SettingsItem(label = "Account & Security", showChevron = true) {}
            SettingsItem(label = "Log out") { onLogout() }
            SettingsItem(label = "Permission Manager", showChevron = true) {
                navController.navigate("permissions")
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- Health & Metrics ---
        SettingsHeader("Health & Metrics")
        SettingsCard {
            // Health Connect — status badge
            SettingsItem(
                label = "Health Connect",
                trailing = {
                    HealthConnectBadge(isAvailable = isHealthConnectAvailable)
                },
            ) {}

            // Sleep Schedule — bottom sheet
            SettingsItem(label = "Sleep Schedule", showChevron = true) {
                showSleepSheet = true
            }

            // Activity Level — dropdown
            SettingsItem(
                label = "Activity Level",
                trailing = {
                    Box {
                        SettingsSelector(
                            value = activityLevel,
                            onClick = { showActivityDropdown = true },
                        )
                        DropdownMenu(
                            expanded = showActivityDropdown,
                            onDismissRequest = { showActivityDropdown = false },
                            containerColor = KalliorColors.SurfaceElevated,
                        ) {
                            listOf("Sedentary", "Casual", "Highly Active").forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option,
                                            color = if (option == activityLevel) KalliorColors.AccentOrange
                                            else Color.White,
                                        )
                                    },
                                    onClick = {
                                        showActivityDropdown = false
                                        scope.launch { prefsStore.setActivityLevel(option) }
                                    },
                                )
                            }
                        }
                    }
                },
            ) { showActivityDropdown = true }

            // Screen Use — dropdown
            SettingsItem(
                label = "Screen Use",
                trailing = {
                    Box {
                        SettingsSelector(
                            value = screenUse,
                            onClick = { showScreenUseDropdown = true },
                        )
                        DropdownMenu(
                            expanded = showScreenUseDropdown,
                            onDismissRequest = { showScreenUseDropdown = false },
                            containerColor = KalliorColors.SurfaceElevated,
                        ) {
                            listOf("1-2 hours", "3-4 hours", "5-6 hours", "6+ hours").forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option,
                                            color = if (option == screenUse) KalliorColors.AccentOrange
                                            else Color.White,
                                        )
                                    },
                                    onClick = {
                                        showScreenUseDropdown = false
                                        scope.launch { prefsStore.setScreenUse(option) }
                                    },
                                )
                            }
                        }
                    }
                },
            ) { showScreenUseDropdown = true }
        }

        Spacer(Modifier.height(32.dp))

        // --- App ---
        SettingsHeader("App")
        SettingsCard {
            SettingsItem(label = "Report Bug") {}
            SettingsItem(label = "About Us", showChevron = true) {
                navController.navigate("about")
            }
        }

        Spacer(
            Modifier
                .height(140.dp)
                .navigationBarsPadding()
        )
    }

    // ---- Sleep Schedule Bottom Sheet ----
    if (showSleepSheet) {
        SleepScheduleBottomSheet(
            initialSchedule = currentSchedule,
            onDismiss = { showSleepSheet = false },
            onSave = { schedule ->
                scope.launch {
                    sleepStore.saveSchedule(schedule)
                }
                showSleepSheet = false
            },
        )
    }
}

// ============================================================================
// Health Connect Badge
// ============================================================================

@Composable
private fun HealthConnectBadge(isAvailable: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isAvailable) Color(0xFF4CAF50).copy(alpha = 0.12f)
                else Color(0xFFB80E0C).copy(alpha = 0.12f)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = if (isAvailable) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFB80E0C),
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = if (isAvailable) "Available" else "Not Available",
            style = MaterialTheme.typography.labelSmall,
            color = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFB80E0C),
        )
    }
}

// ============================================================================
// Sleep Schedule Bottom Sheet — Wheel Pickers
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepScheduleBottomSheet(
    initialSchedule: SleepSchedule?,
    onDismiss: () -> Unit,
    onSave: (SleepSchedule) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var sleepHour by remember { mutableIntStateOf(initialSchedule?.sleepHour ?: 23) }
    var sleepMinute by remember { mutableIntStateOf(initialSchedule?.sleepMinute ?: 0) }
    var wakeHour by remember { mutableIntStateOf(initialSchedule?.wakeHour ?: 7) }
    var wakeMinute by remember { mutableIntStateOf(initialSchedule?.wakeMinute ?: 0) }

    val schedule = SleepSchedule(sleepHour, sleepMinute, wakeHour, wakeMinute)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KalliorColors.PrimaryLayer,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title
            Text(
                text = "Sleep Schedule",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = Philosopher,
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Set when you usually sleep and wake up",
                style = MaterialTheme.typography.bodySmall,
                color = KalliorColors.MutedText,
            )

            Spacer(Modifier.height(24.dp))

            // Sleep Time
            Text(
                text = "SLEEP TIME",
                style = MaterialTheme.typography.labelMedium,
                color = KalliorColors.MutedText,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                WheelPicker(
                    items = (0..23).map { String.format("%02d", it) },
                    selectedIndex = sleepHour,
                    onValueChange = { sleepHour = it },
                    modifier = Modifier.width(80.dp),
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                WheelPicker(
                    items = (0..59).map { String.format("%02d", it) },
                    selectedIndex = sleepMinute,
                    onValueChange = { sleepMinute = it },
                    modifier = Modifier.width(80.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            // Wake Time
            Text(
                text = "WAKE TIME",
                style = MaterialTheme.typography.labelMedium,
                color = KalliorColors.MutedText,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                WheelPicker(
                    items = (0..23).map { String.format("%02d", it) },
                    selectedIndex = wakeHour,
                    onValueChange = { wakeHour = it },
                    modifier = Modifier.width(80.dp),
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                WheelPicker(
                    items = (0..59).map { String.format("%02d", it) },
                    selectedIndex = wakeMinute,
                    onValueChange = { wakeMinute = it },
                    modifier = Modifier.width(80.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            // Duration estimate
            Text(
                text = "≈ ${String.format("%.1f", schedule.durationHours())} hours of sleep",
                style = MaterialTheme.typography.labelLarge,
                color = KalliorColors.AccentOrange,
            )

            Spacer(Modifier.height(28.dp))

            // Save button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KalliorColors.AccentOrange)
                    .clickable { onSave(schedule) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ============================================================================
// Wheel Picker — Snap-scrolling number drum
// ============================================================================

@Composable
private fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
) {
    val itemHeightDp = 44.dp
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeightDp.toPx() }
    val halfVisible = visibleCount / 2
    val totalHeight = itemHeightDp * visibleCount

    // Pad items so the first and last can be centred
    val paddedItems = List(halfVisible) { "" } + items + List(halfVisible) { "" }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Emit selection changes when snapping settles
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centreIndex = listState.firstVisibleItemIndex +
                    halfVisible -
                    // Adjust based on the offset of the first visible item
                    if (listState.firstVisibleItemScrollOffset > itemHeightPx / 2) 0 else 0
            val idx = (listState.firstVisibleItemIndex).coerceIn(0, items.size - 1)
            if (idx != selectedIndex) {
                onValueChange(idx)
            }
        }
    }

    Box(
        modifier = modifier
            .height(totalHeight)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    // Top fade
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF161616),
                                Color.Transparent,
                            ),
                            startY = 0f,
                            endY = itemHeightPx * 1.5f,
                        ),
                    )
                    // Bottom fade
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF161616),
                            ),
                            startY = size.height - itemHeightPx * 1.5f,
                            endY = size.height,
                        ),
                    )
                },
        ) {
            items(paddedItems.size) { index ->
                val text = paddedItems[index]
                val realIndex = index - halfVisible
                val isSelected = realIndex == selectedIndex

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (text.isNotEmpty()) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (isSelected) Color.White
                            else Color.White.copy(alpha = 0.25f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Centre highlight strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                )
                .background(
                    Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(10.dp),
                ),
        )
    }
}

// ============================================================================
// Shared private composables (kept from original)
// ============================================================================

@Composable
private fun SettingsHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = Philosopher,
            fontWeight = FontWeight.Bold
        ),
        color = Color.White,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(KalliorColors.SurfaceCharcoal),
        content = content
    )
}

@Composable
private fun SettingsItem(
    label: String,
    showChevron: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        if (trailing != null) {
            trailing()
        } else if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsSelector(value: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Icon(
                imageVector = Icons.Default.UnfoldMore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
