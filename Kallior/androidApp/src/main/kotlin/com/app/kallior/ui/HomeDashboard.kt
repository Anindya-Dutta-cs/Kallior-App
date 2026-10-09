package com.app.kallior.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kallos.model.Remainder
import kallos.model.TaskStatus
import kallos.viewmodel.TaskUi
import com.app.kallior.ui.theme.ShadowButton
import com.app.kallior.ui.theme.ShadowGradientBot
import com.app.kallior.ui.theme.ShadowGradientTop
import kotlin.math.roundToInt

@Composable
internal fun SharedHomeSections(
    tasks: List<TaskUi>,
    reminders: List<Remainder>,
    isShadow: Boolean,
    progressPercent: Float,
    onAddTask: () -> Unit,
    onAddReminder: () -> Unit,
    onRemoveReminder: (String) -> Unit,
    onBadgesTap: () -> Unit,
    onCompleteTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    contentAlpha: Float = 1f,
) {
    val accent = if (isShadow) ShadowButton else KalliorColors.AccentOrange

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cornerRadius = this.maxWidth * 0.225f
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = contentAlpha }
                .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
                .background(
                    Brush.verticalGradient(
                        colorStops = if (isShadow) {
                            arrayOf(
                                0.0f to ShadowGradientTop,
                                0.22f to ShadowGradientBot,
                                1.0f to ShadowGradientBot,
                            )
                        } else {
                            arrayOf(
                                0.0f to KalliorColors.DarkBrown,
                                0.18f to KalliorColors.CanvasBackground,
                                1.0f to KalliorColors.CanvasBackground,
                            )
                        }
                    )
                )
                .padding(top = 28.dp, bottom = 220.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                DailyProgressArcSection(
                    progressPercent = progressPercent,
                    accent = accent,
                )

                HomeSectionHeader(
                    title = "Tasks",
                    onAddClick = if (isShadow) null else onAddTask,
                )
                if (tasks.isEmpty()) {
                    CompactEmptyState(
                        title = "No tasks for today",
                        subtitle = "Take a moment to plan your next move",
                        actionLabel = if (isShadow) null else "Add task",
                        onAction = onAddTask,
                    )
                } else {
                    val ordered = tasks.sortedWith(compareBy { it.task.status == TaskStatus.Completed })
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ordered.forEach { taskUi ->
                            SwipeableTaskItem(
                                task = taskUi.task,
                                taskUi = taskUi,
                                interactionsEnabled = !isShadow,
                                onComplete = { onCompleteTask(taskUi.task.id) },
                                onDelete = { onDeleteTask(taskUi.task.id) },
                            )
                        }
                    }
                }

                HomeSectionHeader(
                    title = "Reminders",
                    onAddClick = if (isShadow) null else onAddReminder,
                )
                if (reminders.isEmpty()) {
                    CompactEmptyState(
                        title = "No reminders yet",
                        subtitle = "Keep something important on your radar",
                        actionLabel = if (isShadow) null else "Add reminder",
                        onAction = onAddReminder,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reminders.forEach { reminder ->
                            SwipeableReminderItem(
                                reminder = reminder,
                                interactionsEnabled = !isShadow,
                                onDelete = { onRemoveReminder(reminder.id) },
                            )
                        }
                    }
                }

                HomeSectionHeader(
                    title = "Badges",
                    actionLabel = "See all",
                    onAction = if (isShadow) null else onBadgesTap,
                )
                BadgePreview(
                    enabled = !isShadow,
                    onTap = onBadgesTap,
                )

                SleepScheduleCard(enabled = !isShadow)
            }
        }
    }
}

@Composable
private fun DailyProgressArcSection(
    progressPercent: Float,
    accent: Color,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "dailyArcProgress",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Today's progress",
            color = KalliorColors.MutedText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .width(220.dp)
                .height(130.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                val strokeWidth = 14.dp.toPx()
                val arcWidth = size.width - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(arcWidth, arcWidth)

                val startAngle = 180f
                val sweepAngle = 180f

                // Track (Background Arc)
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )

                // Ambient glow layer under active progress
                if (animatedProgress > 0f) {
                    drawArc(
                        color = accent.copy(alpha = 0.25f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth + 6.dp.toPx(), cap = StrokeCap.Round),
                    )

                    // Foreground Progress Arc
                    drawArc(
                        color = accent,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedCountText(
                    targetValue = progressPercent.coerceIn(0f, 1f) * 100f,
                    format = { "${it.roundToInt()}%" },
                    style = TextStyle(
                        fontFamily = Philosopher,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                    ),
                    color = KalliorColors.NormalText,
                    label = "TodayProgressPercent",
                )
            }
        }
    }
}

@Composable
internal fun HomeSectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onAddClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontFamily = Philosopher,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = KalliorColors.NormalText,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (actionLabel != null && onAction != null) {
                Row(
                    modifier = Modifier.clickable(onClick = onAction),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = actionLabel,
                        color = KalliorColors.MutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = KalliorColors.MutedText,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (onAddClick != null) {
                SectionAddButton(
                    onClick = onAddClick,
                    contentDescription = "Add $title",
                )
            }
        }
    }
}

@Composable
private fun SectionAddButton(
    onClick: () -> Unit,
    contentDescription: String,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "addButtonScale",
    )

    Box(
        modifier = Modifier
            .size(34.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(KalliorColors.AccentOrange.copy(alpha = 0.16f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = KalliorColors.AccentOrange,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CompactEmptyState(
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(KalliorColors.SurfaceCharcoal)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = title, color = KalliorColors.NormalText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(text = subtitle, color = KalliorColors.MutedText, fontSize = 13.sp)
        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "+ $actionLabel",
                color = KalliorColors.AccentOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

@Composable
private fun BadgePreview(
    enabled: Boolean,
    onTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(KalliorColors.SurfaceCharcoal)
            .clickable(enabled = enabled, onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .drawBehind {
                        drawRoundRect(
                            color = KalliorColors.AccentOrange.copy(alpha = 0.18f - index * 0.04f),
                            cornerRadius = CornerRadius(size.minDimension / 2f, size.minDimension / 2f),
                            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
                        )
                    },
            )
        }
        Text(
            text = "Your first badge is waiting.",
            color = KalliorColors.MutedText,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun PrimaryLayerContent(
    tasks: List<TaskUi>,
    reminders: List<Remainder>,
    progressPercent: Float,
    onAddTask: () -> Unit,
    onAddReminder: () -> Unit,
    onRemoveReminder: (String) -> Unit,
    onBadgesTap: () -> Unit,
    onCompleteTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    contentAlpha: Float = 1f,
) {
    SharedHomeSections(
        tasks = tasks,
        reminders = reminders,
        isShadow = false,
        progressPercent = progressPercent,
        onAddTask = onAddTask,
        onAddReminder = onAddReminder,
        onRemoveReminder = onRemoveReminder,
        onBadgesTap = onBadgesTap,
        onCompleteTask = onCompleteTask,
        onDeleteTask = onDeleteTask,
        contentAlpha = contentAlpha,
    )
}
