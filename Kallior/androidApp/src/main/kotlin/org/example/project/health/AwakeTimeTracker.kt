package org.example.project.health

import java.time.Duration
import java.time.Instant

/**
 * Tracks phone usage during the user's sleep window and persists awake
 * intervals to the [AwakeIntervalRepository].
 *
 * State machine:
 * ```
 * IDLE
 *   │ screen on + unlocked + inside sleep window
 *   ▼
 * TRACKING(startedAt)
 *   │ screen off / locked / sleep window ends
 *   ▼
 * IDLE + persist interval
 * ```
 *
 * Per plan section 11.3: only counts as usage after ACTION_USER_PRESENT
 * (not mere screen-on from notifications).
 */
class AwakeTimeTracker(
    private val sleepWindowCalculator: SleepWindowCalculator,
    private val awakeIntervalRepository: AwakeIntervalRepository,
) {
    @Volatile
    private var trackingStart: Instant? = null

    /**
     * Called when the user unlocks the phone (ACTION_USER_PRESENT).
     * Starts tracking if [now] falls inside the current sleep window.
     */
    suspend fun onPhoneUsageStarted(now: Instant, schedule: SleepSchedule?) {
        if (schedule == null) return
        val window = sleepWindowCalculator.currentOrMostRecentSleepWindow(now, schedule)
        if (!window.contains(now)) return
        if (trackingStart == null) {
            trackingStart = now
        }
    }

    /**
     * Called when the screen turns off (ACTION_SCREEN_OFF).
     * Persists the interval if we were tracking.
     */
    suspend fun onPhoneUsageStopped(now: Instant, schedule: SleepSchedule?) {
        if (schedule == null) return
        val start = trackingStart ?: return
        val window = sleepWindowCalculator.currentOrMostRecentSleepWindow(now, schedule)
        persistInterval(start, now, window)
        trackingStart = null
    }

    /**
     * Periodic tick — if the sleep window has ended while the phone is
     * still in use, flush the current interval clipped to the window end.
     * While still inside the window, checkpoint the running session so a
     * process kill cannot lose the whole interval (the repository merges
     * the fragments on read).
     */
    suspend fun onTick(now: Instant, schedule: SleepSchedule?) {
        if (schedule == null) return
        val start = trackingStart ?: return
        val window = sleepWindowCalculator.currentOrMostRecentSleepWindow(now, schedule)
        if (!window.contains(now)) {
            persistInterval(start, window.end.coerceAtMost(now), window)
            trackingStart = null
            return
        }
        if (Duration.between(start, now) >= CHECKPOINT_EVERY) {
            persistInterval(start, now, window)
            trackingStart = now
        }
    }

    /** Whether we are currently tracking a usage session. */
    val isTracking: Boolean get() = trackingStart != null

    private suspend fun persistInterval(
        start: Instant,
        end: Instant,
        window: SleepWindow,
    ) {
        val clippedStart = maxOf(start, window.start)
        val clippedEnd = minOf(end, window.end)
        if (!clippedEnd.isAfter(clippedStart)) return

        val durationSeconds = Duration.between(clippedStart, clippedEnd).seconds
        awakeIntervalRepository.insert(
            AwakeIntervalInput(
                anchorDate = window.anchorDate.toString(),
                startTime = clippedStart.toEpochMilli(),
                endTime = clippedEnd.toEpochMilli(),
                durationSeconds = durationSeconds,
                source = "SCREEN_EVENTS",
            )
        )
    }

    private companion object {
        /** How often an in-progress session is persisted as a checkpoint. */
        val CHECKPOINT_EVERY: Duration = Duration.ofMinutes(1)
    }
}
