package org.example.project.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.navigation.NavHostController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.example.project.AppBlockerControllerImpl
import org.example.project.BlockerRepository
import org.example.project.LocalNavBarTransition
import org.example.project.WebsiteBlockerRepository
import kotlin.math.roundToInt

@Composable
fun AddWebsitesScreen(navController: NavHostController) {
    val context = LocalContext.current
    val blockerRepository = remember { BlockerRepository(context) }
    val websiteRepository = remember { WebsiteBlockerRepository(context) }
    val controller = remember {
        AppBlockerControllerImpl(context, blockerRepository, websiteRepository)
    }
    val blockedWebsites by websiteRepository.blockedWebsitesFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    val isNavBarTransitioning = LocalNavBarTransition.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KalliorColors.SecondaryBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(KalliorColors.PrimaryLayer)
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = KalliorColors.NormalText,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Title
            Text(
                text = "Add Websites",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = Philosopher,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                ),
                color = KalliorColors.NormalText,
                textAlign = TextAlign.Center
            )

            // Done Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(KalliorColors.AccentOrange)
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Add Website Divider with Orange + Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            HorizontalDivider(
                color = KalliorColors.RadarLine,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(KalliorColors.AccentOrange)
                    .clickable {
                        scope.launch {
                            isNavBarTransitioning.value = true
                            delay(450) // Trigger slightly before animation finishes
                            showAddDialog = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Website",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Strictly Scrollable Website Cards List
        if (blockedWebsites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No websites added yet.\nTap '+' above to limit a website.",
                    color = KalliorColors.MutedText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(blockedWebsites, key = { it }) { website ->
                    WebsiteSwipeableCard(
                        website = website,
                        onDelete = {
                            scope.launch {
                                controller.removeBlockedWebsite(website)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddWebsiteDialog(
            onDismiss = {
                showAddDialog = false
                isNavBarTransitioning.value = false
            },
            onAdd = { site ->
                scope.launch {
                    controller.addBlockedWebsite(site)
                }
                showAddDialog = false
                isNavBarTransitioning.value = false
            }
        )
    }
}

@Composable
fun WebsiteSwipeableCard(
    website: String,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun startContinuousHaptics() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Repeating light pulse waveform (25ms vibration, 45ms gap) for continuous feel
                val timings = longArrayOf(0, 25, 45)
                val amplitudes = intArrayOf(0, 70, 0)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 25, 45), 0)
            }
        } catch (_: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun stopContinuousHaptics() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    DisposableEffect(Unit) {
        onDispose {
            stopContinuousHaptics()
        }
    }

    // Panel revealed distance in px
    val initialPanelWidth = 64.dp
    val initialPanelWidthPx = with(density) { initialPanelWidth.toPx() }
    val gap = 8.dp
    val gapPx = with(density) { gap.toPx() }
    val revealThresholdPx = initialPanelWidthPx + gapPx

    val offsetX = remember { Animatable(0f) }
    var isRevealed by remember { mutableStateOf(false) }

    // 3.5s hold progress from 0f to 1f
    val holdProgress = remember { Animatable(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    var isHolding by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        val totalWidth = maxWidth
        val totalWidthPx = with(density) { totalWidth.toPx() }
        val progress = holdProgress.value

        // When holding: panel expands from initialPanelWidth to totalWidth
        // Card shrinks horizontally from (totalWidth - initialPanelWidth - gap) down to 0.dp
        val currentPanelWidth = if (isHolding || progress > 0f) {
            initialPanelWidth + (totalWidth - initialPanelWidth) * progress
        } else if (isRevealed) {
            initialPanelWidth
        } else {
            (offsetX.value - gapPx).coerceAtLeast(0f).let { with(density) { it.toDp() } }.coerceAtMost(initialPanelWidth)
        }

        val currentGap = if (isHolding || progress > 0f) {
            gap * (1f - progress)
        } else if (isRevealed || offsetX.value > gapPx) {
            gap
        } else {
            0.dp
        }

        val currentCardOffset = if (isHolding || progress > 0f) {
            currentPanelWidth + currentGap
        } else if (isRevealed) {
            initialPanelWidth + gap
        } else {
            with(density) { offsetX.value.toDp() }
        }

        val currentCardWidth = if (isHolding || progress > 0f) {
            (totalWidth - currentPanelWidth - currentGap).coerceAtLeast(0.dp)
        } else if (isRevealed) {
            (totalWidth - initialPanelWidth - gap).coerceAtLeast(0.dp)
        } else {
            (totalWidth - with(density) { offsetX.value.toDp() }).coerceAtLeast(0.dp)
        }

        // Deletion Panel on the left
        if (offsetX.value > 0f || isRevealed || isHolding || progress > 0f) {
            Box(
                modifier = Modifier
                    .width(currentPanelWidth)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KalliorColors.DangerRed)
                    .pointerInput(website) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                isHolding = true
                                holdJob?.cancel()

                                holdJob = scope.launch {
                                    startContinuousHaptics()

                                    // Periodic tick backup while holding
                                    val ticker = launch {
                                        while (isActive) {
                                            delay(70L)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            }
                                        }
                                    }

                                    // Animate holdProgress from current value to 1f over remaining time (total 3500ms)
                                    val duration = (3500 * (1f - holdProgress.value)).toLong().coerceAtLeast(100L)
                                    holdProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = duration.toInt(), easing = LinearEasing)
                                    )
                                    ticker.cancel()
                                    stopContinuousHaptics()

                                    if (holdProgress.value >= 0.999f) {
                                        onDelete()
                                    }
                                }

                                // Wait for pointer up or cancellation
                                val up = waitForUpOrCancellation()
                                isHolding = false
                                holdJob?.cancel()
                                holdJob = null
                                stopContinuousHaptics()

                                if (up == null || holdProgress.value < 0.999f) {
                                    // Released before 3.5 seconds: safely cancel deletion and contract panel / expand card
                                    scope.launch {
                                        holdProgress.animateTo(0f, animationSpec = tween(250))
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Foreground Website Card (shrinks strictly horizontally)
        if (currentCardWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .offset(x = currentCardOffset)
                    .width(currentCardWidth)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KalliorColors.ForegroundCard)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isRevealed) {
                            scope.launch {
                                offsetX.animateTo(0f, tween(200))
                                isRevealed = false
                            }
                        }
                    }
                    .draggable(
                        state = rememberDraggableState { delta ->
                            val newOffset = (offsetX.value + delta).coerceIn(0f, revealThresholdPx + 20f)
                            scope.launch { offsetX.snapTo(newOffset) }
                        },
                        orientation = Orientation.Horizontal,
                        onDragStopped = {
                            val targetOffset = if (offsetX.value > revealThresholdPx / 2f) {
                                isRevealed = true
                                revealThresholdPx
                            } else {
                                isRevealed = false
                                0f
                            }
                            scope.launch {
                                offsetX.animateTo(targetOffset, tween(200))
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = website,
                    color = KalliorColors.NormalText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitFirstDown(
    requireUnconsumed: Boolean = true
): androidx.compose.ui.input.pointer.PointerInputChange {
    var event: androidx.compose.ui.input.pointer.PointerInputChange
    do {
        val pointerEvent = awaitPointerEvent()
        event = pointerEvent.changes.firstOrNull {
            if (requireUnconsumed) !it.isConsumed && it.pressed else it.pressed
        } ?: pointerEvent.changes.first()
    } while (!event.pressed)
    return event
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.waitForUpOrCancellation(): androidx.compose.ui.input.pointer.PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.all { it.changedToUp() }) {
            return event.changes.firstOrNull()
        }
        if (event.changes.any { it.isConsumed }) {
            return null
        }
    }
}

private fun androidx.compose.ui.input.pointer.PointerInputChange.changedToUp(): Boolean =
    !pressed && previousPressed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWebsiteDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var rawInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 15.3.dp, topEnd = 15.3.dp),
        containerColor = Color.Black,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true,
            shouldDismissOnClickOutside = true,
            isAppearanceLightStatusBars = false,
            isAppearanceLightNavigationBars = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.navigationBarColor = android.graphics.Color.BLACK
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cancel", tint = KalliorColors.MutedText)
                    }
                    Text(
                        "Add Website",
                        color = KalliorColors.NormalText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Philosopher,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    IconButton(
                        onClick = {
                            val normalized = normalizeWebsite(rawInput)
                            if (normalized.isBlank()) {
                                showError = true
                            } else {
                                onAdd(normalized)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, "Add", tint = KalliorColors.AccentOrange)
                    }
                }

                OutlinedTextField(
                    value = rawInput,
                    onValueChange = {
                        rawInput = it
                        showError = false
                    },
                    label = { Text("Website Domain") },
                    placeholder = { Text("e.g. facebook.com") },
                    singleLine = true,
                    isError = showError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val normalized = normalizeWebsite(rawInput)
                            if (normalized.isBlank()) {
                                showError = true
                            } else {
                                onAdd(normalized)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = KalliorColors.NormalText,
                        unfocusedTextColor = KalliorColors.NormalText,
                        focusedBorderColor = KalliorColors.AccentOrange,
                        unfocusedBorderColor = KalliorColors.RadarLine,
                        focusedLabelColor = KalliorColors.AccentOrange,
                        unfocusedLabelColor = KalliorColors.MutedText,
                        cursorColor = KalliorColors.AccentOrange
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text(
                        "Please enter a valid website domain",
                        color = KalliorColors.DangerRed,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
