package org.example.project.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import org.example.project.alarm.AriaAlarmTime
import org.example.project.alarm.AlarmItem
import org.example.project.alarm.AriaSong
import java.util.Calendar
import java.util.Locale

/** Formats an hour/minute pair using the device's 12/24-hour preference. */
internal fun formatClock(context: Context, hour: Int, minute: Int): String {
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    return if (is24Hour) {
        String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    } else {
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        val suffix = if (hour < 12) "AM" else "PM"
        String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, suffix)
    }
}

@Composable
internal fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = KalliorColors.NormalText
        )

        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
internal fun AccentAddButton(
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (enabled) KalliorColors.AccentOrange else KalliorColors.ForegroundCard)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * The most important surface of the screen. Shows the [alarm] the user tapped
 * in the list (or the actual next alarm when nothing is selected), with an
 * orange "NEXT ALARM" eyebrow when the displayed alarm is the upcoming one.
 * Animates gracefully when the displayed alarm changes.
 */
@Composable
internal fun NextAlarmHero(
    alarm: AlarmItem?,
    nextAlarmId: Long?,
    songs: List<AriaSong>,
    previewSongId: String?,
    modifier: Modifier = Modifier,
    onToggle: (alarmId: Long, enabled: Boolean) -> Unit = { _, _ -> },
    onClick: (AlarmItem) -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(KalliorColors.PrimaryLayer)
    ) {
        AnimatedContent(
            targetState = alarm,
            transitionSpec = {
                (fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 8 }) togetherWith
                    fadeOut(tween(250))
            },
            contentKey = { it?.id ?: -1L },
            label = "nextAlarmHero"
        ) { current ->
            if (current == null) {
                EmptyHero()
            } else {
                ActiveHero(
                    alarm = current,
                    isNext = current.id == nextAlarmId,
                    songs = songs,
                    previewSongId = previewSongId,
                    onToggle = { enabled -> onToggle(current.id, enabled) },
                    onClick = { onClick(current) }
                )
            }
        }
    }
}

@Composable
private fun ActiveHero(
    alarm: AlarmItem,
    isNext: Boolean,
    songs: List<AriaSong>,
    previewSongId: String?,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val song = songs.firstOrNull { it.id == alarm.songId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isNext) KalliorColors.AccentOrange else KalliorColors.InactiveNav)
                )
                Text(
                    text = if (isNext) "NEXT ALARM" else "ALARM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = if (isNext) KalliorColors.AccentOrange else KalliorColors.InactiveNav
                )
            }

            Spacer(Modifier.weight(1f))

            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.semantics {
                    contentDescription =
                        if (alarm.enabled) "Disable ${alarm.name}" else "Enable ${alarm.name}"
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = KalliorColors.AccentOrange,
                    uncheckedThumbColor = KalliorColors.InactiveNav,
                    uncheckedTrackColor = KalliorColors.ForegroundCard,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier.size(216.dp),
            contentAlignment = Alignment.Center
        ) {
            AmbientRing(active = alarm.enabled, modifier = Modifier.fillMaxSize())

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatClock(context, alarm.hour, alarm.minute),
                    fontSize = 54.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = KalliorColors.NormalText
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = alarm.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = KalliorColors.MutedText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(KalliorColors.SurfaceCharcoal)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = KalliorColors.MutedText,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = song?.title ?: "No song selected",
                fontSize = 13.sp,
                color = if (song != null) KalliorColors.NormalText else KalliorColors.InactiveNav
            )
            if (song != null && previewSongId == song.id) {
                MiniEqualizer()
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (alarm.enabled) {
                val target = remember(alarm.id, alarm.hour, alarm.minute, alarm.repeatDays) {
                    AriaAlarmTime.nextTriggerAt(alarm.hour, alarm.minute, alarm.repeatDays)
                }
                CountdownText(targetMillis = target)
                Text(
                    text = "· ${AriaAlarmTime.repeatDaysLabel(alarm.repeatDays)}",
                    fontSize = 13.sp,
                    color = KalliorColors.InactiveNav
                )
            } else {
                Text(
                    text = "Off",
                    fontSize = 13.sp,
                    color = KalliorColors.InactiveNav
                )
            }
        }
    }
}

