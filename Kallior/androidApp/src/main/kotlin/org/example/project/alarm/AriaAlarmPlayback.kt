package org.example.project.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AriaAlarmPlayback {

    enum class Phase {
        IDLE,
        PREVIEW,
        PAUSED_AFTER_PREVIEW,
        FULL_PLAYBACK
    }

    data class UiState(
        val phase: Phase = Phase.IDLE,
        val songName: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun setPreview(songName: String?) {
        _uiState.update {
            it.copy(
                phase = Phase.PREVIEW,
                songName = songName
            )
        }
    }

    fun setPausedAfterPreview() {
        _uiState.update {
            it.copy(phase = Phase.PAUSED_AFTER_PREVIEW)
        }
    }

    fun setFullPlayback() {
        _uiState.update {
            it.copy(phase = Phase.FULL_PLAYBACK)
        }
    }

    fun reset() {
        _uiState.value = UiState()
    }
}
