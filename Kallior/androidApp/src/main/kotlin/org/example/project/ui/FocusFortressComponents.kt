package org.example.project.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import kallos.domain.AppUsageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.example.project.R
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Protection events the lotus visualization reacts to. */
enum class LotusEvent { AppBlocked, WebsiteBlocked }

/** A permission the user must grant before protection can run. */
data class PermissionPrompt(val text: String, val action: () -> Unit)

internal fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${totalSeconds}s"
    }
}

/**
 * True when the system has animations disabled ("remove animations" accessibility
 * setting). The lotus then renders statically instead of looping.
 */
@Composable
internal fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

/** Slight scale-down while pressed; pairs with [MutableInteractionSource]-driven clickables. */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(150),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Tiny uppercase category label used above each section. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text.uppercase(),
            color = KalliorColors.MutedText,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.8.sp,
                fontFamily = FontFamily.SansSerif,
            ),
        )
        trailing?.invoke(this)
    }
}

// ============================================================
// PROTECTION HERO
// ============================================================

@Composable
fun ProtectionHero(
    active: Boolean,
    attemptsToday: Int,
    events: Flow<LotusEvent>,
    reducedMotion: Boolean,
    permissionPrompts: List<PermissionPrompt>,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LotusVisualization(
            active = active,
            events = events,
            reducedMotion = reducedMotion,
            onActivate = onActivate,
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp),
        )
        Spacer(Modifier.height(16.dp))
        ProtectionStatusChip(active)
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (active) "Protection is active" else "Protection paused",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp,
            ),
            color = KalliorColors.NormalText,
            textAlign = TextAlign.Center,
        )
        if (attemptsToday > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (attemptsToday == 1) "1 blocked attempt today" else "$attemptsToday blocked attempts today",
                style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.SansSerif),
                color = KalliorColors.MutedText,
            )
        }
        if (!active) {
            Spacer(Modifier.height(20.dp))
            ZenSiloPrimaryButton(text = "Enable Protection", onClick = onActivate)
        }
        if (permissionPrompts.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                permissionPrompts.forEach { prompt -> PermissionPromptRow(prompt) }
            }
        }
    }
}

@Composable
private fun ProtectionStatusChip(active: Boolean) {
    val accent = if (active) KalliorColors.AccentOrange else KalliorColors.InactiveNav
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Text(
            text = if (active) "PROTECTED" else "PAUSED",
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.SansSerif,
            ),
            color = if (active) KalliorColors.AccentOrange else KalliorColors.MutedText,
        )
    }
}

@Composable
private fun ZenSiloPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(KalliorColors.AccentOrange)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 36.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.Black,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
            ),
        )
    }
}

@Composable
private fun PermissionPromptRow(prompt: PermissionPrompt) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(KalliorColors.SurfaceElevated)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = prompt.action)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = prompt.text,
            color = KalliorColors.AccentOrange,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
            ),
        )
    }
}

// ============================================================
// LOTUS VISUALIZATION
// ============================================================

