package com.app.kallior.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle

/** Counts to [targetValue], then briefly bounces in the direction of the change. */
@Composable
fun AnimatedCountText(
    targetValue: Float,
    format: (Float) -> String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    label: String,
) {
    val displayedValue = remember { Animatable(0f) }
    val bounceOffset = remember { Animatable(0f) }
    val bounceDistance = with(LocalDensity.current) { 8f * density }

    LaunchedEffect(targetValue) {
        val startingValue = displayedValue.value
        if (startingValue == targetValue) return@LaunchedEffect

        displayedValue.animateTo(
            targetValue,
            animationSpec = tween(800, easing = FastOutSlowInEasing),
        )
        val directionalOffset = if (targetValue > startingValue) -bounceDistance else bounceDistance
        bounceOffset.animateTo(
            directionalOffset,
            animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        )
        bounceOffset.animateTo(
            0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        )
    }

    Text(
        text = format(displayedValue.value),
        color = color,
        style = style,
        modifier = modifier.graphicsLayer { translationY = bounceOffset.value },
    )
}
