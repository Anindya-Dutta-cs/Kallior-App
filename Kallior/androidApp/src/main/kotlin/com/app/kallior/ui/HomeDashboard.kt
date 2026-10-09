package com.app.kallior.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kallos.model.Remainder
import kallos.model.TaskStatus
import kallos.viewmodel.TaskUi
import com.app.kallior.ui.theme.ShadowButton
import com.app.kallior.ui.theme.ShadowButtonGlyph
import com.app.kallior.ui.theme.ShadowGradientBot
import com.app.kallior.ui.theme.ShadowGradientTop

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
    var showAddChooser by remember { mutableStateOf(false) }
    val accent = if (isShadow) ShadowButton else KalliorColors.AccentOrange
    val glyph = if (isShadow) ShadowButtonGlyph else Color.White

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
                                0.0f to KalliorColors.DarkBrown.copy(alpha = 0.55f),
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
                DailyProgressCard(
                    progressPercent = progressPercent,
                    accent = accent,
                    glyph = glyph,
                    enabled = !isShadow,
                    onAddClick = { if (!isShadow) showAddChooser = true },
                )

                HomeSectionHeader(
                    title = "Tasks",
                    actionLabel = null,
                    onAction = null,
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
                    actionLabel = null,
                    onAction = null,
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

        if (showAddChooser && !isShadow) {
            Dialog(
                onDismissRequest = { showAddChooser = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                AddActionChooser(
                    onTask = {
                        showAddChooser = false
                        onAddTask()
                    },
                    onReminder = {
                        showAddChooser = false
                        onAddReminder()
                    },
                    onDismiss = { showAddChooser = false },
                )
            }
        }
    }
}

@Composable
private fun DailyProgressCard(
    progressPercent: Float,
    accent: Color,
    glyph: Color,
    enabled: Boolean,
    onAddClick: () -> Unit,
) {
    val animated by animateFloatAsState(
        targetValue = progressPercent.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "dailyProgress",
    )
    val percentLabel = (animated * 100f).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KalliorColors.SurfaceCharcoal)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Today's progress",
                color = KalliorColors.MutedText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$percentLabel%",
                color = KalliorColors.NormalText,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animated.coerceAtLeast(0.02f))
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        GlowingPlusButton(
            accent = accent,
            glyph = glyph,
            enabled = enabled,
            onClick = onAddClick,
        )
    }
}

@Composable
private fun GlowingPlusButton(
    accent: Color,
    glyph: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "plusPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "plusGlow",
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = if (pressed) {
            tween(80)
        } else {
            spring(dampingRatio = 0.48f, stiffness = 420f)
        },
        label = "plusPress",
    )

    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer { alpha = if (enabled) pulse else 0.12f }
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.35f)),
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(accent)
                .clickable(
                    enabled = enabled,
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .semantics { contentDescription = "Add task or reminder" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = glyph,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
internal fun HomeSectionHeader(
    title: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
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
private fun AddActionChooser(
    onTask: () -> Unit,
    onReminder: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 96.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(KalliorColors.SurfaceElevated)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Add",
                color = KalliorColors.MutedText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.1.sp,
            )
            AddChooserRow(
                icon = Icons.Outlined.TaskAlt,
                label = "Task",
                subtitle = "Something to complete",
                onClick = onTask,
            )
            AddChooserRow(
                icon = Icons.Outlined.Notifications,
                label = "Reminder",
                subtitle = "Something to remember",
                onClick = onReminder,
            )
        }
    }
}

@Composable
private fun AddChooserRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KalliorColors.SurfaceCharcoal)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(KalliorColors.AccentOrange.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = KalliorColors.AccentOrange, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = KalliorColors.NormalText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = KalliorColors.MutedText, fontSize = 12.sp)
        }
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