@Composable
fun LotusVisualization(
    active: Boolean,
    events: Flow<LotusEvent>,
    reducedMotion: Boolean,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lotusPainter = painterResource(R.drawable.protection__in_active)

    val tint by animateColorAsState(
        targetValue = if (active) KalliorColors.AccentOrange else KalliorColors.InactiveNav,
        animationSpec = tween(500),
        label = "lotusTint",
    )
    val glowColor by animateColorAsState(
        targetValue = if (active) KalliorColors.AccentOrange else KalliorColors.InactiveNav,
        animationSpec = tween(500),
        label = "lotusGlowColor",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (active) 0.16f else 0.05f,
        animationSpec = tween(500),
        label = "lotusGlowAlpha",
    )

    // Event reactions. Animatables start settled (1f) so nothing draws until a
    // real event snaps them back to 0f.
    val appPulse = remember { Animatable(1f) }
    val webPulse = remember { Animatable(1f) }
    val activation = remember { Animatable(1f) }

    LaunchedEffect(events, reducedMotion) {
        events.collect { event ->
            if (reducedMotion) return@collect
            when (event) {
                LotusEvent.AppBlocked -> {
                    appPulse.snapTo(0f)
                    appPulse.animateTo(1f, tween(650, easing = LinearOutSlowInEasing))
                }

                LotusEvent.WebsiteBlocked -> {
                    webPulse.snapTo(0f)
                    webPulse.animateTo(1f, tween(750, easing = LinearOutSlowInEasing))
                }
            }
        }
    }

    // Activation bloom whenever protection comes on (including screen entry while
    // protection is already running) — the "wake" moment of the hero.
    LaunchedEffect(active, reducedMotion) {
        if (active && !reducedMotion) {
            activation.snapTo(0f)
            activation.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        } else {
            activation.snapTo(1f)
        }
    }

    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.985f)
            .clickable(interactionSource = interaction, indication = null, onClick = onActivate),
        contentAlignment = Alignment.Center,
    ) {
        if (active && !reducedMotion) {
            AmbientLotusContent(
                lotusPainter = lotusPainter,
                tint = tint,
                glowColor = glowColor,
                glowAlpha = glowAlpha,
                appPulse = appPulse,
                webPulse = webPulse,
                activation = activation,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            StillLotusContent(
                lotusPainter = lotusPainter,
                tint = tint,
                glowColor = glowColor,
                glowAlpha = glowAlpha,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** Active state: breathing lotus, slow staggered ripples, glow, event rings. */
@Composable
private fun AmbientLotusContent(
    lotusPainter: Painter,
    tint: Color,
    glowColor: Color,
    glowAlpha: Float,
    appPulse: Animatable<Float, AnimationVector1D>,
    webPulse: Animatable<Float, AnimationVector1D>,
    activation: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "lotusAmbient")
    val breath = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lotusBreath",
    )
    val ripple = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(5200, easing = LinearEasing)),
        label = "lotusRipple",
    )

    Canvas(modifier = modifier) {
        drawLotusBase(
            glowColor = glowColor,
            glowAlpha = glowAlpha,
            breath = breath.value,
            ringsEnabled = true,
            ripple = ripple.value,
            appPulse = appPulse.value,
            webPulse = webPulse.value,
            activation = activation.value,
        )
    }

    Image(
        painter = lotusPainter,
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth(0.62f)
            .widthIn(max = 230.dp)
            .aspectRatio(481.9f / 349.31f)
            .graphicsLayer {
                val breathe = 1f + 0.016f * breath.value
                val bump = 1f + 0.04f * sin(PI * appPulse.value).toFloat()
                val scale = breathe * bump
                scaleX = scale
                scaleY = scale
            },
    )
}

/** Inactive / reduced-motion state: subdued lotus, minimal glow, no motion. */
@Composable
private fun StillLotusContent(
    lotusPainter: Painter,
    tint: Color,
    glowColor: Color,
    glowAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawLotusBase(
            glowColor = glowColor,
            glowAlpha = glowAlpha,
            breath = 0f,
            ringsEnabled = false,
            ripple = 0f,
            appPulse = 1f,
            webPulse = 1f,
            activation = 1f,
        )
    }

    Image(
        painter = lotusPainter,
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth(0.62f)
            .widthIn(max = 230.dp)
            .aspectRatio(481.9f / 349.31f)
            .graphicsLayer { alpha = 0.55f },
    )
}