@Composable
private fun EmptyHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(KalliorColors.InactiveNav)
                )
                Text(
                    text = "NO ALARM SCHEDULED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = KalliorColors.InactiveNav
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            tint = KalliorColors.InactiveNav.copy(alpha = 0.6f),
            modifier = Modifier.size(40.dp)
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Your mornings are quiet",
            fontSize = 20.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = KalliorColors.NormalText
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Enable or create an alarm to wake up\nto something worth hearing.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = KalliorColors.MutedText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))
    }
}

/**
 * Slow ambient ring behind the hero time: a breathing orange glow plus two
 * counter-weighted arcs that rotate lazily. Fades out when inactive.
 */
@Composable
internal fun AmbientRing(active: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ambientRing")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "rotation"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val presence by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "presence"
    )

    Canvas(modifier = modifier) {
        if (presence <= 0.01f) return@Canvas

        val radius = size.minDimension / 2f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    KalliorColors.AccentOrange.copy(alpha = 0.14f * breathe * presence),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius
        )

        rotate(degrees = rotation) {
            drawArc(
                color = KalliorColors.AccentOrange.copy(alpha = 0.4f * presence),
                startAngle = -90f,
                sweepAngle = 70f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = KalliorColors.AccentOrange.copy(alpha = 0.14f * presence),
                startAngle = 140f,
                sweepAngle = 200f,
                useCenter = false,
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/** Self-updating countdown text; ticks every 30s without recomposing the screen. */
@Composable
internal fun CountdownText(
    targetMillis: Long,
    modifier: Modifier = Modifier,
    color: Color = KalliorColors.AccentOrange,
    fontSize: TextUnit = 14.sp
) {
    var now by remember(targetMillis) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(targetMillis) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    Text(
        text = AriaAlarmTime.formatCountdown(targetMillis - now),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold
    )
}

/** Three tiny animated bars shown while a song is previewing. */
@Composable
internal fun MiniEqualizer(modifier: Modifier = Modifier, color: Color = KalliorColors.AccentOrange) {
    val transition = rememberInfiniteTransition(label = "miniEqualizer")

    val bar1 by transition.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val bar2 by transition.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(760, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val bar3 by transition.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(660, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar3"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(bar1, bar2, bar3).forEach { phase ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp * phase)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Compact row for one alarm. The next upcoming alarm gets an elevated
 * treatment. Tap shows the alarm in the hero; long-press pops the card into
 * focus (everything else recedes behind a blur) and opens a context menu
 * with Edit and Delete. Three quick taps surface a hint about the long-press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AlarmCard(
    alarm: AlarmItem,
    isNext: Boolean,
    songTitle: String?,
    isSelected: Boolean,
    isMenuFocused: Boolean,
    recedeProgress: Float,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMenuFocusChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showHoldHint by remember { mutableStateOf(false) }
    var lastTapAtMillis by remember { mutableLongStateOf(0L) }
    var tapStreak by remember { mutableIntStateOf(0) }
    var cardHeightPx by remember { mutableIntStateOf(0) }
    var hintHeightPx by remember { mutableIntStateOf(0) }

    val container by animateColorAsState(
        targetValue = if (isNext && alarm.enabled) KalliorColors.SurfaceElevated else KalliorColors.PrimaryLayer,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "alarmCardContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isNext && alarm.enabled) {
            KalliorColors.AccentOrange.copy(alpha = 0.35f)
        } else {
            KalliorColors.Hairline
        },
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "alarmCardBorder"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (alarm.enabled) 1f else 0.55f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "alarmCardAlpha"
    )
    // The focused card pops toward the user while every other card recedes.
    val popProgress by animateFloatAsState(
        targetValue = if (isMenuFocused) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "alarmCardPop"
    )
    val selectionProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "alarmCardSelection"
    )
    val ownRecede = if (isMenuFocused) 0f else recedeProgress

    LaunchedEffect(showHoldHint) {
        if (showHoldHint) {
            delay(2600L)
            showHoldHint = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { cardHeightPx = it.height }
            .blur((6f * ownRecede).dp)
            .graphicsLayer {
                val pop = 1f + 0.03f * popProgress
                val recede = 1f - 0.02f * ownRecede
                scaleX = pop * recede
                scaleY = pop * recede
                alpha = 1f - 0.4f * ownRecede
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(container)
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .combinedClickable(
                    onClick = {
                        val now = System.currentTimeMillis()
                        tapStreak = if (now - lastTapAtMillis < 500L) tapStreak + 1 else 1
                        lastTapAtMillis = now
                        if (tapStreak >= 3) {
                            tapStreak = 0
                            showHoldHint = true
                        }
                        onSelect()
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHoldHint = false
                        onMenuFocusChange(true)
                        menuExpanded = true
                    }
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Text(
            text = formatClock(context, alarm.hour, alarm.minute),
            fontSize = 26.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = KalliorColors.NormalText,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha }
        )

        Spacer(Modifier.width(18.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = contentAlpha }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectionProgress > 0.01f) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer {
                                scaleX = selectionProgress
                                scaleY = selectionProgress
                                alpha = selectionProgress
                            }
                            .clip(CircleShape)
                            .background(KalliorColors.AccentOrange)
                    )
                }
                Text(
                    text = alarm.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KalliorColors.NormalText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isNext && alarm.enabled) {
                    ActiveBadge()
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = KalliorColors.MutedText,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = songTitle ?: "No song selected",
                    fontSize = 13.sp,
                    color = if (songTitle != null) KalliorColors.MutedText else KalliorColors.InactiveNav,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = AriaAlarmTime.repeatDaysLabel(alarm.repeatDays),
                fontSize = 12.sp,
                color = KalliorColors.InactiveNav
            )
        }

        Spacer(Modifier.width(8.dp))

        Switch(
            checked = alarm.enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.semantics {
                contentDescription =
                    if (alarm.enabled) "Disable ${alarm.name}" else "Enable ${alarm.name}"
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = KalliorColors.AccentOrange,
                uncheckedThumbColor = KalliorColors.InactiveNav,
                uncheckedTrackColor = KalliorColors.ForegroundCard,
                uncheckedBorderColor = Color.Transparent
            )
        )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = {
                menuExpanded = false
                onMenuFocusChange(false)
            },
            containerColor = KalliorColors.PrimaryLayer,
            shape = RoundedCornerShape(14.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Text("Edit", color = KalliorColors.NormalText, fontSize = 15.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = KalliorColors.MutedText,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    onMenuFocusChange(false)
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = {
                    Text("Delete", color = KalliorColors.DangerRed, fontSize = 15.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = KalliorColors.DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    onMenuFocusChange(false)
                    onDelete()
                }
            )
        }

        HoldHintPopup(
            visible = showHoldHint,
            bubbleHeightPx = hintHeightPx,
            onBubbleHeightMeasured = { hintHeightPx = it }
        )
    }
}

/**
 * Small bubble that floats just above a card after three quick taps,
 * nudging the user toward the long-press context menu.
 */
@Composable
private fun HoldHintPopup(
    visible: Boolean,
    bubbleHeightPx: Int,
    onBubbleHeightMeasured: (Int) -> Unit
) {
    val gapPx = with(LocalDensity.current) { 12.dp.roundToPx() }

    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, -(bubbleHeightPx + gapPx))
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150)) +
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    transformOrigin = TransformOrigin(0.5f, 1f)
                ),
            exit = fadeOut(tween(120)) +
                scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(140, easing = FastOutSlowInEasing),
                    transformOrigin = TransformOrigin(0.5f, 1f)
                )
        ) {
            Row(
                modifier = Modifier
                    .onSizeChanged { onBubbleHeightMeasured(it.height) }
                    .clip(RoundedCornerShape(12.dp))
                    .background(KalliorColors.PrimaryLayer)
                    .border(1.dp, KalliorColors.Hairline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(KalliorColors.AccentOrange)
                )
                Text(
                    text = "Hold the card for more options",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = KalliorColors.NormalText
                )
            }
        }
    }
}

