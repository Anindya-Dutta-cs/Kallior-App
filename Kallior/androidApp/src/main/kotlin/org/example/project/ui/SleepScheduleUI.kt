package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import android.os.Build
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch
import org.example.project.health.HealthDependencies
import org.example.project.health.SleepSchedule
import kallos.platform.PlatformDataFetcher

/**
 * A card shown on the home screen when the user has no sleep schedule set
 * and Health Connect sleep data is unavailable.
 *
 * Tapping it opens the [SleepScheduleDialog].
 */
@Composable
fun SleepScheduleCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val store = remember { HealthDependencies.sleepScheduleStore(context) }
    var hasSchedule by remember { mutableStateOf(true) } // assume true until loaded
    var healthConnectHasSleep by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        hasSchedule = store.hasSchedule()
        if (!hasSchedule) {
            // If Health Connect provides sleep data, hide the prompt even
            // without a user schedule (plan §16.2).
            val fetcher = PlatformDataFetcher()
            val sleepData = fetcher.getSleepData()
            healthConnectHasSleep = sleepData.value > 0.0
        }
    }

    // Only show this card when both user schedule AND Health Connect sleep
    // are unavailable.
    if (hasSchedule || healthConnectHasSleep) return

    Column(modifier = modifier) {
        Text(
            text = "Sleep Tracking",
            style = MaterialTheme.typography.headlineSmall,
            color = KalliorColors.NormalText,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(KalliorColors.ForegroundCard)
                .clickable(enabled = enabled) { showDialog = true }
                .padding(20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "🌙",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Set your sleep schedule",
                    style = MaterialTheme.typography.titleMedium,
                    color = KalliorColors.NormalText,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "To estimate your sleep better, tell us\nwhen you usually sleep and wake up.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KalliorColors.MutedText,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showDialog) {
        SleepScheduleDialog(
            onDismiss = { showDialog = false },
            onConfirm = { schedule ->
                scope.launch {
                    store.saveSchedule(schedule)
                    hasSchedule = true
                }
                showDialog = false
            },
        )
    }
}

/**
 * Dialog that lets the user set their usual sleep and wake times via
 * Material3 time pickers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (SleepSchedule) -> Unit,
) {
    var showSleepPicker by remember { mutableStateOf(false) }
    var showWakePicker by remember { mutableStateOf(false) }

    val sleepTimeState = rememberTimePickerState(initialHour = 23, initialMinute = 0)
    val wakeTimeState = rememberTimePickerState(initialHour = 7, initialMinute = 0)

    if (showSleepPicker) {
        TimePickerDialog(
            onDismissRequest = { showSleepPicker = false },
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
                Text("When do you usually sleep?")
            },
            confirmButton = {
                TextButton(onClick = { showSleepPicker = false }) {
                    Text("OK", color = KalliorColors.AccentOrange)
                }
            },
        ) {
            TimePicker(state = sleepTimeState)
        }
    }

    if (showWakePicker) {
        TimePickerDialog(
            onDismissRequest = { showWakePicker = false },
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
                Text("When do you usually wake up?")
            },
            confirmButton = {
                TextButton(onClick = { showWakePicker = false }) {
                    Text("OK", color = KalliorColors.AccentOrange)
                }
            },
        ) {
            TimePicker(state = wakeTimeState)
        }
    }

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
            Text(
                text = "Sleep Schedule",
                color = KalliorColors.NormalText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "We use this to estimate your sleep and track phone usage during sleep hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KalliorColors.MutedText,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TimePickerButton(
                        label = "Sleep",
                        hour = sleepTimeState.hour,
                        minute = sleepTimeState.minute,
                        onClick = { showSleepPicker = true },
                        modifier = Modifier.weight(1f),
                    )
                    TimePickerButton(
                        label = "Wake",
                        hour = wakeTimeState.hour,
                        minute = wakeTimeState.minute,
                        onClick = { showWakePicker = true },
                        modifier = Modifier.weight(1f),
                    )
                }

                val durationHours = SleepSchedule(
                    sleepTimeState.hour,
                    sleepTimeState.minute,
                    wakeTimeState.hour,
                    wakeTimeState.minute,
                ).durationHours()

                Text(
                    text = "≈ ${String.format("%.1f", durationHours)} hours of sleep",
                    style = MaterialTheme.typography.labelMedium,
                    color = KalliorColors.AccentOrange,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(KalliorColors.AccentOrange)
                    .clickable {
                        onConfirm(
                            SleepSchedule(
                                sleepHour = sleepTimeState.hour,
                                sleepMinute = sleepTimeState.minute,
                                wakeHour = wakeTimeState.hour,
                                wakeMinute = wakeTimeState.minute,
                            )
                        )
                    }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = KalliorColors.MutedText)
            }
        },
    )
}

@Composable
private fun TimePickerButton(
    label: String,
    hour: Int,
    minute: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KalliorColors.ForegroundCard)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = KalliorColors.MutedText,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format("%02d:%02d", hour, minute),
            style = MaterialTheme.typography.headlineSmall,
            color = KalliorColors.NormalText,
            fontWeight = FontWeight.Bold,
        )
    }
}
