package com.app.kallior.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import kallos.engine.ShadowTaskState
import kallos.model.Remainder
import kallos.model.Task
import kallos.model.TaskStatus
import kallos.viewmodel.GameViewModel
import kallos.viewmodel.TaskUi
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.app.kallior.LocalNavBarTransition
import com.app.kallior.R
import com.app.kallior.notification.AlarmScheduler
import com.app.kallior.notification.NotificationHelper
import com.app.kallior.ui.theme.ShadowPurple

private enum class LayerDragValue { Closed, Open }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    gameViewModel: GameViewModel,
) {
    val shadowHomeState by gameViewModel.shadowHomeState.collectAsState()
    var showTaskDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    val isNavBarTransitioning = LocalNavBarTransition.current

    val context = LocalContext.current
    val alarmScheduler = remember { AlarmScheduler(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* granted or denied */ }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionLauncher.launch(permissions.toTypedArray())
        NotificationHelper.createChannel(context)
    }

    val scrollState = rememberScrollState()
    val scale by animateFloatAsState(
        targetValue = (1f - scrollState.value * 0.00035f).coerceIn(0.88f, 1f),
        label = "radarScale",
    )

    val s = gameViewModel.userScores
    val radarValues = listOf(s.focus, s.discipline, s.health, s.resilience, s.consistency)
    val progressPercent = (radarValues.average() / 100.0).toFloat()

    val radarIcons = listOf(
        painterResource(R.drawable.focus_icon),
        painterResource(R.drawable.discipline),
        painterResource(R.drawable.health),
        painterResource(R.drawable.resilience),
        painterResource(R.drawable.consistency),
    )

    val screenWidthPx = context.resources.displayMetrics.widthPixels.toFloat()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = LayerDragValue.Closed,
            anchors = DraggableAnchors {
                LayerDragValue.Closed at screenWidthPx
                LayerDragValue.Open at 0f
            },
            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
            velocityThreshold = { with(density) { 900.dp.toPx() } },
            snapAnimationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 380f),
            decayAnimationSpec = exponentialDecay(),
        )
    }

    val currentOffset = draggableState.offset.takeIf { !it.isNaN() } ?: screenWidthPx
    val shadowRevealed = currentOffset < screenWidthPx / 2f

    if (draggableState.targetValue == LayerDragValue.Open || currentOffset < screenWidthPx) {
        BackHandler {
            coroutineScope.launch { draggableState.animateTo(LayerDragValue.Closed) }
        }
    }

    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }
    val headerAlpha = ((entrance.value - 0f) / 0.18f).coerceIn(0f, 1f)
    val contentAlpha = ((entrance.value - 0.32f) / 0.45f).coerceIn(0f, 1f)

    var heroHeightPx by remember { mutableIntStateOf(0) }
    val heroDp = with(density) { if (heroHeightPx == 0) 460.dp else heroHeightPx.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KalliorColors.CanvasBackground)
            .anchoredDraggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (shadowRevealed) 1f else -1f)
                .drawWithContent {
                    val scope = this
                    val progress = (currentOffset / screenWidthPx).coerceIn(0f, 1f)
                    val distortIntensity = (1f - kotlin.math.abs(progress - 0.5f) * 2f).coerceIn(0f, 1f)
                    val glitchWidth = 32.dp.toPx() * distortIntensity
                    clipRect(left = currentOffset + glitchWidth) {
                        scope.drawContent()
                    }
                    if (distortIntensity > 0f) {
                        val segmentHeight = 12.dp.toPx()
                        val segments = (size.height / segmentHeight).toInt()
                        for (i in 0 until segments) {
                            val y = i * segmentHeight
                            val jitter = (if (i % 3 == 0) -10f else if (i % 7 == 0) 15f else -5f) * distortIntensity
                            clipRect(
                                left = currentOffset,
                                right = currentOffset + glitchWidth,
                                top = y,
                                bottom = y + segmentHeight,
                            ) {
                                withTransform({ translate(left = jitter) }) {
                                    scope.drawContent()
                                }
                            }
                        }
                    }
                }
        ) {
            ShadowOverlay(
                state = shadowHomeState,
                reminders = gameViewModel.reminders,
                radarIcons = radarIcons,
                scrollState = scrollState,
                progressPercent = progressPercent,
                onAxisTapped = { axisIndex ->
                    navController.navigate("field_score/$axisIndex?isShadow=true")
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (shadowRevealed) -1f else 1f)
                .drawWithContent {
                    val scope = this
                    val progress = (currentOffset / screenWidthPx).coerceIn(0f, 1f)
                    val distortIntensity = (1f - kotlin.math.abs(progress - 0.5f) * 2f).coerceIn(0f, 1f)
                    val glitchWidth = 32.dp.toPx() * distortIntensity
                    clipRect(right = currentOffset - glitchWidth) {
                        scope.drawContent()
                    }
                    if (distortIntensity > 0f) {
                        val segmentHeight = 10.dp.toPx()
                        val segments = (size.height / segmentHeight).toInt()
                        for (i in 0 until segments) {
                            val y = i * segmentHeight
                            val jitter = (if (i % 4 == 0) 12f else if (i % 7 == 0) -8f else 4f) * distortIntensity
                            clipRect(
                                left = currentOffset - glitchWidth,
                                right = currentOffset,
                                top = y,
                                bottom = y + segmentHeight,
                            ) {
                                withTransform({ translate(left = jitter) }) {
                                    scope.drawContent()
                                }
                            }
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(if (scrollState.value > 80) 0f else 2f)
                    .onSizeChanged { heroHeightPx = it.height }
                    .scrollable(
                        orientation = Orientation.Vertical,
                        state = rememberScrollableState { delta ->
                            -scrollState.dispatchRawDelta(-delta)
                        },
                    )
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HomeHeader(
                    userName = gameViewModel.player.name,
                    onProfileTap = { navController.navigate("profile") },
                    contentAlpha = headerAlpha,
                )
                RadarChartView(
                    scores = radarValues,
                    axisIconPainters = radarIcons,
                    modifier = Modifier.padding(top = 20.dp),
                    onAxisTapped = { axisIndex ->
                        navController.navigate("field_score/$axisIndex")
                    },
                    onChartTapped = {
                        if (scrollState.value > 0) {
                            coroutineScope.launch {
                                scrollState.animateScrollTo(0)
                            }
                        }
                    },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroDp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = scrollState.value > 0,
                        ) {
                            coroutineScope.launch {
                                scrollState.animateScrollTo(0)
                            }
                        },
                )
                PrimaryLayerContent(
                    tasks = gameViewModel.tasks.map {
                        TaskUi(it, ShadowTaskState.PENDING, it.status == TaskStatus.Completed)
                    },
                    reminders = gameViewModel.reminders,
                    progressPercent = progressPercent,
                    contentAlpha = contentAlpha,
                    onAddTask = {
                        coroutineScope.launch {
                            isNavBarTransitioning.value = true
                            delay(450.milliseconds)
                            showTaskDialog = true
                        }
                    },
                    onAddReminder = {
                        coroutineScope.launch {
                            isNavBarTransitioning.value = true
                            delay(450.milliseconds)
                            showReminderDialog = true
                        }
                    },
                    onRemoveReminder = { id ->
                        alarmScheduler.cancel(id)
                        gameViewModel.removeReminder(id)
                    },
                    onBadgesTap = { navController.navigate("badges") },
                    onCompleteTask = gameViewModel::completeTask,
                    onDeleteTask = { id ->
                        val task = gameViewModel.tasks.find { it.id == id }
                        if (task?.status == TaskStatus.Pending) {
                            taskToDelete = task
                        } else {
                            gameViewModel.deleteTask(id)
                        }
                    },
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(60.dp)
                .offset { IntOffset(currentOffset.roundToInt() - 30, 0) }
                .graphicsLayer {
                    val progress = (currentOffset / screenWidthPx).coerceIn(0f, 1f)
                    alpha = progress
                },
        ) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0.0f to Color.Transparent,
                    0.4f to ShadowPurple.copy(alpha = 0.25f),
                    0.5f to ShadowPurple.copy(alpha = 0.4f),
                    0.6f to ShadowPurple.copy(alpha = 0.25f),
                    1.0f to Color.Transparent,
                ),
                size = size,
            )
        }

        if (showTaskDialog) {
            AddTaskDialog(
                onDismiss = {
                    showTaskDialog = false
                    isNavBarTransitioning.value = false
                },
                onConfirm = { category, description, customTitle ->
                    suppressNextProgressionFeedback(context)
                    gameViewModel.addTask(category, description, customTitle)
                    showTaskDialog = false
                    isNavBarTransitioning.value = false
                },
            )
        }
        if (showReminderDialog) {
            AddReminderDialog(
                onDismiss = {
                    showReminderDialog = false
                    isNavBarTransitioning.value = false
                },
                onConfirm = { title, description, frequency ->
                    val time = kotlin.time.Instant.fromEpochMilliseconds(
                        System.currentTimeMillis() + if (frequency > 0) frequency * 60 * 1000L else 0L
                    )
                    val newReminder = Remainder.create(
                        title.trim(),
                        time,
                        description?.trim()?.ifBlank { null },
                        frequency,
                    )
                    alarmScheduler.cancel(gameViewModel.reminders.firstOrNull()?.id ?: "")
                    gameViewModel.reminders.forEach { gameViewModel.removeReminder(it.id) }
                    gameViewModel.addReminder(newReminder)
                    alarmScheduler.schedule(newReminder)
                    showReminderDialog = false
                    isNavBarTransitioning.value = false
                },
            )
        }

        taskToDelete?.let { task ->
            DeleteConfirmationDialog(
                onDismiss = { taskToDelete = null },
                onConfirm = {
                    gameViewModel.deleteTask(task.id)
                    taskToDelete = null
                },
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
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
            Text("Delete Task")
        },
        text = { Text("Are you sure you want to delete this task?") },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text("Proceed", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = KalliorColors.PrimaryLayer,
        titleContentColor = KalliorColors.NormalText,
        textContentColor = KalliorColors.MutedText,
    )
}

@Composable
fun ShadowOverlay(
    state: kallos.viewmodel.ShadowHomeState,
    reminders: List<Remainder>,
    radarIcons: List<androidx.compose.ui.graphics.painter.Painter>,
    scrollState: androidx.compose.foundation.ScrollState,
    progressPercent: Float,
    onAxisTapped: ((Int) -> Unit)? = null,
) {
    val scale by animateFloatAsState(
        targetValue = (1f - scrollState.value * 0.00035f).coerceIn(0.88f, 1f),
        label = "shadowRadarScale",
    )
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var heroHeightPx by remember { mutableIntStateOf(0) }
    val heroDp = with(density) { if (heroHeightPx == 0) 460.dp else heroHeightPx.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KalliorColors.CanvasBackground)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(if (scrollState.value > 80) 0f else 2f)
                .onSizeChanged { heroHeightPx = it.height }
                .scrollable(
                    orientation = Orientation.Vertical,
                    state = rememberScrollableState { delta ->
                        -scrollState.dispatchRawDelta(-delta)
                    },
                )
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeHeader(
                userName = "Shadow",
                onProfileTap = {},
                accentColor = ShadowPurple,
                interactionsEnabled = false,
            )
            val scores = state.scores
            val radarValues = listOf(scores.focus, scores.discipline, scores.health, scores.resilience, scores.consistency)
            RadarChartView(
                scores = radarValues,
                axisIconPainters = radarIcons,
                modifier = Modifier.padding(top = 20.dp),
                accentColor = ShadowPurple,
                onAxisTapped = onAxisTapped,
                onChartTapped = {
                    if (scrollState.value > 0) {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(0)
                        }
                    }
                },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroDp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = scrollState.value > 0,
                    ) {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(0)
                        }
                    },
            )
            SharedHomeSections(
                tasks = state.tasks,
                reminders = reminders,
                isShadow = true,
                progressPercent = (listOf(
                    state.scores.focus,
                    state.scores.discipline,
                    state.scores.health,
                    state.scores.resilience,
                    state.scores.consistency,
                ).average() / 100.0).toFloat(),
                onAddTask = { },
                onAddReminder = { },
                onRemoveReminder = { },
                onBadgesTap = { },
                onCompleteTask = { },
                onDeleteTask = { },
            )
        }
    }
}
