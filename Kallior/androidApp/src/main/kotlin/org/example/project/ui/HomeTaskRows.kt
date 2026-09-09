package org.example.project.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kallos.model.Category
import kallos.model.Remainder
import kallos.model.Task
import kallos.model.TaskStatus
import kallos.viewmodel.TaskUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

internal enum class HomeDragValue { Closed, Open }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SwipeableTaskItem(
    task: Task,
    modifier: Modifier = Modifier,
    taskUi: TaskUi? = null,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    interactionsEnabled: Boolean = true,
) {
    val completed = taskUi?.shadowCompleted ?: (task.status == TaskStatus.Completed)
    if (!interactionsEnabled || completed) {
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
            initialValue = HomeDragValue.Closed,
            anchors = DraggableAnchors {
                HomeDragValue.Closed at 0f
                HomeDragValue.Open at maxOffsetPx
            },
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            decayAnimationSpec = exponentialDecay(),
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
            .height(IntrinsicSize.Min)
    ) {
        if (currentOffset > gapPx) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(with(density) { (currentOffset - gapPx).toDp() })
                    .clip(RoundedCornerShape(16.dp))
                    .background(panelColor.copy(alpha = 0.35f + ringFraction * 0.65f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete task",
                    tint = iconColor,
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .graphicsLayer { alpha = ringFraction },
                )
            }
        }

        TaskItemRow(
            task = task,
            completed = false,
            onComplete = onComplete,
            ringAlpha = 1f - ringFraction,
            modifier = Modifier
                .offset { IntOffset(currentOffset.roundToInt(), 0) }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                ),
        )
    }
}

@Composable
private fun TaskItemRow(
    task: Task,
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

    val alpha by animateFloatAsState(if (completed) 0.5f else 1f, label = "taskAlpha")
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(completed) {
        if (completed) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
    }
    val grayMatrix = remember { ColorMatrix().apply { setToSaturation(0f) } }
    val colorFilter = if (completed) {
        remember(grayMatrix) { ColorFilter.colorMatrix(grayMatrix) }
    } else {
        null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(KalliorColors.SurfaceCharcoal)
            .drawCompletionSweep(sweep.value)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
            .graphicsLayer { this.alpha = alpha; this.colorFilter = colorFilter }
            .semantics { contentDescription = task.title },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TickButton(
            progress = progress,
            enabled = enabled,
            completed = completed,
            ringAlpha = ringAlpha,
            lockedDot = !tickEnabled,
            onClick = onComplete,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(task.category.iconRes()),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(KalliorColors.MutedText),
                    modifier = Modifier.size(14.dp),
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
                    text = parseFormattedText(task.description),
                    style = MaterialTheme.typography.labelSmall,
                    color = KalliorColors.MutedText,
                    fontSize = 11.sp,
                )
            }
        }
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(task.category.toColor().copy(alpha = 0.85f)),
        )
    }
}

private fun Modifier.drawCompletionSweep(progress: Float): Modifier {
    if (progress <= 0f || progress >= 1f) return this
    return graphicsLayer { }
        .then(
            Modifier.background(
                Brush.horizontalGradient(
                    0f to Color.Transparent,
                    (progress - 0.12f).coerceAtLeast(0f) to KalliorColors.AccentOrange.copy(alpha = 0.10f),
                    progress to KalliorColors.AccentOrange.copy(alpha = 0.22f),
                    (progress + 0.12f).coerceAtMost(1f) to Color.Transparent,
                )
            )
        )
}

internal fun Category.toColor(): Color = when (this) {
    Category.Exercise -> Color(0xFFB80E0C)
    Category.Work -> Color(0xFF066DB1)
    Category.Meditation -> Color(0xFF65C8CB)
    Category.Diet -> Color(0xFFA0DB2A)
    Category.Other -> Color(0xFFDDDDDD)
}

internal fun Color.contrastColor(): Color {
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
    return if (luminance > 0.5f) Color.Black else Color.White
}

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
                delay(1.seconds)
            }
        } else {
            nowMs = Clock.System.now().toEpochMilliseconds()
        }
    }
    return nowMs
}

@Composable
internal fun TickButton(
    progress: Float,
    enabled: Boolean,
    completed: Boolean,
    ringAlpha: Float = 1f,
    lockedDot: Boolean = false,
    onClick: () -> Unit,
) {
    val size = 28.dp
    val stroke = 1.6.dp
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .graphicsLayer { this.alpha = ringAlpha }
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                contentDescription = when {
                    completed -> "Task completed"
                    enabled -> "Mark task complete"
                    else -> "Task locked until timer finishes"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val diameter = size.toPx() - strokePx
            val r = diameter / 2f
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val topLeft = Offset(center.x - r, center.y - r)
            val dotState = lockedDot && !enabled && !completed
            val accent = if (dotState) KalliorColors.MutedText else KalliorColors.AccentOrange

            drawCircle(
                color = KalliorColors.MutedText.copy(alpha = if (enabled || completed || dotState) 0.28f else 0.4f),
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
                completed -> {
                    drawCircle(color = accent, radius = r)
                    drawCheck(center, r, Color.White)
                }
                enabled -> Unit
                dotState -> drawCircle(color = KalliorColors.MutedText, radius = r * 0.28f)
                else -> drawLock(center, r)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCheck(center: Offset, r: Float, color: Color) {
    val len = r * 0.5f
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
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(x = r * 0.18f, y = r * 0.18f),
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
internal fun ReminderItemRow(
    reminder: Remainder,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KalliorColors.SurfaceCharcoal)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(KalliorColors.AccentOrange.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = KalliorColors.AccentOrange,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.titleSmall,
                color = KalliorColors.NormalText,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatReminderTime(reminder.time),
                style = MaterialTheme.typography.labelSmall,
                color = KalliorColors.MutedText,
            )
            reminder.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = parseFormattedText(desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = KalliorColors.MutedText,
                    )
                }
            }
        }
    }
}

internal fun formatReminderTime(time: kotlin.time.Instant): String {
    val date = Date(time.toEpochMilliseconds())
    return SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SwipeableReminderItem(
    reminder: Remainder,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    interactionsEnabled: Boolean = true,
) {
    if (!interactionsEnabled) {
        ReminderItemRow(reminder = reminder, modifier = modifier)
        return
    }

    val density = LocalDensity.current
    val maxOffsetPx = with(density) { 120.dp.toPx() }
    val state = remember(reminder.id) {
        AnchoredDraggableState(
            initialValue = HomeDragValue.Closed,
            anchors = DraggableAnchors {
                HomeDragValue.Closed at 0f
                HomeDragValue.Open at maxOffsetPx
            },
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            decayAnimationSpec = exponentialDecay(),
        )
    }

    val currentOffset = state.requireOffset()
    val panelColor = KalliorColors.AccentOrange
    val iconColor = panelColor.contrastColor()
    val gapPx = with(density) { 8.dp.toPx() }
    val ringFraction = (currentOffset / maxOffsetPx).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        if (currentOffset > gapPx) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(with(density) { (currentOffset - gapPx).toDp() })
                    .clip(RoundedCornerShape(16.dp))
                    .background(panelColor.copy(alpha = 0.35f + ringFraction * 0.65f))
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
                    orientation = Orientation.Horizontal,
                ),
        )
    }
}
