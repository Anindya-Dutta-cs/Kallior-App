package org.example.project.health

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.awakeIntervalDataStore by preferencesDataStore(
    name = "awake_intervals"
)

@Serializable
data class AwakeIntervalInput(
    val anchorDate: String,   // yyyy-MM-dd — the wake date this interval belongs to
    val startTime: Long,      // epoch millis
    val endTime: Long,        // epoch millis
    val durationSeconds: Long,
    val source: String,       // "SCREEN_EVENTS" or "USAGE_STATS_BACKFILL"
)

/**
 * Stores phone-awake intervals that occurred during sleep windows.
 *
 * Uses DataStore for lightweight persistence. Intervals are stored per
 * anchor-date and merged (de-duplicated) before summation so overlapping
 * SCREEN_EVENTS and USAGE_STATS_BACKFILL intervals don't double-count.
 */
class AwakeIntervalRepository(private val context: Context) {

    private val dateKey = { date: String -> stringPreferencesKey("intervals_$date") }

    private val json = Json { ignoreUnknownKeys = true }

    /** Persist one awake interval. */
    suspend fun insert(input: AwakeIntervalInput) {
        val key = dateKey(input.anchorDate)
        context.awakeIntervalDataStore.edit { prefs ->
            val existing = prefs[key]?.let {
                json.decodeFromString<List<AwakeIntervalInput>>(it)
            } ?: emptyList()
            val updated = existing + input
            prefs[key] = json.encodeToString(updated)
        }
    }

    /** Total de-duplicated awake seconds for a given anchor date. */
    suspend fun totalMergedSecondsForDate(anchorDate: String): Long {
        val key = dateKey(anchorDate)
        val prefs = context.awakeIntervalDataStore.data.first()
        val intervals = prefs[key]?.let {
            json.decodeFromString<List<AwakeIntervalInput>>(it)
        } ?: return 0L

        return mergeAndSum(intervals)
    }

    /** Raw intervals for a given anchor date (for debugging / UI). */
    suspend fun intervalsForDate(anchorDate: String): List<AwakeIntervalInput> {
        val key = dateKey(anchorDate)
        val prefs = context.awakeIntervalDataStore.data.first()
        return prefs[key]?.let {
            json.decodeFromString<List<AwakeIntervalInput>>(it)
        } ?: emptyList()
    }

    /**
     * Merge overlapping intervals and return the total seconds.
     * This prevents double-counting from SCREEN_EVENTS + USAGE_STATS_BACKFILL.
     */
    private fun mergeAndSum(intervals: List<AwakeIntervalInput>): Long {
        if (intervals.isEmpty()) return 0L

        val sorted = intervals.sortedBy { it.startTime }
        val merged = mutableListOf<Pair<Long, Long>>()

        var currentStart = sorted[0].startTime
        var currentEnd = sorted[0].endTime

        for (i in 1 until sorted.size) {
            val interval = sorted[i]
            if (interval.startTime <= currentEnd) {
                // Overlapping — extend
                currentEnd = maxOf(currentEnd, interval.endTime)
            } else {
                // Gap — persist current and start new
                merged.add(currentStart to currentEnd)
                currentStart = interval.startTime
                currentEnd = interval.endTime
            }
        }
        merged.add(currentStart to currentEnd)

        return merged.sumOf { (start, end) -> (end - start) / 1000 }
    }

    /** Remove intervals older than the given anchor date. */
    suspend fun cleanupBefore(anchorDate: String) {
        context.awakeIntervalDataStore.edit { prefs ->
            val keysToRemove = prefs.asMap().keys.filterIsInstance<androidx.datastore.preferences.core.Preferences.Key<String>>()
                .filter { it.name.startsWith("intervals_") && it.name < "intervals_$anchorDate" }
            keysToRemove.forEach { prefs.remove(it) }
        }
    }
}
