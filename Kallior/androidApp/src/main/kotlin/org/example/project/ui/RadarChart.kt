package org.example.project.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.cos
import kotlin.math.sin

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
) {
    val axisCount = 5
    val ringCount = 4
    val maxRadiusFraction = 0.34f
    val iconRadiusDp = ((250f * maxRadiusFraction) + 28f).dp

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
    val axisTrig = remember(axisCount) {
        List(axisCount) { i ->
            val a = axisAngle(i, axisCount)
            cos(a).toFloat() to sin(a).toFloat()
        }
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

    Box(
        modifier = modifier.size(250.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(250.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = minOf(size.width, size.height) * maxRadiusFraction
            val ringColor = KalliorColors.RadarLine
            val hairline = 0.8f
            val outline = 1.5f

            for (ring in 1..ringCount) {
                drawCircle(
                    color = ringColor,
                    radius = maxRadius * ring / ringCount,
                    center = center,
                    style = Stroke(width = hairline),
                )
            }
            for (i in 0 until axisCount) {
                val (cosA, sinA) = axisTrig[i]
                drawLine(
                    color = ringColor,
                    start = center,
                    end = Offset(center.x + maxRadius * cosA, center.y - maxRadius * sinA),
                    strokeWidth = hairline,
                )
            }

            dataPath.rewind()
            for (i in 0 until axisCount) {
                val (cosA, sinA) = axisTrig[i]
                val r = maxRadius * animatedScores[i].value
                val v = Offset(center.x + r * cosA, center.y - r * sinA)
                if (i == 0) dataPath.moveTo(v.x, v.y) else dataPath.lineTo(v.x, v.y)
            }
            dataPath.close()

            drawPath(dataPath, color = accentColor.copy(alpha = 0.15f), style = Fill)
            drawPath(dataPath, color = accentColor.copy(alpha = 0.7f), style = Stroke(width = outline))

            for (i in 0 until axisCount) {
                val (cosA, sinA) = axisTrig[i]
                val r = maxRadius * animatedScores[i].value
                val v = Offset(center.x + r * cosA, center.y - r * sinA)
                drawCircle(color = accentColor, radius = 2.5f, center = v)
            }
        }

        for (i in 0 until axisCount) {
            val (cosA, sinA) = axisTrig[i]
            val dx = (iconRadiusDp.value * cosA).dp
            val dy = (-iconRadiusDp.value * sinA).dp
            axisIconPainters.getOrNull(i)?.let { painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(accentColor),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = dx, y = dy)
                        .size(22.dp),
                )
            }
        }
    }
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
