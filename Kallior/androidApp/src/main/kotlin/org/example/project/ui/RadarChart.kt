package org.example.project.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

val RadarAxisNames = listOf("Focus", "Discipline", "Health", "Resilience", "Consistency")

private val RadarAxisHints = listOf(
    "Attention and deep work",
    "Schedules and limits",
    "Steps and restful sleep",
    "Recovery and momentum",
    "Routines over time",
)

/**
 * 5-axis radar (spider-web) chart drawn with [Canvas].
 *
 * Renders concentric circular rings, radial axis lines, a filled data polygon and
 * the per-vertex data points. Icons supplied via [axisIconPainters] are overlaid at
 * each axis. Every axis uses `angle = 90° + (360°/5)·i`, so vertex 0 points straight
 * up (12 o'clock) and the remaining axes are evenly spaced 72° apart clockwise.
 */
@Composable
fun RadarChartView(
    scores: List<Double>,
    axisIconPainters: List<Painter>,
    modifier: Modifier = Modifier,
    accentColor: Color = KalliorColors.AccentOrange,
    axisNames: List<String> = RadarAxisNames,
    onAxisTapped: ((axisIndex: Int) -> Unit)? = null,
) {
    val axisCount = 5
    val ringCount = 5

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppOpen by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isAppOpen = true
                Lifecycle.Event.ON_PAUSE -> isAppOpen = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val radarPreferences = remember(context) {
        context.getSharedPreferences("radar_chart_animation", android.content.Context.MODE_PRIVATE)
    }
    val targetScores = List(axisCount) { index -> normalized(scores.getOrNull(index)) }
    val initialScores = remember(radarPreferences) {
        List(axisCount) { index ->
            radarPreferences.getFloat("score_$index", targetScores[index])
        }
    }
    val dataPath = remember { Path() }
    val glowPath = remember { Path() }
    val axisTrig = remember(axisCount) {
        List(axisCount) { i ->
            val a = axisAngle(i, axisCount)
            cos(a).toFloat() to sin(a).toFloat()
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var selectedAxis by remember { mutableStateOf<Int?>(null) }

    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(
            1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }

    val animatedScores = List(axisCount) { index ->
        val animatedScore = remember { Animatable(initialScores[index]) }
        LaunchedEffect(targetScores[index], isAppOpen) {
            if (isAppOpen) {
                animatedScore.animateTo(
                    targetScores[index],
                    animationSpec = tween(durationMillis = 1_000, easing = EaseOut),
                )
            }
        }
        animatedScore
    }

    LaunchedEffect(targetScores, isAppOpen) {
        if (isAppOpen) {
            radarPreferences.edit().apply {
                targetScores.forEachIndexed { index, score -> putFloat("score_$index", score) }
            }.apply()
        }
    }

    val reveal = entrance.value
    val gridAlpha = stage(reveal, 0f, 0.22f)
    val axisLineAlpha = stage(reveal, 0.12f, 0.36f)
    val polygonGrow = stage(reveal, 0.22f, 0.68f)
    val pointsAlpha = stage(reveal, 0.52f, 0.74f)
    val glowAlpha = stage(reveal, 0.58f, 0.86f)
    val labelsAlpha = stage(reveal, 0.68f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Performance radar chart" },
        contentAlignment = Alignment.Center,
    ) {
        val boundedWidth = if (maxWidth == Dp.Infinity || maxWidth.value.isNaN()) 360.dp else maxWidth
        val chartBox = minOf(boundedWidth, 380.dp)
        val maxRadiusFraction = 0.30f
        val labelRadius = chartBox * (maxRadiusFraction + 0.145f)

        Box(
            modifier = Modifier.size(chartBox),
            contentAlignment = Alignment.Center,
        ) {
            if (selectedAxis != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { selectedAxis = null },
                )
            }

            Canvas(modifier = Modifier.size(chartBox * 0.78f)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = min(size.width, size.height) * 0.48f
                val hairline = 0.9f
                val outline = 2.2f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.16f * glowAlpha),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = maxRadius * 1.15f,
                    ),
                    radius = maxRadius * 1.15f,
                    center = center,
                )

                for (ring in 1..ringCount) {
                    val ringT = ring / ringCount.toFloat()
                    drawCircle(
                        color = KalliorColors.RadarLine.copy(
                            alpha = (0.18f + ringT * 0.22f) * gridAlpha,
                        ),
                        radius = maxRadius * ringT,
                        center = center,
                        style = Stroke(width = hairline),
                    )
                }
                for (i in 0 until axisCount) {
                    val (cosA, sinA) = axisTrig[i]
                    val emphasized = selectedAxis == i
                    drawLine(
                        color = (if (emphasized) accentColor else KalliorColors.RadarLine)
                            .copy(alpha = (if (emphasized) 0.7f else 0.28f) * axisLineAlpha),
                        start = center,
                        end = Offset(center.x + maxRadius * cosA, center.y - maxRadius * sinA),
                        strokeWidth = if (emphasized) 1.6f else hairline,
                    )
                }

                glowPath.rewind()
                dataPath.rewind()
                for (i in 0 until axisCount) {
                    val (cosA, sinA) = axisTrig[i]
                    val r = maxRadius * animatedScores[i].value * polygonGrow
                    val v = Offset(center.x + r * cosA, center.y - r * sinA)
                    if (i == 0) {
                        dataPath.moveTo(v.x, v.y)
                        glowPath.moveTo(v.x, v.y)
                    } else {
                        dataPath.lineTo(v.x, v.y)
                        glowPath.lineTo(v.x, v.y)
                    }
                }
                dataPath.close()
                glowPath.close()

                drawPath(
                    glowPath,
                    color = accentColor.copy(alpha = 0.12f * glowAlpha),
                    style = Stroke(width = 10f),
                )
                drawPath(dataPath, color = accentColor.copy(alpha = 0.20f * polygonGrow), style = Fill)
                drawPath(
                    dataPath,
                    color = accentColor.copy(alpha = 0.92f * polygonGrow),
                    style = Stroke(width = outline),
                )

                for (i in 0 until axisCount) {
                    val (cosA, sinA) = axisTrig[i]
                    val r = maxRadius * animatedScores[i].value * polygonGrow
                    val v = Offset(center.x + r * cosA, center.y - r * sinA)
                    val emphasized = selectedAxis == i
                    drawCircle(
                        color = accentColor.copy(alpha = 0.28f * pointsAlpha),
                        radius = if (emphasized) 9f else 6.5f,
                        center = v,
                    )
                    drawCircle(
                        color = accentColor.copy(alpha = pointsAlpha),
                        radius = if (emphasized) 4.2f else 3.1f,
                        center = v,
                    )
                }
            }

            for (i in 0 until axisCount) {
                val (cosA, sinA) = axisTrig[i]
                val dx = (labelRadius.value * cosA).dp
                val dy = (-labelRadius.value * sinA).dp
                val name = axisNames.getOrElse(i) { RadarAxisNames.getOrElse(i) { "Metric" } }
                val valuePct = (animatedScores[i].value * 100f).roundToInt()
                val emphasized = selectedAxis == i
                axisIconPainters.getOrNull(i)?.let { painter ->
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = dx, y = dy)
                            .graphicsLayer { alpha = labelsAlpha }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (selectedAxis == i && onAxisTapped != null) {
                                    coroutineScope.launch { onAxisTapped.invoke(i) }
                                } else {
                                    selectedAxis = i
                                }
                            }
                            .semantics {
                                contentDescription = "$name $valuePct percent"
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painter,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(accentColor),
                            modifier = Modifier
                                .size(if (emphasized) 20.dp else 16.dp)
                                .graphicsLayer {
                                    val bump = if (emphasized) 1.12f else 1f
                                    scaleX = bump
                                    scaleY = bump
                                },
                        )
                        Text(
                            text = name,
                            color = if (emphasized) KalliorColors.NormalText else KalliorColors.MutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                        Text(
                            text = "$valuePct%",
                            color = accentColor.copy(alpha = if (emphasized) 1f else 0.85f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }

            selectedAxis?.let { axis ->
                val (cosA, sinA) = axisTrig[axis]
                val name = axisNames.getOrElse(axis) { "Metric" }
                val valuePct = (animatedScores[axis].value * 100f).roundToInt()
                val hint = RadarAxisHints.getOrElse(axis) { "" }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(
                            x = labelRadius * cosA * 0.35f,
                            y = -labelRadius * sinA * 0.18f,
                        )
                        .widthIn(min = 108.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(KalliorColors.SurfaceElevated)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAxisTapped?.invoke(axis)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = name.uppercase(),
                            color = KalliorColors.MutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                        )
                        Text(
                            text = "$valuePct%",
                            color = accentColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        if (hint.isNotBlank()) {
                            Text(
                                text = hint,
                                color = KalliorColors.MutedText,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun stage(progress: Float, start: Float, end: Float): Float {
    if (end <= start) return if (progress >= end) 1f else 0f
    return ((progress - start) / (end - start)).coerceIn(0f, 1f)
}

/** Normalized score in [0, 1] from a 0–100 value (null-safe). */
private fun normalized(value: Double?): Float =
    (value?.coerceIn(0.0, 100.0)?.div(100.0) ?: 0.0).toFloat()

/**
 * Axis angle in radians for axis `index`, measured counter-clockwise from the
 * positive x-axis (math convention, y-up). Vertex 0 is 90° (straight up); each
 * subsequent axis steps +72°, placing the 5 axes at 90°, 162°, 234°, 306°, 18°.
 */
private fun axisAngle(index: Int, axisCount: Int): Double =
    Math.PI / 2.0 + (2.0 * Math.PI * index / axisCount)
