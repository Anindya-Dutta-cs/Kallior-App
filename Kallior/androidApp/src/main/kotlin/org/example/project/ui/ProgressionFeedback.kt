package org.example.project.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kallos.domain.RadarScores
import kallos.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import org.example.project.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PREFERENCES_NAME = "progression_feedback"
private const val SUPPRESS_NEXT_SCORE_CHANGE = "suppress_next_score_change"

/** Prevents task creation from being treated as a progression event. */
fun suppressNextProgressionFeedback(context: Context) {
    context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(SUPPRESS_NEXT_SCORE_CHANGE, true)
        .apply()
}

@Composable
fun ProgressionFeedbackHost(gameViewModel: GameViewModel) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    var queue by remember { mutableStateOf<List<ProgressPopupChange>>(emptyList()) }
    var today by remember { mutableStateOf(currentDay()) }
    // The first refresh after entering the app can emit several intermediate
    // snapshots. Wait for it to settle, then compare the old visible value to
    // the final value once.
    var shouldCoalesceForegroundUpdate by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppOpen by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val scores = gameViewModel.userScores
    val hasLoadedGameState = gameViewModel.todaySnapshot != null

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    shouldCoalesceForegroundUpdate = true
                    isAppOpen = true
                }
                Lifecycle.Event.ON_PAUSE -> isAppOpen = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            today = currentDay()
        }
    }

    LaunchedEffect(scores, hasLoadedGameState, isAppOpen, today) {
        // Keep the last visible score as the baseline while Kallior is closed.
        // Resuming then produces one old-to-final popup instead of a popup per
        // background metric update.
        if (!hasLoadedGameState || !isAppOpen) return@LaunchedEffect
        if (shouldCoalesceForegroundUpdate) {
            delay(400L)
            shouldCoalesceForegroundUpdate = false
        }

        val lastSeenDay = preferences.getString("last_seen_day", null)
        val isNewDay = lastSeenDay != null && lastSeenDay != today
        val hasPreviousScores = preferences.contains("score_consistency")
        val previousScores = storedScores(preferences)
        val suppressFeedback = preferences.getBoolean(SUPPRESS_NEXT_SCORE_CHANGE, false)

        if (isNewDay) {
            // A day boundary intentionally replaces every score notification.
            queue = listOf(ProgressPopupChange.newDay())
        } else if (hasPreviousScores && !suppressFeedback) {
            queue += progressionChanges(previousScores, scores)
        }

        preferences.edit()
            .putString("last_seen_day", today)
            .putBoolean(SUPPRESS_NEXT_SCORE_CHANGE, false)
            .putFloat("score_consistency", scores.consistency.toFloat())
            .putFloat("score_discipline", scores.discipline.toFloat())
            .putFloat("score_focus", scores.focus.toFloat())
            .putFloat("score_health", scores.health.toFloat())
            .putFloat("score_resilience", scores.resilience.toFloat())
            .apply()
    }

    queue.firstOrNull()?.let { change ->
        ProgressionPopup(change = change, onFinished = { queue = queue.drop(1) })
    }
}

private data class ProgressPopupChange(
    val label: String,
    val iconRes: Int?,
    val from: Int,
    val to: Int,
    val isNewDay: Boolean = false,
) {
    companion object {
        fun newDay() = ProgressPopupChange("New Day, New Start!", null, 0, 0, isNewDay = true)
    }
}

private fun currentDay(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun storedScores(preferences: android.content.SharedPreferences) = RadarScores(
    consistency = preferences.getFloat("score_consistency", 0f).toDouble(),
    discipline = preferences.getFloat("score_discipline", 0f).toDouble(),
    focus = preferences.getFloat("score_focus", 0f).toDouble(),
    health = preferences.getFloat("score_health", 0f).toDouble(),
    resilience = preferences.getFloat("score_resilience", 0f).toDouble(),
)

private fun progressionChanges(before: RadarScores, after: RadarScores): List<ProgressPopupChange> =
    listOf(
        ProgressPopupChange("Consistency", R.drawable.consistency, before.consistency.roundToInt(), after.consistency.roundToInt()),
        ProgressPopupChange("Discipline", R.drawable.discipline, before.discipline.roundToInt(), after.discipline.roundToInt()),
        ProgressPopupChange("Focus", R.drawable.focus_icon, before.focus.roundToInt(), after.focus.roundToInt()),
        ProgressPopupChange("Health", R.drawable.health, before.health.roundToInt(), after.health.roundToInt()),
        ProgressPopupChange("Resilience", R.drawable.resilience, before.resilience.roundToInt(), after.resilience.roundToInt()),
    ).filter { abs(it.to - it.from) > 1 }

@Composable
private fun ProgressionPopup(change: ProgressPopupChange, onFinished: () -> Unit) {
    var visible by remember(change) { mutableStateOf(false) }
    val displayedValue = remember(change) { Animatable(change.from.toFloat()) }
    val bounceOffset = remember(change) { Animatable(0f) }
    val bounceDistance = with(LocalDensity.current) { 6.dp.toPx() }

    LaunchedEffect(change) {
        visible = true
        if (!change.isNewDay) {
            displayedValue.animateTo(change.to.toFloat(), animationSpec = tween(700))
            val directionalOffset = if (change.to > change.from) -bounceDistance else bounceDistance
            bounceOffset.animateTo(directionalOffset, animationSpec = tween(100))
            bounceOffset.animateTo(0f, animationSpec = tween(180))
        }
        delay(1_700L)
        visible = false
        delay(200L)
        onFinished()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(tween(220)) + scaleIn(tween(220)),
            exit = fadeOut(tween(180)) + scaleOut(tween(180)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
                .fillMaxWidth(0.86f),
        ) {
            Surface(
                shape = RoundedCornerShape(maxWidth * 0.1935f),
                color = KalliorColors.PrimaryLayer,
                border = BorderStroke(1.dp, KalliorColors.AccentOrange.copy(alpha = 0.65f)),
                shadowElevation = 10.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    change.iconRes?.let { iconRes ->
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = KalliorColors.AccentOrange,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Column {
                        Text(
                            text = change.label,
                            color = KalliorColors.NormalText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        if (!change.isNewDay) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${change.from}  →  ${displayedValue.value.roundToInt()}",
                                color = KalliorColors.AccentOrange,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.graphicsLayer { translationY = bounceOffset.value },
                            )
                        }
                    }
                }
            }
        }
    }
}
