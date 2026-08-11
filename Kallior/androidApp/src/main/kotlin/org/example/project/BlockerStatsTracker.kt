package org.example.project

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Counts how many times the user tried to open a blocked app today (an "attempt")
 * versus how many times they overrode the block with "Allow Until" (a "bypass").
 *
 * These two counters feed the Focus axis of the radar chart through
 * [PlatformMetricsCollector]. They are kept in memory only: the process starts at
 * zero and counts reset when the app is killed. The daily snapshot persisted by
 * GameViewModel captures the values at every refresh.
 */
object BlockerStatsTracker {
    private val attempts = AtomicInteger(0)
    private val bypasses = AtomicInteger(0)
    private val _updates = MutableStateFlow(BlockerStats(0, 0))

    /** Emits whenever an attempt or bypass changes so the radar can refresh immediately. */
    val updates: StateFlow<BlockerStats> = _updates

    /** Call when a blocked app is actually opened and its blocking overlay appears. */
    fun recordAttempt() {
        attempts.incrementAndGet()
        publish()
    }

    /**
     * Call after the displayed blocking overlay is overridden with "Allow Until".
     * The overlay opening has already recorded the corresponding attempt, so this
     * increments only the bypass counter while preserving the one-attempt-per-open
     * invariant.
     */
    fun recordBypass() {
        bypasses.incrementAndGet()
        publish()
    }

    val currentAttempts: Int get() = attempts.get()
    val currentBypasses: Int get() = bypasses.get()

    fun reset() {
        attempts.set(0)
        bypasses.set(0)
        publish()
    }

    private fun publish() {
        _updates.value = BlockerStats(currentAttempts, currentBypasses)
    }
}

data class BlockerStats(
    val attempts: Int,
    val bypasses: Int,
)
