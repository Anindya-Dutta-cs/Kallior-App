package org.example.project.ui

import android.os.Build
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.launch
import org.example.project.alarm.AlarmItem
import org.example.project.alarm.AriaAlarmTime
import org.example.project.alarm.AriaSong

/**
 * Create/edit interface for one alarm. [existing] == null creates a new alarm.
 * Used for both flows so the UI is not duplicated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AriaAlarmEditorSheet(
    existing: AlarmItem?,
    songs: List<AriaSong>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        hour: Int,
        minute: Int,
        repeatDays: Set<Int>,
        songId: String?,
        enabled: Boolean
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var hour by remember { mutableIntStateOf(existing?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(existing?.minute ?: 0) }
    var repeatDays by remember { mutableStateOf(existing?.repeatDays ?: AlarmItem.ALL_DAYS) }
    var songId by remember { mutableStateOf(existing?.songId) }
    var enabled by remember { mutableStateOf(existing?.enabled ?: true) }
    var showTimePicker by remember { mutableStateOf(false) }
    var songPickerExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val previewState by AriaSongPreview.state.collectAsState()

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KalliorColors.PrimaryLayer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(KalliorColors.InactiveNav.copy(alpha = 0.5f))
            )
        }
    ) {
        // Blend the system navigation bar into the sheet surface instead of
        // leaving a contrasting strip below the rounded corners.
        val sheetView = LocalView.current
        SideEffect {
            val window = (sheetView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.navigationBarColor = KalliorColors.PrimaryLayer.toArgb()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (existing == null) "New Alarm" else "Edit Alarm",
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = KalliorColors.NormalText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Alarm name", color = KalliorColors.InactiveNav) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = KalliorColors.NormalText,
                    unfocusedTextColor = KalliorColors.NormalText,
                    cursorColor = KalliorColors.AccentOrange,
                    focusedBorderColor = KalliorColors.AccentOrange.copy(alpha = 0.6f),
                    unfocusedBorderColor = KalliorColors.Hairline,
                    focusedContainerColor = KalliorColors.SurfaceCharcoal,
                    unfocusedContainerColor = KalliorColors.SurfaceCharcoal
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // Time selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(KalliorColors.SurfaceCharcoal)
                    .clickable { showTimePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time",
                    fontSize = 14.sp,
                    color = KalliorColors.MutedText
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatClock(context, hour, minute),
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = KalliorColors.AccentOrange
                )
            }

            Spacer(Modifier.height(14.dp))

            // Repeat-days selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(KalliorColors.SurfaceCharcoal)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Repeat",
                        fontSize = 14.sp,
                        color = KalliorColors.MutedText
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = AriaAlarmTime.repeatDaysLabel(repeatDays),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KalliorColors.AccentOrange
                    )
                }

                Spacer(Modifier.height(12.dp))

                DayOfWeekSelector(
                    selectedDays = repeatDays,
                    onToggleDay = { day ->
                        repeatDays = if (day in repeatDays) {
                            // Never allow deselecting the last active day.
                            if (repeatDays.size > 1) repeatDays - day else repeatDays
                        } else {
                            repeatDays + day
                        }
                    }
                )
            }

            Spacer(Modifier.height(14.dp))

            // Song selector (expandable library inside the sheet)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(KalliorColors.SurfaceCharcoal)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { songPickerExpanded = !songPickerExpanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Song",
                        fontSize = 14.sp,
                        color = KalliorColors.MutedText
                    )
                    Spacer(Modifier.weight(1f))
                    val selectedSong = songs.firstOrNull { it.id == songId }
                    Text(
                        text = selectedSong?.title ?: "No song selected",
                        fontSize = 15.sp,
                        color = if (selectedSong != null) KalliorColors.NormalText else KalliorColors.InactiveNav,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(2f)
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (songPickerExpanded) "Collapse song list" else "Choose song",
                        tint = KalliorColors.MutedText,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(20.dp)
                            .graphicsLayer { rotationZ = if (songPickerExpanded) 180f else 0f }
                    )
                }

                AnimatedVisibility(visible = songPickerExpanded) {
                    if (songs.isEmpty()) {
                        Text(
                            text = "Your music library is empty. Add MP3s from the Music section.",
                            fontSize = 13.sp,
                            color = KalliorColors.MutedText,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 14.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            item(key = "none") {
                                SongPickRow(
                                    title = "No song",
                                    selected = songId == null,
                                    isPlaying = false,
                                    showPreview = false,
                                    onPick = { songId = null },
                                    onPreview = {}
                                )
                            }
                            items(songs, key = { it.id }) { song ->
                                SongPickRow(
                                    title = song.title,
                                    selected = songId == song.id,
                                    isPlaying = previewState.songId == song.id,
                                    onPick = { songId = song.id },
                                    onPreview = { AriaSongPreview.toggle(song) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Enabled toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(KalliorColors.SurfaceCharcoal)
                    .toggleable(
                        value = enabled,
                        role = Role.Switch,
                        onValueChange = { enabled = it }
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enabled",
                    fontSize = 14.sp,
                    color = KalliorColors.NormalText
                )
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = KalliorColors.AccentOrange,
                        uncheckedThumbColor = KalliorColors.InactiveNav,
                        uncheckedTrackColor = KalliorColors.ForegroundCard,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text(
                            "Delete",
                            color = KalliorColors.DangerRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                TextButton(onClick = { close() }) {
                    Text("Cancel", color = KalliorColors.MutedText)
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        // A song deleted while the sheet was open falls back to none.
                        val validSongId = songId?.takeIf { id -> songs.any { it.id == id } }
                        onSave(
                            name.trim().ifBlank { "Alarm" },
                            hour,
                            minute,
                            repeatDays,
                            validSongId,
                            enabled
                        )
                        close()
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KalliorColors.AccentOrange,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Save",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        AriaTimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { pickedHour, pickedMinute ->
                hour = pickedHour
                minute = pickedMinute
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showDeleteConfirm) {
        AriaDeleteAlarmDialog(
            alarmName = existing?.name ?: "Alarm",
            onConfirm = {
                showDeleteConfirm = false
                onDelete?.invoke()
                close()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

/** Confirmation guard so alarms cannot be deleted by a single accidental tap. */
@Composable
internal fun AriaDeleteAlarmDialog(
    alarmName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KalliorColors.PrimaryLayer,
        titleContentColor = KalliorColors.NormalText,
        textContentColor = KalliorColors.MutedText,
        title = {
            Text(
                "Delete alarm?",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Text(
                "This removes \"$alarmName\" and cancels its schedule.",
                fontSize = 15.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Delete",
                    color = KalliorColors.DangerRed,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = KalliorColors.MutedText)
            }
        }
    )
}

@Composable
private fun SongPickRow(
    title: String,
    selected: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    showPreview: Boolean = true,
    onPick: () -> Unit,
    onPreview: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) KalliorColors.AccentOrange.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onPick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = KalliorColors.MutedText,
            modifier = Modifier.size(16.dp)
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) KalliorColors.NormalText else KalliorColors.MutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isPlaying) {
            MiniEqualizer(modifier = Modifier.padding(end = 4.dp))
        }

        if (showPreview) {
            IconButton(onClick = onPreview, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop preview of $title" else "Preview $title",
                    tint = KalliorColors.NormalText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = KalliorColors.AccentOrange,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp)
            )
        }
    }
}

/** Dark-styled time input dialog honouring the device's 12/24-hour preference. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AriaTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)

    val state = remember(is24Hour) {
        TimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = is24Hour
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
                    window.navigationBarColor = 0xFF161616.toInt()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
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
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
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