private fun DrawScope.drawLotusBase(
    glowColor: Color,
    glowAlpha: Float,
    breath: Float,
    ringsEnabled: Boolean,
    ripple: Float,
    appPulse: Float,
    webPulse: Float,
    activation: Float,
) {
    val center = center
    val base = min(size.width, size.height)
    val lotusRadius = base * 0.26f
    val strokeWidth = 1.2.dp.toPx()

    // Soft breathing halo behind the lotus.
    val glowRadius = base * (0.5f + 0.02f * breath)
    val eventGlow = (1f - appPulse).coerceIn(0f, 1f) * 0.22f +
        (1f - webPulse).coerceIn(0f, 1f) * 0.18f +
        (1f - activation).coerceIn(0f, 1f) * 0.3f
    val effectiveGlow = (glowAlpha + eventGlow).coerceAtMost(0.45f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glowColor.copy(alpha = effectiveGlow), Color.Transparent),
            center = center,
            radius = glowRadius,
        ),
        radius = glowRadius,
        center = center,
    )

    // Thin static halo ring hugging the lotus.
    drawCircle(
        color = glowColor.copy(alpha = 0.10f),
        radius = lotusRadius * 1.22f,
        center = center,
        style = Stroke(width = strokeWidth),
    )

    if (ringsEnabled) {
        // Two staggered ambient ripples: expand, fade, restart.
        listOf(0f, 0.5f).forEach { phase ->
            val progress = (ripple + phase) % 1f
            drawRing(
                from = lotusRadius * 1.05f,
                to = lotusRadius * 2.15f,
                progress = progress,
                baseAlpha = 0.20f,
                color = glowColor,
                width = strokeWidth,
            )
        }
    }

    // App-block reaction: a ring reaching further out.
    if (appPulse in 0.001f..0.999f) {
        drawRing(
            from = lotusRadius * 1.05f,
            to = lotusRadius * 1.9f,
            progress = appPulse,
            baseAlpha = 0.5f,
            color = glowColor,
            width = strokeWidth * 1.3f,
        )
    }

    // Website-block reaction: a tighter, quicker ring.
    if (webPulse in 0.001f..0.999f) {
        drawRing(
            from = lotusRadius * 1.05f,
            to = lotusRadius * 1.6f,
            progress = webPulse,
            baseAlpha = 0.42f,
            color = glowColor,
            width = strokeWidth,
        )
    }

    // Activation burst: three quick staggered rings.
    if (activation in 0.001f..0.999f) {
        for (i in 0..2) {
            val progress = (activation * 1.5f - i * 0.18f).coerceIn(0f, 1f)
            if (progress in 0.001f..0.999f) {
                drawRing(
                    from = lotusRadius * 1.05f,
                    to = lotusRadius * 2.3f,
                    progress = progress,
                    baseAlpha = 0.38f,
                    color = glowColor,
                    width = strokeWidth,
                )
            }
        }
    }
}

private fun DrawScope.drawRing(
    from: Float,
    to: Float,
    progress: Float,
    baseAlpha: Float,
    color: Color,
    width: Float,
) {
    val radius = from + (to - from) * progress
    drawCircle(
        color = color.copy(alpha = baseAlpha * (1f - progress)),
        radius = radius,
        style = Stroke(width = width, cap = StrokeCap.Round),
    )
}

// ============================================================
// IMPACT SUMMARY
// ============================================================

@Composable
fun ImpactSummaryRow(
    sinkSeconds: Long,
    totalSeconds: Long?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ImpactMetric(
            label = "Time sunk",
            seconds = if (isLoading) null else sinkSeconds,
            emphasized = true,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .width(1.dp)
                .height(48.dp)
                .background(KalliorColors.Hairline),
        )
        ImpactMetric(
            label = "Screen time",
            seconds = if (isLoading) null else totalSeconds,
            emphasized = false,
            alignEnd = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ImpactMetric(
    label: String,
    seconds: Long?,
    emphasized: Boolean,
    alignEnd: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = label.uppercase(),
            color = KalliorColors.MutedText,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.8.sp,
                fontFamily = FontFamily.SansSerif,
            ),
        )
        Spacer(Modifier.height(8.dp))
        val valueColor = if (emphasized) KalliorColors.AccentOrange else KalliorColors.NormalText
        val valueStyle = MaterialTheme.typography.displaySmall.copy(
            fontFamily = Philosopher,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
        )
        if (seconds == null) {
            Text(text = "--", color = valueColor, style = valueStyle)
        } else {
            AnimatedCountText(
                targetValue = seconds.toFloat(),
                format = { formatDuration(it.toLong()) },
                color = valueColor,
                style = valueStyle,
                label = "${label}CountUp",
            )
        }
    }
}

