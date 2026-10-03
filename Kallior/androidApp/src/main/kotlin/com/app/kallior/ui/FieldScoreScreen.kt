package com.app.kallior.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kallos.viewmodel.GameViewModel
import com.app.kallior.R
import com.app.kallior.ui.theme.ShadowPurple
import kotlin.math.roundToInt

/**
 * Screen displaying the details and score for a specific radar chart field.
 *
 * Reached by tapping one of the 5 radar chart icons:
 * 0 -> Focus
 * 1 -> Discipline
 * 2 -> Health
 * 3 -> Resilience
 * 4 -> Consistency
 */
@Composable
fun FieldScoreScreen(
    navController: NavHostController,
    gameViewModel: GameViewModel,
    fieldIndex: Int,
    isShadow: Boolean = false,
) {
    BackHandler { navController.popBackStack() }

    val shadowState by gameViewModel.shadowHomeState.collectAsState()
    val scores = if (isShadow) shadowState.scores else gameViewModel.userScores
    val snapshot = gameViewModel.todaySnapshot

    val fieldInfo = remember(fieldIndex) {
        when (fieldIndex) {
            0 -> FieldDetails(
                name = "Focus",
                iconRes = R.drawable.focus_icon,
                description = "Measures attention control, distraction resistance, and sustained deep work throughout your daily sessions.",
                metricLabel = "Screen Time",
            )
            1 -> FieldDetails(
                name = "Discipline",
                iconRes = R.drawable.discipline,
                description = "Reflects adherence to daily schedules, blocker limits, and avoiding excessive entertainment screen time.",
                metricLabel = "Blocker Bypasses",
            )
            2 -> FieldDetails(
                name = "Health",
                iconRes = R.drawable.health,
                description = "Combines physical activity from daily step counts with healthy, restful sleep duration.",
                metricLabel = "Steps & Sleep",
            )
            3 -> FieldDetails(
                name = "Resilience",
                iconRes = R.drawable.resilience,
                description = "Tracks consistency across challenging periods, task streak recovery, and maintaining momentum.",
                metricLabel = "Streak Recovery",
            )
            4 -> FieldDetails(
                name = "Consistency",
                iconRes = R.drawable.consistency,
                description = "Evaluates regular task completions and daily routine execution calculated over rolling intervals.",
                metricLabel = "Completion Rate",
            )
            else -> FieldDetails(
                name = "Metric",
                iconRes = R.drawable.focus_icon,
                description = "Performance score based on tracked activities.",
                metricLabel = "Score",
            )
        }
    }

    val scoreValue = when (fieldIndex) {
        0 -> scores.focus
        1 -> scores.discipline
        2 -> scores.health
        3 -> scores.resilience
        4 -> scores.consistency
        else -> 0.0
    }

    val targetScore = scoreValue.roundToInt().coerceIn(0, 100)
    val accentColor = if (isShadow) ShadowPurple else KalliorColors.AccentOrange

    // Animated score counting from 0 to targetScore
    val animatedScore = remember { Animatable(0f) }
    LaunchedEffect(targetScore) {
        animatedScore.animateTo(
            targetValue = targetScore.toFloat(),
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        )
    }

    // Animated progression ring from 0 to targetProgress
    val ringProgress = remember { Animatable(0f) }
    val targetProgress = (targetScore / 100f).coerceIn(0f, 1f)
    LaunchedEffect(targetProgress) {
        ringProgress.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KalliorColors.SecondaryBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top App Bar with back navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(KalliorColors.PrimaryLayer)
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "<",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = if (isShadow) "Shadow ${fieldInfo.name}" else fieldInfo.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Philosopher,
                    fontWeight = FontWeight.Bold,
                    color = KalliorColors.NormalText,
                ),
            )

            // Balance placeholder to keep title centered
            Box(modifier = Modifier.size(44.dp))
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Progression Ring with Score and Icon inside
        Box(
            modifier = Modifier.size(230.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                // Background track
                drawArc(
                    color = KalliorColors.RadarLine.copy(alpha = 0.35f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )

                // Animated progress arc
                if (ringProgress.value > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to accentColor.copy(alpha = 0.75f),
                            0.6f to accentColor,
                            1.0f to accentColor,
                            center = center,
                        ),
                        startAngle = -90f,
                        sweepAngle = ringProgress.value * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }

            // Central content inside the ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(fieldInfo.iconRes),
                    contentDescription = fieldInfo.name,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${animatedScore.value.roundToInt()}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = Philosopher,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                )

                Text(
                    text = "/ 100",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = KalliorColors.MutedText,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Performance Tier Pill
        val tierText = when {
            targetScore >= 80 -> "Mastery"
            targetScore >= 60 -> "Strong"
            targetScore >= 40 -> "Developing"
            else -> "Needs Focus"
        }
        val tierBorderColor = when {
            targetScore >= 80 -> accentColor
            targetScore >= 60 -> accentColor.copy(alpha = 0.7f)
            targetScore >= 40 -> KalliorColors.MutedText.copy(alpha = 0.5f)
            else -> KalliorColors.DangerRed.copy(alpha = 0.7f)
        }

        Surface(
            color = KalliorColors.PrimaryLayer,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, tierBorderColor),
            modifier = Modifier.padding(bottom = 24.dp),
        ) {
            Text(
                text = tierText,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        // Details & Breakdown Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = KalliorColors.PrimaryLayer,
            border = BorderStroke(1.dp, KalliorColors.RadarLine.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = KalliorColors.NormalText,
                    ),
                )

                Text(
                    text = fieldInfo.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = KalliorColors.MutedText,
                        lineHeight = 22.sp,
                    ),
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Contextual breakdown metrics
                when (fieldIndex) {
                    0 -> { // Focus
                        MetricRow(
                            label = "Total Screen Time",
                            value = "${snapshot?.totalScreenMinutes?.toInt() ?: 0} min",
                        )
                        MetricRow(
                            label = "Entertainment Time",
                            value = "${snapshot?.entertainmentMinutes?.toInt() ?: 0} min",
                        )
                    }
                    1 -> { // Discipline
                        MetricRow(
                            label = "Blocker Bypasses",
                            value = "${snapshot?.blockerBypasses ?: 0}",
                        )
                        MetricRow(
                            label = "Blocker Attempts",
                            value = "${snapshot?.blockerAttempts ?: 0}",
                        )
                    }
                    2 -> { // Health
                        MetricRow(
                            label = "Daily Steps",
                            value = "${snapshot?.steps ?: 0}",
                        )
                        val sleepHours = ((snapshot?.minutesSlept ?: 0.0) / 60.0)
                        MetricRow(
                            label = "Sleep Duration",
                            value = String.format(java.util.Locale.US, "%.1f hrs", sleepHours),
                        )
                    }
                    3 -> { // Resilience
                        val rate = ((snapshot?.completionRate ?: 0.0) * 100).roundToInt()
                        MetricRow(
                            label = "Completion Rate",
                            value = "$rate%",
                        )
                    }
                    4 -> { // Consistency
                        val completed = snapshot?.tasksCompleted ?: 0
                        val scheduled = snapshot?.tasksScheduled ?: 0
                        MetricRow(
                            label = "Tasks Completed",
                            value = "$completed / $scheduled",
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = KalliorColors.MutedText,
            fontSize = 14.sp,
        )
        Text(
            text = value,
            color = KalliorColors.NormalText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private data class FieldDetails(
    val name: String,
    val iconRes: Int,
    val description: String,
    val metricLabel: String,
)
