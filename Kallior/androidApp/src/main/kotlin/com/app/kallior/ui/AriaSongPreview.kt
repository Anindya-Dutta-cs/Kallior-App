package com.app.kallior.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.app.kallior.alarm.AriaSong

/**
 * Lightweight in-app song previewer for the music library and song picker.
 * Only one preview plays at a time; [stop] must be called when the screen
 * hosting previews goes away.
 */
object AriaSongPreview {

    data class State(
        val songId: String? = null,
        val isPlaying: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var player: MediaPlayer? = null

    fun toggle(song: AriaSong) {
        if (_state.value.songId == song.id) {
            stop()
            return
        }

        stop()
        _state.value = State(songId = song.id, isPlaying = false)

        try {
            val newPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(song.file.absolutePath)
                setOnPreparedListener { prepared ->
                    prepared.start()
                    _state.value = State(songId = song.id, isPlaying = true)
                }
                setOnCompletionListener { stop() }
                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
                prepareAsync()
            }
            player = newPlayer
        } catch (_: Exception) {
            stop()
        }
    }

    fun stop() {
        player?.let { current ->
            try {
                if (current.isPlaying) current.stop()
            } catch (_: Exception) {
            }
            try {
                current.release()
            } catch (_: Exception) {
            }
        }
        player = null
        _state.value = State()
    }
}