// ============================================================
// RESTRICTIONS
// ============================================================

@Composable
fun RestrictionsSection(
    limitedApps: Int,
    limitedWebsites: Int,
    onAppsClick: () -> Unit,
    onWebsitesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RestrictionCard(
            title = "Limited Apps",
            count = limitedApps,
            iconRes = R.drawable.apps_blocked,
            onClick = onAppsClick,
            modifier = Modifier.weight(1f),
        )
        RestrictionCard(
            title = "Limited Websites",
            count = limitedWebsites,
            iconRes = R.drawable.websites_blocked,
            onClick = onWebsitesClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RestrictionCard(
    title: String,
    count: Int,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 116.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(KalliorColors.SurfaceElevated)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = KalliorColors.NormalText,
            modifier = Modifier.size(34.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = KalliorColors.NormalText,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (count > 0) "$count blocked" else "None yet",
                color = KalliorColors.MutedText,
                style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.SansSerif),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = KalliorColors.InactiveNav,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ============================================================
// TOP DISTRACTIONS
// ============================================================

@Composable
fun TopDistractionsSection(
    apps: List<AppUsageData>,
    hasUsageData: Boolean,
    loadIcon: (String) -> Drawable?,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(
            text = "Top distractions",
            trailing = {
                val interaction = remember { MutableInteractionSource() }
                Text(
                    text = "Manage",
                    color = KalliorColors.MutedText,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .pressScale(interaction)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onManage,
                        )
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                )
            },
        )
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(KalliorColors.SurfaceCharcoal)
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            val ranked = apps.sortedByDescending { it.timeInForegroundMs }.take(4)
            if (!hasUsageData || ranked.isEmpty()) {
                DistractionEmptyState()
            } else {
                val maxMs = ranked.maxOf { it.timeInForegroundMs }.coerceAtLeast(1L)
                ranked.forEachIndexed { index, app ->
                    DistractionRow(
                        app = app,
                        fraction = app.timeInForegroundMs.toFloat() / maxMs,
                        emphasized = index == 0,
                        loadIcon = loadIcon,
                    )
                    if (index != ranked.lastIndex) Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DistractionEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "You're clear",
            color = KalliorColors.NormalText,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "No distracting app usage recorded yet.",
            color = KalliorColors.MutedText,
            style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.SansSerif),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DistractionRow(
    app: AppUsageData,
    fraction: Float,
    emphasized: Boolean,
    loadIcon: (String) -> Drawable?,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = app.packageName, size = 26.dp, loadIcon = loadIcon)
            Spacer(Modifier.width(12.dp))
            Text(
                text = app.appName,
                color = KalliorColors.NormalText,
                style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.SansSerif),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatDuration(app.timeInForegroundMs / 1000L),
                color = KalliorColors.MutedText,
                style = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.SansSerif),
            )
        }
        Spacer(Modifier.height(8.dp))
        val animatedFraction by animateFloatAsState(
            targetValue = fraction.coerceIn(0f, 1f),
            animationSpec = tween(400, easing = FastOutSlowInEasing),
            label = "usageBar",
        )
        Box(
            modifier = Modifier
                .padding(start = 38.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(KalliorColors.Hairline),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (emphasized) KalliorColors.AccentOrange
                        else Color.White.copy(alpha = 0.30f)
                    ),
            )
        }
    }
}

/** Real launcher icon for [packageName], loaded lazily off the main thread. */
@Composable
private fun AppIcon(
    packageName: String,
    size: Dp,
    loadIcon: (String) -> Drawable?,
    modifier: Modifier = Modifier,
) {
    val icon by produceState<Drawable?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) { loadIcon(packageName) }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(KalliorColors.ForegroundCard.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        icon?.let {
            Image(
                bitmap = it.toImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(size - 6.dp),
            )
        }
    }
}