@Composable
private fun ActiveBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(KalliorColors.AccentOrange.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(KalliorColors.AccentOrange)
        )
        Text(
            text = "Active",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = KalliorColors.AccentOrange
        )
    }
}

/** Compact library row with a play/pause preview control. */
@Composable
internal fun SongLibraryRow(
    song: AriaSong,
    isPreviewing: Boolean,
    loadArtist: suspend (AriaSong) -> String?,
    modifier: Modifier = Modifier,
    onTogglePreview: () -> Unit,
    onDelete: () -> Unit
) {
    var artist by remember(song.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(song.id) {
        artist = loadArtist(song)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KalliorColors.SurfaceCharcoal)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(KalliorColors.SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = KalliorColors.MutedText,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 15.sp,
                color = KalliorColors.NormalText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val artistName = artist
            if (!artistName.isNullOrBlank()) {
                Text(
                    text = artistName,
                    fontSize = 12.sp,
                    color = KalliorColors.MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isPreviewing) {
            MiniEqualizer(modifier = Modifier.padding(end = 6.dp))
        }

        IconButton(onClick = onTogglePreview, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = if (isPreviewing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPreviewing) {
                    "Stop preview of ${song.title}"
                } else {
                    "Preview ${song.title}"
                },
                tint = KalliorColors.NormalText,
                modifier = Modifier.size(22.dp)
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete ${song.title}",
                tint = KalliorColors.DangerRed,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun AriaEmptyState(
    title: String,
    body: String,
    actionLabel: String,
    modifier: Modifier = Modifier,
    onAction: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(KalliorColors.PrimaryLayer)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = KalliorColors.NormalText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = body,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = KalliorColors.MutedText,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(KalliorColors.AccentOrange)
                .clickable(onClick = onAction)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = actionLabel,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** One-shot fade/slide entrance used to stagger the screen's sections on open. */
@Composable
internal fun Modifier.entrance(delayMillis: Long): Modifier {
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!played) {
            delay(delayMillis)
            progress.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
            played = true
        }
    }

    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 14.dp.toPx()
    }
}

/**
 * Recedes background content while an alarm card's context menu is focused:
 * dimmed, softly blurred and scaled down a touch. [progress] runs 0 (normal)
 * to 1 (fully receded); the caller animates it.
 */
internal fun Modifier.menuRecede(progress: Float): Modifier = this
    .blur((6f * progress).dp)
    .graphicsLayer {
        alpha = 1f - 0.4f * progress
        scaleX = 1f - 0.02f * progress
        scaleY = 1f - 0.02f * progress
    }

/** Monday-first display order for [DayOfWeekSelector]. */
private val SELECTOR_DAYS = listOf(
    Calendar.MONDAY,
    Calendar.TUESDAY,
    Calendar.WEDNESDAY,
    Calendar.THURSDAY,
    Calendar.FRIDAY,
    Calendar.SATURDAY,
    Calendar.SUNDAY
)

private fun dayInitial(day: Int): String = when (day) {
    Calendar.MONDAY -> "M"
    Calendar.TUESDAY -> "T"
    Calendar.WEDNESDAY -> "W"
    Calendar.THURSDAY -> "T"
    Calendar.FRIDAY -> "F"
    Calendar.SATURDAY -> "S"
    Calendar.SUNDAY -> "S"
    else -> "?"
}

private fun dayName(day: Int): String = when (day) {
    Calendar.MONDAY -> "Monday"
    Calendar.TUESDAY -> "Tuesday"
    Calendar.WEDNESDAY -> "Wednesday"
    Calendar.THURSDAY -> "Thursday"
    Calendar.FRIDAY -> "Friday"
    Calendar.SATURDAY -> "Saturday"
    Calendar.SUNDAY -> "Sunday"
    else -> "Day"
}

/**
 * Seven circular day chips (Mon–Sun) for picking an alarm's repeat days.
 * Selected days fill with the accent orange; the last remaining day cannot
 * be deselected so an alarm can never end up with no active days.
 */
@Composable
internal fun DayOfWeekSelector(
    selectedDays: Set<Int>,
    modifier: Modifier = Modifier,
    onToggleDay: (Int) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SELECTOR_DAYS.forEach { day ->
            DayChip(
                day = day,
                selected = day in selectedDays,
                isOnlySelected = selectedDays.size == 1 && day in selectedDays,
                onToggle = { onToggleDay(day) }
            )
        }
    }
}

@Composable
private fun DayChip(
    day: Int,
    selected: Boolean,
    isOnlySelected: Boolean,
    onToggle: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) KalliorColors.AccentOrange else KalliorColors.ForegroundCard,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "dayChipBackground"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else KalliorColors.Hairline,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "dayChipBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else KalliorColors.MutedText,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "dayChipText"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dayChipScale"
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(background)
            .border(1.dp, borderColor, CircleShape)
            .semantics {
                contentDescription =
                    "${dayName(day)}, ${if (selected) "selected" else "not selected"}"
            }
            .clickable(enabled = !isOnlySelected, onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayInitial(day),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}
