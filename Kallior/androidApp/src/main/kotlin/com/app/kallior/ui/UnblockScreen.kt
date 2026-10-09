package com.app.kallior.ui

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import com.app.kallior.BlockEventBus
import com.app.kallior.BlockerRepository
import com.app.kallior.BlockerStatsTracker
import com.app.kallior.WebsiteBlockerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.ceil

private const val COUNTDOWN_MS = 5_000L
private const val HOLD_MS = 3_000L
private const val SUCCESS_MS = 450L

private enum class UnblockPhase { SELECT_DURATION, COUNTDOWN, READY_TO_HOLD, HOLDING, SAVING, SUCCESS }

/** The allowance is written only after the deliberate pause and completed hold. */
@Composable
fun UnblockScreen(
    packageName: String,
    blockedLabel: String,
    domain: String,
    isWebsite: Boolean,
    onUnblocked: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val reducedMotion = remember(context) { !ValueAnimator.areAnimatorsEnabled() }
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    var phase by remember(packageName, domain, isWebsite) { mutableStateOf(UnblockPhase.SELECT_DURATION) }
    var minutes by remember(packageName, domain, isWebsite) { mutableStateOf<Int?>(null) }
    var countdownStart by remember(packageName, domain, isWebsite) { mutableLongStateOf(0L) }
    var countdownElapsed by remember(packageName, domain, isWebsite) { mutableLongStateOf(0L) }
    var holdStart by remember(packageName, domain, isWebsite) { mutableLongStateOf(0L) }
    var holdProgress by remember(packageName, domain, isWebsite) { mutableFloatStateOf(0f) }
    var accessibleHold by remember(packageName, domain, isWebsite) { mutableStateOf(false) }
    var error by remember(packageName, domain, isWebsite) { mutableStateOf<String?>(null) }

    fun cancel() {
        if (phase == UnblockPhase.SAVING || phase == UnblockPhase.SUCCESS) return
        vibrator?.cancel()
        phase = UnblockPhase.SELECT_DURATION
        minutes = null
        holdProgress = 0f
        onCancel()
    }

    // A backgrounded screen cannot silently finish the pause or a held gesture.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && phase != UnblockPhase.SAVING && phase != UnblockPhase.SUCCESS) {
                vibrator?.cancel()
                phase = UnblockPhase.SELECT_DURATION
                minutes = null
                holdProgress = 0f
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler { cancel() } // Overrides the parent destination's Back handler, including during a write.

    DisposableEffect(phase, vibrator) {
        if (phase == UnblockPhase.HOLDING && vibrator?.hasVibrator() == true) {
            try {
                // A gentle repeating pulse runs only while the deliberate hold is active.
                vibrator.vibrate(VibrationEffect.createWaveform(
                    longArrayOf(0L, 25L, 90L), intArrayOf(0, 55, 0), 1,
                ))
            } catch (_: RuntimeException) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
        onDispose { if (phase == UnblockPhase.HOLDING) vibrator?.cancel() }
    }

    LaunchedEffect(phase, countdownStart) {
        if (phase != UnblockPhase.COUNTDOWN) return@LaunchedEffect
        var previousSecond = 5
        while (phase == UnblockPhase.COUNTDOWN) {
            countdownElapsed = (SystemClock.elapsedRealtime() - countdownStart).coerceIn(0L, COUNTDOWN_MS)
            val second = ((COUNTDOWN_MS - countdownElapsed + 999L) / 1000L).toInt()
            if (second < previousSecond) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                previousSecond = second
            }
            if (countdownElapsed >= COUNTDOWN_MS) {
                phase = UnblockPhase.READY_TO_HOLD
                break
            }
            delay(if (reducedMotion) 100L else 32L)
        }
    }

    LaunchedEffect(phase, holdStart) {
        if (phase != UnblockPhase.HOLDING) return@LaunchedEffect
        while (phase == UnblockPhase.HOLDING) {
            val elapsed = SystemClock.elapsedRealtime() - holdStart
            holdProgress = (elapsed.toFloat() / HOLD_MS).coerceIn(0f, 1f)
            if (elapsed >= HOLD_MS) {
                vibrator?.cancel()
                phase = UnblockPhase.SAVING
                break
            }
            delay(16L)
        }
    }

    LaunchedEffect(phase) {
        if (phase != UnblockPhase.SAVING) return@LaunchedEffect
        val duration = minutes ?: return@LaunchedEffect
        try {
            val expiry = System.currentTimeMillis() + duration * 60_000L
            if (isWebsite) {
                WebsiteBlockerRepository(context).setAllowUntil(domain, expiry)
                BlockEventBus.updateWhitelist(domain, expiry)
            } else {
                BlockerRepository(context).setAllowUntil(packageName, expiry)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "Couldn't save the unblock. Please try again."
            holdProgress = 0f
            phase = UnblockPhase.READY_TO_HOLD
            return@LaunchedEffect
        }
        BlockerStatsTracker.recordBypass()
        BlockEventBus.emitUnblockSuccess(blockedLabel, duration)
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        phase = UnblockPhase.SUCCESS
    }

    LaunchedEffect(phase) {
        if (phase == UnblockPhase.SUCCESS) {
            delay(SUCCESS_MS)
            minutes?.let(onUnblocked)
        }
    }

    Box(Modifier.fillMaxSize().background(KalliorColors.CanvasBackground)
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars)) {
        Column(Modifier.fillMaxSize()) {
            UnblockTopBar(phase != UnblockPhase.SAVING && phase != UnblockPhase.SUCCESS, ::cancel)
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))
                AppIdentity(packageName, blockedLabel, isWebsite, phase, holdProgress)
                Spacer(Modifier.height(32.dp))
                DurationStage(
                    minutes = minutes,
                    phase = phase,
                    reducedMotion = reducedMotion,
                    countdownElapsed = countdownElapsed,
                    holdProgress = holdProgress,
                    error = error,
                    onSelect = { selected ->
                        if (phase == UnblockPhase.SELECT_DURATION && minutes != selected) {
                            minutes = selected
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onConfirm = {
                        if (phase == UnblockPhase.SELECT_DURATION && minutes != null) {
                            error = null
                            countdownElapsed = 0L
                            countdownStart = SystemClock.elapsedRealtime()
                            phase = UnblockPhase.COUNTDOWN
                        }
                    },
                    onHoldStart = { accessible ->
                        if (phase == UnblockPhase.READY_TO_HOLD) {
                            accessibleHold = accessible
                            holdStart = SystemClock.elapsedRealtime()
                            holdProgress = 0f
                            phase = UnblockPhase.HOLDING
                        }
                    },
                    onHoldEnd = {
                        if (phase == UnblockPhase.HOLDING && !accessibleHold) {
                            vibrator?.cancel()
                            phase = UnblockPhase.READY_TO_HOLD
                            holdProgress = 0f
                        }
                    },
                )
                Spacer(Modifier.height(40.dp))
                Text(
                    "“Take a moment before continuing.”",
                    color = KalliorColors.MutedText,
                    fontFamily = FontFamily.Serif,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun UnblockTopBar(canCancel: Boolean, onCancel: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 20.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Kallior", color = KalliorColors.NormalText, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold, fontSize = 25.sp)
        TextButton(onClick = onCancel, enabled = canCancel, modifier = Modifier.height(48.dp)) {
            Text("Cancel", color = if (canCancel) KalliorColors.NormalText else KalliorColors.MutedText)
        }
    }
}

@Composable
private fun AppIdentity(packageName: String, label: String, isWebsite: Boolean,
                        phase: UnblockPhase, progress: Float) {
    val context = LocalContext.current
    val drawable = remember(packageName, isWebsite) {
        if (isWebsite || packageName.isBlank()) null
        else runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
    }
    Box(Modifier.size(184.dp).drawBehind {
        for (radius in listOf(64.dp, 79.dp, 91.dp)) {
            drawCircle(KalliorColors.AccentOrange.copy(alpha = if (phase == UnblockPhase.HOLDING) 0.14f else 0.07f),
                radius = radius.toPx(), center = center, style = Stroke(1.dp.toPx()))
        }
        if (phase == UnblockPhase.HOLDING || phase == UnblockPhase.SUCCESS) {
            drawArc(KalliorColors.AccentOrange.copy(alpha = 0.55f), -90f, progress * 360f,
                useCenter = false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
    }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(92.dp).clip(RoundedCornerShape(24.dp))
            .background(KalliorColors.SurfaceCharcoal).border(1.dp,
                KalliorColors.AccentOrange.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
            .semantics { contentDescription = if (isWebsite) "Website" else "App icon for $label" },
            contentAlignment = Alignment.Center) {
            if (drawable != null) {
                AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                    update = { it.setImageDrawable(drawable) }, modifier = Modifier.size(68.dp))
            } else {
                Icon(if (isWebsite) Icons.Default.Language else Icons.Default.Apps,
                    contentDescription = null, tint = KalliorColors.NormalText, modifier = Modifier.size(42.dp))
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(if (isWebsite) "UNBLOCK WEBSITE" else "UNBLOCK APP", color = KalliorColors.AccentOrange,
        fontSize = 11.sp, letterSpacing = 2.sp)
    Spacer(Modifier.height(8.dp))
    Text(label.ifBlank { if (isWebsite) "Website" else "App" }, color = KalliorColors.NormalText,
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp,
        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(8.dp))
    Text("A little space to choose intentionally.", color = KalliorColors.MutedText,
        fontSize = 14.sp, textAlign = TextAlign.Center)
}

@Composable
private fun DurationStage(
    minutes: Int?, phase: UnblockPhase, reducedMotion: Boolean,
    countdownElapsed: Long, holdProgress: Float, error: String?,
    onSelect: (Int) -> Unit, onConfirm: () -> Unit,
    onHoldStart: (Boolean) -> Unit, onHoldEnd: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("UNBLOCK FOR", color = KalliorColors.MutedText, fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(16.dp))
        AnimatedContent(
            targetState = phase == UnblockPhase.SELECT_DURATION,
            transitionSpec = {
                (fadeIn(tween(if (reducedMotion) 0 else 280)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(if (reducedMotion) 0 else 280)))
                    .togetherWith(fadeOut(tween(if (reducedMotion) 0 else 180)))
            },
            label = "Duration confirmation",
        ) { selecting ->
            if (selecting) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    DurationSelector(minutes, reducedMotion, onSelect)
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = onConfirm,
                        enabled = minutes != null,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        border = BorderStroke(1.dp, if (minutes != null) KalliorColors.AccentOrange else KalliorColors.MutedText.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (minutes != null) KalliorColors.AccentOrange.copy(alpha = 0.12f) else KalliorColors.SurfaceCharcoal,
                            contentColor = KalliorColors.NormalText,
                        ),
                    ) { Text(minutes?.let { "Confirm $it minutes" } ?: "Select a duration", fontWeight = FontWeight.SemiBold) }
                }
            } else {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    minutes?.let { confirmed ->
                        Text("$confirmed min", color = KalliorColors.NormalText, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                .background(KalliorColors.AccentOrange.copy(alpha = 0.1f))
                                .border(1.dp, KalliorColors.AccentOrange.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                .semantics { stateDescription = "$confirmed minutes confirmed" })
                    }
                    Spacer(Modifier.height(24.dp))
                    AnimatedContent(
                        targetState = phase == UnblockPhase.COUNTDOWN,
                        transitionSpec = {
                            (fadeIn(tween(if (reducedMotion) 0 else 250)) +
                                scaleIn(initialScale = 0.96f, animationSpec = tween(if (reducedMotion) 0 else 250)))
                                .togetherWith(fadeOut(tween(if (reducedMotion) 0 else 180)))
                        },
                        label = "Countdown to hold",
                    ) { counting ->
                        UnblockInteraction(
                            if (counting) UnblockPhase.COUNTDOWN else phase,
                            countdownElapsed, holdProgress, reducedMotion, error, onHoldStart, onHoldEnd,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationSelector(minutes: Int?, reducedMotion: Boolean, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(5, 10, 15).forEach { duration ->
            val active = minutes == duration
            val outline by animateFloatAsState(if (active) 1f else 0f,
                animationSpec = tween(if (reducedMotion) 0 else 180), label = "Duration outline")
            OutlinedButton(
                onClick = { onSelect(duration) },
                modifier = Modifier.weight(1f).height(52.dp).semantics {
                    selected = active
                    stateDescription = if (active) "Selected, $duration minutes" else "$duration minutes"
                },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, lerp(KalliorColors.MutedText.copy(alpha = 0.3f),
                    KalliorColors.AccentOrange, outline)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (active) KalliorColors.AccentOrange.copy(alpha = 0.08f) else KalliorColors.SurfaceCharcoal,
                    contentColor = KalliorColors.NormalText,
                ),
            ) { Text("$duration min", maxLines = 1, fontSize = 14.sp) }
        }
    }
}

@Composable
private fun CountdownNumber(seconds: Int) {
    Text("$seconds", fontFamily = FontFamily.Serif, fontSize = 64.sp,
        color = KalliorColors.NormalText)
}

@Composable
private fun UnblockInteraction(phase: UnblockPhase, countdownElapsed: Long,
                               holdProgress: Float, reducedMotion: Boolean, error: String?,
                               onHoldStart: (Boolean) -> Unit, onHoldEnd: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (phase) {
            UnblockPhase.SELECT_DURATION -> {
                Spacer(Modifier.height(52.dp))
                Text("Choose a duration to begin", color = KalliorColors.NormalText, textAlign = TextAlign.Center)
                Spacer(Modifier.height(52.dp))
            }
            UnblockPhase.COUNTDOWN -> {
                Text("Take a moment", color = KalliorColors.MutedText)
                Spacer(Modifier.height(12.dp))
                val seconds = ceil((COUNTDOWN_MS - countdownElapsed) / 1000.0).toInt().coerceAtLeast(1)
                Box(Modifier.size(136.dp).drawBehind {
                    drawCircle(KalliorColors.MutedText.copy(alpha = 0.22f), style = Stroke(3.dp.toPx()))
                    drawArc(KalliorColors.AccentOrange, -90f,
                        360f * (countdownElapsed.toFloat() / COUNTDOWN_MS), useCenter = false,
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                }.semantics { stateDescription = "$seconds seconds remaining" }, contentAlignment = Alignment.Center) {
                    if (reducedMotion) {
                        CountdownNumber(seconds)
                    } else {
                        AnimatedContent(
                            targetState = seconds,
                            transitionSpec = {
                                (fadeIn(tween(240)) + scaleIn(initialScale = 0.92f, animationSpec = tween(240)))
                                    .togetherWith(fadeOut(tween(120)) + scaleOut(targetScale = 0.92f, animationSpec = tween(120)))
                            },
                            label = "Countdown number",
                        ) { number -> CountdownNumber(number) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Hold becomes available when the countdown ends", color = KalliorColors.MutedText,
                    fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            else -> {
                Text(when (phase) {
                    UnblockPhase.SAVING -> "Saving your unblock…"
                    UnblockPhase.SUCCESS -> "You chose to continue"
                    else -> "Ready when you are"
                }, color = KalliorColors.MutedText)
                Spacer(Modifier.height(24.dp))
                val displayProgress by animateFloatAsState(holdProgress,
                    animationSpec = tween(if (reducedMotion || phase == UnblockPhase.HOLDING) 0 else 180),
                    label = "Hold progress")
                Box(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp))
                    .background(KalliorColors.SurfaceElevated)
                    .then(if (phase == UnblockPhase.READY_TO_HOLD || phase == UnblockPhase.HOLDING) {
                        Modifier.semantics {
                            role = Role.Button
                            stateDescription = if (phase == UnblockPhase.HOLDING) "Unblocking in progress" else "Ready to unblock"
                            progressBarRangeInfo = ProgressBarRangeInfo(displayProgress, 0f..1f)
                            onClick(label = "Start timed unblock") {
                                onHoldStart(true) // TalkBack activation uses the same three-second gate.
                                true
                            }
                        }.pointerInput(phase == UnblockPhase.READY_TO_HOLD || phase == UnblockPhase.HOLDING) {
                            if (phase != UnblockPhase.READY_TO_HOLD) return@pointerInput
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false).consume()
                                onHoldStart(false)
                                try { waitForUpOrCancellation() } finally { onHoldEnd() }
                            }
                        }
                    } else Modifier), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxWidth(displayProgress).height(64.dp)
                        .align(Alignment.CenterStart).background(KalliorColors.AccentOrange.copy(alpha = 0.5f)))
                    Text(when (phase) {
                        UnblockPhase.SAVING -> "Saving…"
                        UnblockPhase.SUCCESS -> "✓ Unblocked"
                        else -> "Hold to Unblock"
                    }, color = KalliorColors.NormalText, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp)
                }
                Spacer(Modifier.height(10.dp))
                if (phase == UnblockPhase.READY_TO_HOLD || phase == UnblockPhase.HOLDING) {
                    Text("Press and hold for 3 seconds", color = KalliorColors.MutedText, fontSize = 12.sp)
                }
            }
        }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = KalliorColors.DangerRed, textAlign = TextAlign.Center)
        }
    }
}
