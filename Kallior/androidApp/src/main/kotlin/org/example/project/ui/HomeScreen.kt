package org.example.project.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import org.example.project.notification.AlarmScheduler
import org.example.project.notification.NotificationHelper
import kallos.model.Remainder
import kallos.model.Task
import kallos.model.TaskStatus
import kallos.viewmodel.GameViewModel
import kotlin.time.Clock
import org.example.project.R
import kallos.model.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import kallos.viewmodel.TaskUi
import org.example.project.ui.theme.*
import kallos.engine.ShadowTaskState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.semantics.semantics
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    gameViewModel: GameViewModel,
    onMenuClick: () -> Unit,
) {
    val shadowHomeState by gameViewModel.shadowHomeState.collectAsState()
    var showTaskDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

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
        targetValue = (1f - scrollState.value * 0.0005f).coerceIn(0.8f, 1f),
        label = "radarScale",
    )
    val slideEnter = tween<IntOffset>(300, easing = FastOutSlowInEasing)

    // Remap the backend RadarScores ([Consistency, Discipline, Focus, Health, Resilience])
    // into the chart's vertex order ([Focus, Discipline, Health, Resilience, Consistency]).
    val s = gameViewModel.userScores
    val radarValues = listOf(s.focus, s.discipline, s.health, s.resilience, s.consistency)

    val radarIcons = listOf(
        painterResource(R.drawable.focus_icon),
        painterResource(R.drawable.discipline),
        painterResource(R.drawable.health),
        painterResource(R.drawable.resilience),
        painterResource(R.drawable.consistency),
    )

    // Shadow Reveal State
    val density = LocalDensity.current
    val screenWidthPx = with(density) { context.resources.displayMetrics.widthPixels.toFloat() }
    val coroutineScope = rememberCoroutineScope()

    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = DragValue.Closed,
            anchors = DraggableAnchors {
                DragValue.Closed at screenWidthPx
                DragValue.Open at 0f
            },
            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
            velocityThreshold = { with(density) { 900.dp.toPx() } },
            snapAnimationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 380f),
            decayAnimationSpec = androidx.compose.animation.core.exponentialDecay()
        )
    }

    val currentOffset = draggableState.offset.takeIf { !it.isNaN() } ?: screenWidthPx

    // The drawWithContent clips below are visual only — Compose hit-testing ignores them,
    // so a hidden layer can still intercept taps/swipes aimed at the revealed one (e.g. the
    // shadow's "done" tick completing the user's task, or a shadow-side swipe opening the
    // user's delete panel). The revealed layer must therefore also be the FRONT layer for
    // pointer events: lift the shadow overlay above the primary while it is revealed (more
    // than halfway), and let the primary stay on top when closed.
    val shadowRevealed = currentOffset < screenWidthPx / 2f

    if (draggableState.targetValue == DragValue.Open || currentOffset < screenWidthPx) {
        BackHandler {
            coroutineScope.launch { draggableState.animateTo(DragValue.Closed) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .anchoredDraggable(
                state = draggableState,
                orientation = Orientation.Horizontal
            ),
    ) {
        // 1. Shadow Overlay (Stationary Background with Distortion)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (shadowRevealed) 1f else 0f)
                .drawWithContent {
                    val scope = this
                    val clipX = currentOffset
                    val progress = (clipX / screenWidthPx).coerceIn(0f, 1f)
                    val distortIntensity = (1f - kotlin.math.abs(progress - 0.5f) * 2f).coerceIn(0f, 1f)
                    val glitchWidth = 32.dp.toPx() * distortIntensity

                    // 1. Draw main shadow content (revealed from boundary)
                    clipRect(left = clipX + glitchWidth) {
                        scope.drawContent()
                    }

                    // 2. Draw "Glitch Zone" segments for Shadow
                    if (distortIntensity > 0f) {
                        val segmentHeight = 12.dp.toPx()
                        val segments = (size.height / segmentHeight).toInt()
                        for (i in 0 until segments) {
                            val y = i * segmentHeight
                            val jitter = (if (i % 3 == 0) -10f else if (i % 7 == 0) 15f else -5f) * distortIntensity
                            clipRect(
                                left = clipX,
                                right = clipX + glitchWidth,
                                top = y,
                                bottom = y + segmentHeight
                            ) {
                                withTransform({
                                    translate(left = jitter)
                                }) {
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
                slideEnter = slideEnter,
                radarIcons = radarIcons,
                scrollState = scrollState,
                onClose = {
                    coroutineScope.launch { draggableState.animateTo(DragValue.Closed) }
                }
            )
        }

        // 2. Primary Content (Stationary Clipped Foreground with Distortion)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val scope = this
                    val clipX = currentOffset
                    val progress = (clipX / screenWidthPx).coerceIn(0f, 1f)
                    val distortIntensity = (1f - kotlin.math.abs(progress - 0.5f) * 2f).coerceIn(0f, 1f)
                    val glitchWidth = 32.dp.toPx() * distortIntensity
                    
                    // 1. Draw main clean content (clipped to reveal edge)
                    clipRect(right = clipX - glitchWidth) {
                        scope.drawContent()
                    }

                    // 2. Draw "Glitch Zone" segments at the boundary
                    if (distortIntensity > 0f) {
                        val segmentHeight = 10.dp.toPx()
                        val segments = (size.height / segmentHeight).toInt()
                        
                        for (i in 0 until segments) {
                            val y = i * segmentHeight
                            // Deterministic jitter based on vertical position
                            val jitter = (if (i % 4 == 0) 12f else if (i % 7 == 0) -8f else 4f) * distortIntensity
                            
                            clipRect(
                                left = clipX - glitchWidth,
                                right = clipX,
                                top = y,
                                bottom = y + segmentHeight
                            ) {
                                withTransform({
                                    translate(left = jitter)
                                }) {
                                    scope.drawContent()
                                }
                            }
                        }
                    }
                }
        ) {
            TopNavBar(
                onMenuClick = onMenuClick,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
                    .padding(top = 48.dp),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                RadarChartView(
                    scores = radarValues,
                    axisIconPainters = radarIcons,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                Spacer(modifier = Modifier.height(360.dp))
                PrimaryLayerContent(
                    gameViewModel = gameViewModel,
                    reminders = gameViewModel.reminders,
                    onAddTask = { showTaskDialog = true },
                    onAddReminder = { showReminderDialog = true },
                    onRemoveReminder = { id ->
                        alarmScheduler.cancel(id)
                        gameViewModel.removeReminder(id)
                    },
                    onBadgesTap = { navController.navigate("badges") },
                    onCompleteTask = { id -> gameViewModel.completeTask(id) },
                    onDeleteTask = { id ->
                        val task = gameViewModel.tasks.find { it.id == id }
                        if (task?.status == TaskStatus.Pending) {
                            taskToDelete = task
                        } else {
                            gameViewModel.deleteTask(id)
                        }
                    },
                    slideEnter = slideEnter,
                )
            }
        }

        // 3. Distortion Glow (Fades as we swipe left)
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(60.dp)
                .offset { IntOffset(currentOffset.roundToInt() - 30, 0) }
                .graphicsLayer { 
                    val progress = (currentOffset / screenWidthPx).coerceIn(0f, 1f)
                    alpha = progress 
                }
        ) {
            // Subtle vertical glow centered on the "tear"
            drawRect(
                brush = Brush.horizontalGradient(
                    0.0f to Color.Transparent,
                    0.4f to ShadowPurple.copy(alpha = 0.25f),
                    0.5f to ShadowPurple.copy(alpha = 0.4f),
                    0.6f to ShadowPurple.copy(alpha = 0.25f),
                    1.0f to Color.Transparent,
                ),
                size = size
            )
        }

        if (showTaskDialog) {
            AddTaskDialog(
                onDismiss = { showTaskDialog = false },
                onConfirm = { category, description, customTitle ->
                    gameViewModel.addTask(category, description, customTitle)
                    showTaskDialog = false
                },
            )
        }
        if (showReminderDialog) {
            AddReminderDialog(
                onDismiss = { showReminderDialog = false },
                onConfirm = { title, description, frequency ->
                    val time = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis() + if(frequency > 0) frequency * 60 * 1000L else 0L)
                    val newReminder = Remainder.create(
                        title.trim(),
                        time,
                        description?.trim()?.ifBlank { null },
                        frequency
                    )
                    // Only one reminder allowed - replace any existing one
                    alarmScheduler.cancel(gameViewModel.reminders.firstOrNull()?.id ?: "")
                    gameViewModel.reminders.forEach { gameViewModel.removeReminder(it.id) }
                    gameViewModel.addReminder(newReminder)
                    alarmScheduler.schedule(newReminder)
                    showReminderDialog = false
                },
            )
        }

        taskToDelete?.let { task ->
            DeleteConfirmationDialog(
                onDismiss = { taskToDelete = null },
                onConfirm = {
                    gameViewModel.deleteTask(task.id)
                    taskToDelete = null
                }
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
        title = { Text("Delete Task") },
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
private fun TopNavBar(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.side_bar_button),
            contentDescription = "Menu",
            tint = Color.White,
            modifier = Modifier
                .size(36.dp)
                .clickable { onMenuClick() }
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KalliorColors.PrimaryLayer),
        )
    }
}

@Composable
private fun PrimaryLayerContent(
    gameViewModel: GameViewModel,
    reminders: List<kallos.model.Remainder>,
    onAddTask: () -> Unit,
    onAddReminder: () -> Unit,
    onRemoveReminder: (String) -> Unit,
    onBadgesTap: () -> Unit,
    onCompleteTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    slideEnter: androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.IntOffset>,
) {
    SharedHomeSections(
        tasks = gameViewModel.tasks.map { TaskUi(it, ShadowTaskState.PENDING, it.status == kallos.model.TaskStatus.Completed) },
        reminders = reminders,
        isShadow = false,
        onAddTask = onAddTask,
        onAddReminder = onAddReminder,
        onRemoveReminder = onRemoveReminder,
        onBadgesTap = onBadgesTap,
        onCompleteTask = onCompleteTask,
        onDeleteTask = onDeleteTask,
        slideEnter = slideEnter
    )
}

@Composable
private fun SharedHomeSections(
    tasks: List<TaskUi>,
    reminders: List<kallos.model.Remainder>,
    isShadow: Boolean,
    onAddTask: () -> Unit,
    onAddReminder: () -> Unit,
    onRemoveReminder: (String) -> Unit,
    onBadgesTap: () -> Unit,
    onCompleteTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    slideEnter: androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.IntOffset>,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 150.dp, topEnd = 150.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = if (isShadow) {
                            listOf(ShadowGradientTop, ShadowGradientBot)
                        } else {
                            listOf(KalliorColors.DarkBrown, Color.Black)
                        }
                    )
                )
                .padding(top = 130.dp, bottom = 50.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(60.dp),
            ) {
                SectionView(
                    title = "Tasks",
                    subtitle = if (isShadow) {
                        val count = tasks.count { it.shadowState == ShadowTaskState.DONE }
                        if (count > 0) "$count tasks completed" else null
                    } else null,
                    buttonLabel = "+",
                    onButtonTap = {
                        if (!isShadow) {
                            onAddTask()
                        }
                    },
                    contentEndPadding = 20.dp,
                    emptyText = if (tasks.isEmpty()) "Click on + to add a\nnew task" else null,
                    buttonColor = if (isShadow) ShadowButton else KalliorColors.PrimaryLayer,
                    buttonGlyphColor = if (isShadow) ShadowButtonGlyph else Color.White,
                    enabled = !isShadow,
                ) {
                    val ordered = tasks.sortedWith(compareBy { it.task.status == kallos.model.TaskStatus.Completed })
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(tasksHeight(ordered.size))
                            .clipToBounds(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        userScrollEnabled = false,
                    ) {
                        items(items = ordered, key = { it.task.id }) { taskUi ->
                            SwipeableTaskItem(
                                task = taskUi.task,
                                taskUi = taskUi,
                                interactionsEnabled = !isShadow,
                                onComplete = { onCompleteTask(taskUi.task.id) },
                                onDelete = { onDeleteTask(taskUi.task.id) },
                                modifier = Modifier
                                    .animateItem(placementSpec = slideEnter),
                            )
                        }
                    }
                }

                SectionView(
                    title = "Reminders",
                    buttonLabel = "+",
                    onButtonTap = {
                        if (!isShadow) {
                            onAddReminder()
                        }
                    },
                    contentEndPadding = 12.dp,
                    emptyText = if (reminders.isEmpty()) "Reminder-free mind!" else null,
                    buttonColor = if (isShadow) ShadowButton else KalliorColors.PrimaryLayer,
                    buttonGlyphColor = if (isShadow) ShadowButtonGlyph else Color.White,
                    enabled = !isShadow,
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(remindersHeight(reminders.size))
                            .clipToBounds(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        userScrollEnabled = false,
                    ) {
                        items(items = reminders, key = { it.id }) { reminder ->
                            SwipeableReminderItem(
                                reminder = reminder,
                                interactionsEnabled = !isShadow,
                                onDelete = { onRemoveReminder(reminder.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }

                BadgesCard(onTap = onBadgesTap, enabled = !isShadow)
                SleepScheduleCard(enabled = !isShadow)
            }
        }
    }
}

/** Generous height so the nested [LazyColumn] lays out all rows without an inner scroll. */
private fun tasksHeight(count: Int): androidx.compose.ui.unit.Dp {
    if (count <= 0) return 0.dp
    val row = 120.dp
    val gap = 4.dp
    return row * count + gap * (count - 1) + 24.dp
}

@Composable
private fun BadgesCard(
    onTap: () -> Unit,
    enabled: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Badges",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = Philosopher,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = KalliorColors.NormalText,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = KalliorColors.NormalText,
                modifier = Modifier.size(28.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 140.dp)
                .border(
                    BorderStroke(1.dp, Color(0xFF2C2C2C)),
                    RoundedCornerShape(24.dp)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Transparent)
                .clickable(enabled = enabled, onClick = onTap)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Wow, such empty!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Philosopher
                ),
                color = KalliorColors.MutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private enum class DragValue { Closed, Open }

/**
 * Wraps [TaskItemRow] so the card can be swiped to the right to reveal a red delete
 * affordance. The card stays open once swiped past the halfway point; it does NOT delete
 * on release. The task is only removed when the user taps the bin icon. Swiping back
 * to the start closes the card again. While dragging, the circular progress ring fades
 * out (its alpha is driven by the drag distance) so the card visibly "empties" as the
 * delete is revealed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableTaskItem(
    task: kallos.model.Task,
    taskUi: TaskUi? = null,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    interactionsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val completed = taskUi?.shadowCompleted ?: (task.status == kallos.model.TaskStatus.Completed)
    if (!interactionsEnabled || completed) {
        // Read-only row (shadow side): no swipe-to-delete, tick always locked.
        TaskItemRow(
            task = task,
            completed = completed,
            onComplete = onComplete,
            tickEnabled = interactionsEnabled,
            modifier = modifier,
        )
        return
    }

    val density = LocalDensity.current
    val maxOffsetPx = with(density) { 120.dp.toPx() }

    val state = remember(task.id) {
        AnchoredDraggableState(
            initialValue = DragValue.Closed,
            anchors = DraggableAnchors {
                DragValue.Closed at 0f
                DragValue.Open at maxOffsetPx
            },
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            decayAnimationSpec = exponentialDecay()
        )
    }

    val currentOffset = state.requireOffset()
    val ringFraction = (currentOffset / maxOffsetPx).coerceIn(0f, 1f)

    val panelColor = task.category.toColor()
    val iconColor = panelColor.contrastColor()

    val gapPx = with(density) { 8.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
    ) {
        if (currentOffset > gapPx) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(with(density) { (currentOffset - gapPx).toDp() })
                    .clip(RoundedCornerShape(12.dp))
                    .background(panelColor)
                    .clickable { onDelete() },
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete task",
                    tint = iconColor,
                    modifier = Modifier.padding(start = 20.dp),
                )
            }
        }

        TaskItemRow(
            task = task,
            completed = completed,
            onComplete = onComplete,
            ringAlpha = 1f - ringFraction,
            modifier = Modifier
                .offset { IntOffset(currentOffset.roundToInt(), 0) }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal
                ),
        )
    }
}

@Composable
private fun TaskItemRow(
    task: kallos.model.Task,
    completed: Boolean,
    onComplete: () -> Unit,
    ringAlpha: Float = 1f,
    tickEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val nowMs = rememberNowMs(task)
    val windowMs = task.estimateMinutes * 60_000L
    val shelvedAtMs = task.shelvedAt?.toEpochMilliseconds() ?: 0L
    val elapsed = (nowMs - shelvedAtMs).coerceAtLeast(0L)
    val progress = if (windowMs > 0L) (elapsed.toFloat() / windowMs).coerceIn(0f, 1f) else 1f
    val enabled = progress >= 1f && !completed && tickEnabled

    val alpha by animateFloatAsState(if (completed) 0.45f else 1f, label = "taskAlpha")
    val grayMatrix = remember { ColorMatrix().apply { setToSaturation(0f) } }
    val colorFilter = if (completed) {
        remember(grayMatrix) { ColorFilter.colorMatrix(grayMatrix) }
    } else {
        null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
            .graphicsLayer { this.alpha = alpha; this.colorFilter = colorFilter },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Vertical Color Indicator
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(task.category.toColor())
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(task.category.iconRes()),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(KalliorColors.NormalText),
                    modifier = Modifier.size(16.dp)
                )

                val displayTitle = if (task.category == Category.Other && task.title != task.category.displayName) {
                    "Other: ${task.title}"
                } else {
                    task.title
                }

                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = KalliorColors.NormalText,
                    maxLines = 1,
                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                )
            }
            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = KalliorColors.MutedText,
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        TickButton(
            progress = progress,
            enabled = enabled,
            completed = completed,
            ringAlpha = ringAlpha,
            lockedDot = !tickEnabled,
            onClick = onComplete,
        )
    }
}

private fun Category.toColor(): Color = when (this) {
    Category.Exercise -> Color(0xFFB80E0C)
    Category.Work -> Color(0xFF066DB1)
    Category.Meditation -> Color(0xFF65C8CB)
    Category.Diet -> Color(0xFFA0DB2A)
    Category.Other -> Color(0xFFDDDDDD)
}

/** Returns black or white depending on the background luminance, for readable icon contrast. */
private fun Color.contrastColor(): Color {
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
    return if (luminance > 0.5f) Color.Black else Color.White
}

/**
 * Drives a recomposing clock while a task is still [TaskStatus.Shelved] so the circular timer ring
 * animates smoothly. Time is derived from [Clock.System] (not a running counter), so the progress
 * is correct even after the app is backgrounded and later resumed.
 */
@Composable
private fun rememberNowMs(task: Task): Long {
    var nowMs by remember(task.id) { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    val windowMs = task.estimateMinutes * 60_000L
    val shelvedAtMs = task.shelvedAt?.toEpochMilliseconds() ?: 0L
    LaunchedEffect(task.id, task.status) {
        if (task.status == TaskStatus.Shelved) {
            while (true) {
                val cur = Clock.System.now().toEpochMilliseconds()
                nowMs = cur
                if (cur - shelvedAtMs >= windowMs) break
                delay(1000)
            }
        } else {
            nowMs = Clock.System.now().toEpochMilliseconds()
        }
    }
    return nowMs
}

/**
 * Circular tick/checkbox. While the task is shelving, the progress ring fills 0% -> 100% over the
 * timer window and the button is locked (dimmed + lock glyph). Once the ring is full the button
 * becomes enabled (bright accent) and a tap completes the task.
 *
 * [lockedDot] renders the enabled-style dot + ring but in grey instead of the lock glyph — used on
 * the shadow side so the tick reads as locked without a literal lock icon.
 */
@Composable
private fun TickButton(
    progress: Float,
    enabled: Boolean,
    completed: Boolean,
    ringAlpha: Float = 1f,
    lockedDot: Boolean = false,
    onClick: () -> Unit,
) {
    val size = 44.dp
    val stroke = 2.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .graphicsLayer { this.alpha = ringAlpha }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val diameter = size.toPx() - strokePx
            val r = diameter / 2f
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val topLeft = Offset(center.x - r, center.y - r)

            // Locked-dot state (shadow side): same shape as the enabled tick, but grey.
            val dotState = lockedDot && !enabled && !completed
            val accent = if (dotState) KalliorColors.MutedText else KalliorColors.AccentOrange

            drawCircle(
                color = KalliorColors.MutedText.copy(alpha = if (enabled || completed || dotState) 0.25f else 0.4f),
                radius = r,
                style = Stroke(width = strokePx),
            )
            if (progress > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }

            when {
                completed -> drawCheck(center, r)
                enabled -> drawCircle(color = KalliorColors.AccentOrange, radius = r * 0.45f)
                dotState -> drawCircle(color = KalliorColors.MutedText, radius = r * 0.45f)
                else -> drawLock(center, r)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCheck(center: Offset, r: Float) {
    val len = r * 0.5f
    val color = KalliorColors.AccentOrange
    val stroke = Stroke(width = r * 0.22f, cap = StrokeCap.Round)
    val p1 = Offset(center.x - len * 0.6f, center.y)
    val p2 = Offset(center.x - len * 0.1f, center.y + len * 0.5f)
    val p3 = Offset(center.x + len * 0.7f, center.y - len * 0.5f)
    drawLine(color, p1, p2, stroke.width, stroke.cap)
    drawLine(color, p2, p3, stroke.width, stroke.cap)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLock(center: Offset, r: Float) {
    val dim = KalliorColors.MutedText
    val bodyW = r * 0.9f
    val bodyH = r * 0.7f
    val bodyTop = center.y - bodyH * 0.1f
    drawRoundRect(
        color = dim,
        topLeft = Offset(center.x - bodyW / 2f, bodyTop),
        size = Size(bodyW, bodyH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            x = r * 0.18f,
            y = r * 0.18f,
        ),
    )
    drawArc(
        color = dim,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - bodyW * 0.32f, bodyTop - bodyH * 0.5f),
        size = Size(bodyW * 0.64f, bodyH),
        style = Stroke(width = r * 0.18f, cap = StrokeCap.Round),
    )
}

@Composable
private fun ReminderItemRow(
    reminder: Remainder,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.titleSmall,
                color = KalliorColors.NormalText,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            val detail = buildString {
                append(formatReminderTime(reminder.time))
                reminder.description?.let { append(" · $it") }
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = KalliorColors.MutedText,
            )
        }
    }
}

private fun formatReminderTime(time: kotlin.time.Instant): String {
    val date = Date(time.toEpochMilliseconds())
    return SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date)
}

/** Fixed height so the nested [LazyColumn] lays out all rows without an inner scroll. */
private fun remindersHeight(count: Int): androidx.compose.ui.unit.Dp {
    if (count <= 0) return 0.dp
    val row = 64.dp
    val gap = 4.dp
    return row * count + gap * (count - 1) + 16.dp
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableReminderItem(
    reminder: Remainder,
    onDelete: () -> Unit,
    interactionsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (!interactionsEnabled) {
        // Read-only row (shadow side): no swipe-to-delete.
        ReminderItemRow(reminder = reminder, modifier = modifier)
        return
    }

    val density = LocalDensity.current
    val maxOffsetPx = with(density) { 120.dp.toPx() }
    val state = remember(reminder.id) {
        AnchoredDraggableState(
            initialValue = DragValue.Closed,
            anchors = DraggableAnchors {
                DragValue.Closed at 0f
                DragValue.Open at maxOffsetPx
            },
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            decayAnimationSpec = exponentialDecay()
        )
    }

    val currentOffset = state.requireOffset()

    val panelColor = KalliorColors.AccentOrange
    val iconColor = panelColor.contrastColor()

    val gapPx = with(density) { 8.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
    ) {
        if (currentOffset > gapPx) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(with(density) { (currentOffset - gapPx).toDp() })
                    .clip(RoundedCornerShape(12.dp))
                    .background(panelColor)
                    .clickable { onDelete() },
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete reminder",
                    tint = iconColor,
                    modifier = Modifier.padding(start = 20.dp),
                )
            }
        }

        ReminderItemRow(
            reminder = reminder,
            modifier = Modifier
                .offset { IntOffset(currentOffset.roundToInt(), 0) }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal
                ),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.ShadowOverlay(
    state: kallos.viewmodel.ShadowHomeState,
    reminders: List<kallos.model.Remainder>,
    slideEnter: androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.IntOffset>,
    radarIcons: List<androidx.compose.ui.graphics.painter.Painter>,
    scrollState: androidx.compose.foundation.ScrollState,
    onClose: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = (1f - scrollState.value * 0.0005f).coerceIn(0.8f, 1f),
        label = "shadowRadarScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Bar - Aligned with TopNavBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(1f)
                .padding(top = 48.dp)
                .padding(horizontal = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.side_bar_button),
                contentDescription = "Close Shadow Homescreen",
                tint = ShadowPurple,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onClose() }
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Transparent)
            )
        }

        // Radar Layer - Matches Home structure
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            val s = state.scores
            val radarValues = listOf(s.focus, s.discipline, s.health, s.resilience, s.consistency)
            RadarChartView(
                scores = radarValues,
                axisIconPainters = radarIcons,
                modifier = Modifier.padding(top = 16.dp),
                accentColor = ShadowPurple
            )
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            Spacer(modifier = Modifier.height(360.dp))

            SharedHomeSections(
                tasks = state.tasks,
                reminders = reminders,
                isShadow = true,
                onAddTask = { },
                onAddReminder = { },
                onRemoveReminder = { },
                onBadgesTap = { },
                onCompleteTask = { },
                onDeleteTask = { },
                slideEnter = slideEnter
            )
        }
    }
}

