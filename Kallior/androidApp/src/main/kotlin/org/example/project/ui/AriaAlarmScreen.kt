package org.example.project.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import org.example.project.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.alarm.AriaAlarmPreferences
import org.example.project.alarm.AriaAlarmScheduler
import org.example.project.alarm.AriaMusicRepository
import java.io.File
import java.util.Locale

@Composable
fun AriaAlarmScreen(onMenuClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val repository = remember { AriaMusicRepository(context) }
    val prefs = remember { AriaAlarmPreferences(context) }
    val alarmManager = remember {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    var songs by remember { mutableStateOf<List<File>>(emptyList()) }
    var alarmEnabled by remember { mutableStateOf(prefs.enabled) }
    var hour by remember { mutableIntStateOf(prefs.hour) }
    var minute by remember { mutableIntStateOf(prefs.minute) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    var canScheduleExact by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        )
    }

    // Re-check exact alarm permission when returning from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        canScheduleExact = alarmManager.canScheduleExactAlarms()
                    }
                    songs = repository.songs()
                }
            }
        )
    }

    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && alarmEnabled) {
            val scheduled = AriaAlarmScheduler.schedule(context, hour, minute)
            if (!scheduled) {
                canScheduleExact = false
            }
        }
    }

    // SAF document picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isImporting = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    repository.importMp3Files(uris)
                }
                isImporting = false
                songs = repository.songs()

                val msg = if (result.skipped > 0) {
                    "Imported ${result.imported.size} song(s), skipped ${result.skipped} non-MP3 file(s)."
                } else {
                    "Imported ${result.imported.size} song(s)."
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        songs = repository.songs()
    }

    // Scrollable background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KalliorColors.SecondaryBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 36.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Menu Button
            Icon(
                painter = painterResource(R.drawable.side_bar_button),
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onMenuClick() }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Title
            Text(
                text = "AriaAlarm",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp
                ),
                color = KalliorColors.NormalText
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Upload your .mp3 files and start\nyour morning with a bang",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ),
                color = KalliorColors.NormalText
            )

            Spacer(modifier = Modifier.height(60.dp))

            // ── Songs Section ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
            ) {
                Column {
                    // Song card header row (+ button)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Songs (${songs.size})",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = KalliorColors.NormalText
                        )

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(KalliorColors.AccentOrange)
                                .clickable(enabled = !isImporting) {
                                    documentPickerLauncher.launch(arrayOf("audio/*"))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (isImporting) "Importing…" else "Upload",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Song list card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(songsListHeight(songs.size))
                            .clip(RoundedCornerShape(24.dp))
                            .background(KalliorColors.PrimaryLayer),
                        contentAlignment = if (songs.isEmpty()) Alignment.Center else Alignment.TopStart
                    ) {
                        if (songs.isEmpty()) {
                            Text(
                                text = if (isImporting) "Importing…" else "Tap the '+' to upload",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 18.sp
                                ),
                                color = KalliorColors.MutedText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(24.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                userScrollEnabled = songs.size > 6
                            ) {
                                items(items = songs, key = { it.absolutePath }) { song ->
                                    SongRow(
                                        name = song.nameWithoutExtension,
                                        onDelete = {
                                            repository.deleteSong(song)
                                            songs = repository.songs()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Alarm Settings Section ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(KalliorColors.PrimaryLayer)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Alarm",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = KalliorColors.NormalText
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Time selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(KalliorColors.ForegroundCard)
                            .clickable { showTimePicker = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Time",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontSize = 18.sp
                            ),
                            color = KalliorColors.NormalText
                        )

                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = KalliorColors.AccentOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enable/disable toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(KalliorColors.ForegroundCard)
                            .clickable {
                                val newState = !alarmEnabled
                                alarmEnabled = newState
                                prefs.enabled = newState

                                if (newState) {
                                    enableAlarm(context, prefs, alarmManager, hour, minute) { perm ->
                                        if (perm == "notification") {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            canScheduleExact = false
                                        }
                                    }
                                } else {
                                    AriaAlarmScheduler.cancel(context)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enabled",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontSize = 18.sp
                            ),
                            color = KalliorColors.NormalText
                        )

                        Switch(
                            checked = alarmEnabled,
                            onCheckedChange = { enabled ->
                                alarmEnabled = enabled
                                prefs.enabled = enabled

                                if (enabled) {
                                    enableAlarm(context, prefs, alarmManager, hour, minute) { perm ->
                                        if (perm == "notification") {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            canScheduleExact = false
                                        }
                                    }
                                } else {
                                    AriaAlarmScheduler.cancel(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = KalliorColors.AccentOrange,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = KalliorColors.ForegroundCard
                            )
                        )
                    }

                    // Permission warnings
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact && alarmEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Exact alarm permission is required. Tap to grant.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 14.sp
                            ),
                            color = KalliorColors.AccentOrange,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val intent = Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Time Picker Dialog ─────────────────────────────────────
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { h, m ->
                hour = h
                minute = m
                prefs.hour = h
                prefs.minute = m

                if (alarmEnabled) {
                    AriaAlarmScheduler.cancel(context)
                    enableAlarm(context, prefs, alarmManager, h, m) { perm ->
                        if (perm == "notification") {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            canScheduleExact = false
                        }
                    }
                }
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun SongRow(
    name: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp
            ),
            color = KalliorColors.NormalText,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete song",
                tint = KalliorColors.DangerRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = remember {
        TimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KalliorColors.PrimaryLayer,
        titleContentColor = KalliorColors.NormalText,
        textContentColor = KalliorColors.NormalText,
        title = {
            val view = LocalView.current
            SideEffect {
                val window = (view.parent as? DialogWindowProvider)?.window
                if (window != null) {
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                }
            }
            Text(
                text = "Select alarm time",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimeInput(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(state.hour, state.minute)
            }) {
                Text(
                    "OK",
                    color = KalliorColors.AccentOrange,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = KalliorColors.MutedText
                )
            }
        }
    )
}

/**
 * Attempts to enable the alarm, requesting necessary permissions first.
 * Calls [onPermissionNeeded] with either "notification" or "schedule_exact_alarm"
 * if a permission is missing, so the caller can launch the appropriate intent.
 */
private fun enableAlarm(
    context: Context,
    prefs: AriaAlarmPreferences,
    alarmManager: AlarmManager,
    hour: Int,
    minute: Int,
    onPermissionNeeded: (String) -> Unit
) {
    // Check notification permission (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionNeeded("notification")
            return
        }
    }

    // Check exact alarm permission (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            onPermissionNeeded("schedule_exact_alarm")
            prefs.enabled = false
            return
        }
    }

    val scheduled = AriaAlarmScheduler.schedule(context, hour, minute)
    if (!scheduled) {
        onPermissionNeeded("schedule_exact_alarm")
        prefs.enabled = false
    }
}

/** Fixed height so the nested [LazyColumn] renders without an inner scroll. */
private fun songsListHeight(count: Int): androidx.compose.ui.unit.Dp {
    if (count <= 0) return 200.dp
    val row = 48.dp
    val gap = 4.dp
    val content = row * count.coerceAtMost(6) + gap * (count.coerceAtMost(6) - 1)
    return content + 24.dp // padding
}