private fun Drawable.toImageBitmap(): androidx.compose.ui.graphics.ImageBitmap {
    val width = if (intrinsicWidth > 0) intrinsicWidth else 1
    val height = if (intrinsicHeight > 0) intrinsicHeight else 1
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

// ============================================================
// TIME / VALUE AT RISK
// ============================================================

@Composable
fun ValueAtRiskSection(
    hasUsageData: Boolean,
    totalSinkSeconds: Long,
    apps: List<AppUsageData>,
    ratePerSecond: Float,
    onRateChange: (Float) -> Unit,
    loadIcon: (String) -> Drawable?,
    modifier: Modifier = Modifier,
) {
    val earnings = totalSinkSeconds * ratePerSecond
    var showRateDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(text = "Time / value at risk")
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(KalliorColors.SurfaceCharcoal)
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            if (hasUsageData) {
                AnimatedCountText(
                    targetValue = earnings,
                    format = { "$ " + String.format("%.2f", it) },
                    color = KalliorColors.AccentOrange,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = Philosopher,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                    ),
                    label = "valueAtRiskCountUp",
                )
            } else {
                Text(
                    text = "$ --",
                    color = KalliorColors.AccentOrange,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = Philosopher,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Estimated value associated with your distraction time.",
                color = KalliorColors.MutedText,
                style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.SansSerif),
            )

            val ranked = apps.sortedByDescending { it.timeInForegroundMs }.take(4)
            if (hasUsageData && ranked.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = KalliorColors.Hairline, thickness = 1.dp)
                Spacer(Modifier.height(16.dp))
                ranked.forEachIndexed { index, app ->
                    ValueRow(app = app, ratePerSecond = ratePerSecond, loadIcon = loadIcon)
                    if (index != ranked.lastIndex) Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = KalliorColors.Hairline, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .background(KalliorColors.SurfaceElevated)
                    .pressScale(interaction)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                    ) { showRateDialog = true }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$ ${String.format("%.3f", ratePerSecond)} /sec",
                        color = KalliorColors.NormalText,
                        style = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.SansSerif),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Edit",
                        color = KalliorColors.AccentOrange,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                        ),
                    )
                }
            }
        }
    }

    if (showRateDialog) {
        RateDialog(
            currentRate = ratePerSecond,
            onDismiss = { showRateDialog = false },
            onSave = {
                onRateChange(it)
                showRateDialog = false
            },
        )
    }
}

@Composable
private fun ValueRow(
    app: AppUsageData,
    ratePerSecond: Float,
    loadIcon: (String) -> Drawable?,
) {
    val seconds = app.timeInForegroundMs / 1000L
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = app.packageName, size = 22.dp, loadIcon = loadIcon)
        Spacer(Modifier.width(10.dp))
        Text(
            text = app.appName,
            color = KalliorColors.NormalText,
            style = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.SansSerif),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatDuration(seconds),
            color = KalliorColors.MutedText,
            style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.SansSerif),
            modifier = Modifier.widthIn(min = 52.dp),
            textAlign = TextAlign.End,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "$ ${String.format("%.2f", seconds * ratePerSecond)}",
            color = KalliorColors.AccentOrange,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
            ),
            modifier = Modifier.widthIn(min = 64.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun RateDialog(currentRate: Float, onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var value by remember { mutableStateOf(String.format("%.3f", currentRate)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = KalliorColors.PrimaryLayer,
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
            Text("Set earning rate", color = KalliorColors.NormalText)
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Amount per second") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = dialogFieldColors(),
            )
        },
        confirmButton = {
            TextButton(onClick = { value.toFloatOrNull()?.let { onSave(it.coerceIn(0.001f, 1f)) } }) {
                Text("Save", color = KalliorColors.AccentOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = KalliorColors.MutedText) }
        },
    )
}
