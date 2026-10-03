package com.app.kallior.ui

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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.app.kallior.alarm.AriaAlarmScheduler
import com.app.kallior.alarm.AriaAlarmStore
import com.app.kallior.alarm.AriaAlarmTime
import com.app.kallior.alarm.AlarmItem
import com.app.kallior.alarm.AriaMusicRepository
import com.app.kallior.alarm.AriaSong

@Composable
fun AriaAlarmScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val repository = remember { AriaMusicRepository(context) }
    val store = remember { AriaAlarmStore(context) }
    val alarmManager = remember {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    var alarms by remember { mutableStateOf(store.loadAll()) }
    var songs by remember { mutableStateOf(repository.songs()) }
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
    var editorTarget by remember { mutableStateOf<AlarmItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<AlarmItem?>(null) }
    var selectedAlarmId by remember { mutableStateOf<Long?>(null) }
    var pendingEnableId by remember { mutableStateOf<Long?>(null) }
    var pendingNotifResult by remember { mutableStateOf<Boolean?>(null) }
    // The alarm card whose context menu is open, if any. While one is
    // focused, the rest of the screen recedes behind a soft blur.
    var focusedMenuAlarmId by remember { mutableStateOf<Long?>(null) }

    val previewState by AriaSongPreview.state.collectAsState()

    // 0 = everything normal, 1 = a context menu is focused and the rest of
    // the screen is receded. Shared so every section moves together.
    val menuRecede by animateFloatAsState(
        targetValue = if (focusedMenuAlarmId != null) 1f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "menuRecede"
    )

    val nextAlarm = remember(alarms) {
        alarms.filter { it.enabled }.minByOrNull {
            AriaAlarmTime.nextTriggerAt(it.hour, it.minute, it.repeatDays)
        }
    }

    // The hero shows the alarm tapped in the list; when the selection is gone
    // (never made, or the alarm was deleted) it falls back to the next alarm.
    val heroAlarm = alarms.firstOrNull { it.id == selectedAlarmId } ?: nextAlarm

    fun refreshAlarms() {
        alarms = store.loadAll()
    }

    fun refreshSongs() {
        songs = repository.songs()
    }

    // Notification permission request (Android 13+); the result is consumed below.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingNotifResult = granted
    }

    fun scheduleEnabledAlarm(alarm: AlarmItem) {
        val scheduled = AriaAlarmScheduler.schedule(context, alarm)
        if (!scheduled) {
            store.setEnabled(alarm.id, false)
            refreshAlarms()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                canScheduleExact = false
            }
        }
    }

    fun commitEnable(alarm: AlarmItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingEnableId = alarm.id
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        scheduleEnabledAlarm(alarm)
    }

    fun toggleAlarm(alarm: AlarmItem, enabled: Boolean) {
        store.setEnabled(alarm.id, enabled)
        refreshAlarms()

        if (enabled) {
            commitEnable(alarm)
        } else {
            AriaAlarmScheduler.cancel(context, alarm.id)
        }
    }

    fun saveAlarm(
        name: String,
        hour: Int,
        minute: Int,
        repeatDays: Set<Int>,
        songId: String?,
        enabled: Boolean
    ) {
        val target = editorTarget
        val alarm = if (target != null) {
            AlarmItem(
                id = target.id,
                name = name,
                hour = hour,
                minute = minute,
                enabled = enabled,
                songId = songId,
                repeatDays = repeatDays
            ).also { store.upsert(it) }
        } else {
            store.insert(
                name = name,
                hour = hour,
                minute = minute,
                enabled = enabled,
                songId = songId,
                repeatDays = repeatDays
            )
        }
        refreshAlarms()

        // A newly created alarm may have changed the schedule; return the
        // hero to its next-alarm duty. Edits keep the current selection.
        if (target == null) {
            selectedAlarmId = null
        }

        if (enabled) {
            commitEnable(alarm)
        } else {
            AriaAlarmScheduler.cancel(context, alarm.id)
        }
    }

    fun deleteAlarm(id: Long) {
        AriaAlarmScheduler.cancel(context, id)
        store.delete(id)
        refreshAlarms()
    }

    fun deleteSong(song: AriaSong) {
        if (previewState.songId == song.id) {
            AriaSongPreview.stop()
        }
        repository.deleteSong(song.id)
        // Alarms referencing this song fall back to "No song selected".
        store.clearSong(song.id)
        refreshSongs()
        refreshAlarms()
    }

    // SAF document picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isImporting = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    repository.importMp3Files(uris)
                }
                isImporting = false
                refreshSongs()

                val msg = if (result.skipped > 0) {
                    "Imported ${result.imported.size} song(s), skipped ${result.skipped} non-MP3 file(s)."
                } else {
                    "Imported ${result.imported.size} song(s)."
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Consume a notification permission result: schedule the exact alarm that
    // was being enabled — either way, the alarm can still ring without notifications.
    LaunchedEffect(pendingNotifResult) {
        val granted = pendingNotifResult ?: return@LaunchedEffect
        pendingNotifResult = null
        val id = pendingEnableId
        pendingEnableId = null
        val alarm = id?.let { store.find(it) }
        if (alarm != null && alarm.enabled) {
            scheduleEnabledAlarm(alarm)
        }
    }

    // Re-check exact alarm permission and refresh data when returning to the screen.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        canScheduleExact = alarmManager.canScheduleExactAlarms()
                    }
                    refreshSongs()
                    refreshAlarms()
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            AriaSongPreview.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KalliorColors.SecondaryBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 150.dp)
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .entrance(0L)
                        .menuRecede(menuRecede)
                ) {
                    Text(
                        text = "AriaAlarm",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        ),
                        color = KalliorColors.NormalText
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Wake up to something worth hearing.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        ),
                        color = KalliorColors.MutedText
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !canScheduleExact &&
                alarms.any { it.enabled }
            ) {
                item(key = "banner") {
                    Text(
                        text = "Exact alarm permission is required. Tap to grant.",
                        fontSize = 13.sp,
                        color = KalliorColors.AccentOrange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .menuRecede(menuRecede)
                            .clip(RoundedCornerShape(12.dp))
                            .background(KalliorColors.AccentOrange.copy(alpha = 0.08f))
                            .clickable {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            item(key = "hero") {
                NextAlarmHero(
                    alarm = heroAlarm,
                    nextAlarmId = nextAlarm?.id,
                    songs = songs,
                    previewSongId = previewState.songId,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .entrance(90L)
                        .menuRecede(menuRecede),
                    onToggle = { alarmId, enabled ->
                        alarms.firstOrNull { it.id == alarmId }?.let {
                            toggleAlarm(it, enabled)
                        }
                    },
                    onClick = { alarm ->
                        editorTarget = alarm
                        showEditor = true
                    }
                )
            }

            item(key = "alarmsHeader") {
                SectionHeader(
                    title = "Alarms",
                    modifier = Modifier
                        .padding(top = 28.dp)
                        .entrance(160L)
                        .menuRecede(menuRecede)
                ) {
                    AccentAddButton(description = "Add alarm") {
                        editorTarget = null
                        showEditor = true
                    }
                }
            }

            if (alarms.isEmpty()) {
                item(key = "alarmsEmpty") {
                    AriaEmptyState(
                        title = "No alarms yet",
                        body = "Create your first alarm and wake up\nto your own soundtrack.",
                        actionLabel = "+ Create Alarm",
                        modifier = Modifier.padding(top = 12.dp).menuRecede(menuRecede),
                        onAction = {
                            editorTarget = null
                            showEditor = true
                        }
                    )
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        isNext = nextAlarm?.id == alarm.id,
                        songTitle = songs.firstOrNull { it.id == alarm.songId }?.title,
                        isSelected = selectedAlarmId == alarm.id,
                        isMenuFocused = focusedMenuAlarmId == alarm.id,
                        recedeProgress = menuRecede,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .animateItem(),
                        onToggle = { enabled -> toggleAlarm(alarm, enabled) },
                        onSelect = { selectedAlarmId = alarm.id },
                        onEdit = {
                            editorTarget = alarm
                            showEditor = true
                        },
                        onDelete = { deleteTarget = alarm },
                        onMenuFocusChange = { focused ->
                            focusedMenuAlarmId = if (focused) alarm.id else null
                        }
                    )
                }
            }

            item(key = "musicHeader") {
                SectionHeader(
                    title = "Music (${songs.size})",
                    modifier = Modifier
                        .padding(top = 28.dp)
                        .entrance(240L)
                        .menuRecede(menuRecede)
                ) {
                    AccentAddButton(description = "Add song", enabled = !isImporting) {
                        documentPickerLauncher.launch(arrayOf("audio/*"))
                    }
                }
            }

            if (songs.isEmpty()) {
                item(key = "musicEmpty") {
                    AriaEmptyState(
                        title = if (isImporting) "Importing…" else "Your music library is empty",
                        body = if (isImporting) {
                            "Adding your songs to AriaAlarm."
                        } else {
                            "Add an MP3\nto personalize your alarms."
                        },
                        actionLabel = "Add Song",
                        modifier = Modifier.padding(top = 12.dp).menuRecede(menuRecede),
                        onAction = {
                            documentPickerLauncher.launch(arrayOf("audio/*"))
                        }
                    )
                }
            } else {
                items(songs, key = { it.id }) { song ->
                    SongLibraryRow(
                        song = song,
                        isPreviewing = previewState.songId == song.id,
                        loadArtist = { repository.artistOf(it) },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .animateItem()
                            .menuRecede(menuRecede),
                        onTogglePreview = { AriaSongPreview.toggle(song) },
                        onDelete = { deleteSong(song) }
                    )
                }
            }

            item(key = "bottomSpace") {
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showEditor) {
        AriaAlarmEditorSheet(
            existing = editorTarget,
            songs = songs,
            onDismiss = {
                showEditor = false
                editorTarget = null
            },
            onSave = { name, hour, minute, repeatDays, songId, enabled ->
                saveAlarm(name, hour, minute, repeatDays, songId, enabled)
            },
            onDelete = editorTarget?.let { target ->
                {
                    deleteAlarm(target.id)
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AriaDeleteAlarmDialog(
            alarmName = target.name,
            onConfirm = {
                deleteTarget = null
                deleteAlarm(target.id)
            },
            onDismiss = { deleteTarget = null }
        )
    }
}
