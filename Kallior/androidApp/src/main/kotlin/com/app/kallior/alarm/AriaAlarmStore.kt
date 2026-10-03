package com.app.kallior.alarm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * Immutable data-class for a single alarm.
 *
 * [id] is a stable, monotonically increasing number that also derives the
 * alarm's PendingIntent request code. [songId] references an imported song by
 * its file name inside the [AriaMusicRepository] library (null = no song).
 * [repeatDays] holds Calendar.DAY_OF_WEEK constants; empty or all seven
 * means the alarm fires every day.
 */
data class AlarmItem(
    val id: Long,
    val name: String = "Alarm",
    val hour: Int = 7,
    val minute: Int = 0,
    val enabled: Boolean = true,
    val songId: String? = null,
    val repeatDays: Set<Int> = ALL_DAYS
) {
    /** Stable PendingIntent request code for this alarm's broadcast. */
    val requestCode: Int get() = REQUEST_CODE_BASE + id.toInt()

    companion object {
        internal const val REQUEST_CODE_BASE = 5100

        val WEEKDAYS: Set<Int> = setOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY
        )
        val WEEKEND: Set<Int> = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
        val ALL_DAYS: Set<Int> = WEEKDAYS + WEEKEND
    }
}

/**
 * Manages multiple alarms, persisted as a JSON array in SharedPreferences.
 *
 * On first access the legacy single-alarm [AriaAlarmPreferences] entry is
 * migrated into one [AlarmItem] and the legacy PendingIntent is cancelled
 * (per-alarm scheduling replaces it). The legacy keys themselves are left
 * untouched so existing data can never be lost.
 */
class AriaAlarmStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateIfNeeded()
    }

    @Synchronized
    fun loadAll(): List<AlarmItem> {
        val json = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        return decode(json)
    }

    fun find(id: Long): AlarmItem? = loadAll().firstOrNull { it.id == id }

    @Synchronized
    fun insert(
        name: String,
        hour: Int,
        minute: Int,
        enabled: Boolean,
        songId: String?,
        repeatDays: Set<Int> = AlarmItem.ALL_DAYS
    ): AlarmItem {
        val id = prefs.getLong(KEY_NEXT_ID, 1L)
        prefs.edit().putLong(KEY_NEXT_ID, id + 1L).apply()

        val alarm = AlarmItem(
            id = id,
            name = name,
            hour = hour,
            minute = minute,
            enabled = enabled,
            songId = songId,
            repeatDays = repeatDays
        )
        saveAll(loadAll() + alarm)
        return alarm
    }

    @Synchronized
    fun upsert(alarm: AlarmItem) {
        val list = loadAll().toMutableList()
        val index = list.indexOfFirst { it.id == alarm.id }
        if (index >= 0) list[index] = alarm else list.add(alarm)
        saveAll(list)
    }

    @Synchronized
    fun setEnabled(id: Long, enabled: Boolean) {
        val list = loadAll()
        if (list.none { it.id == id && it.enabled != enabled }) return
        saveAll(list.map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    @Synchronized
    fun delete(id: Long) {
        saveAll(loadAll().filter { it.id != id })
    }

    /** Clears the song reference from every alarm using [songId] (song removed from the library). */
    @Synchronized
    fun clearSong(songId: String) {
        val list = loadAll()
        if (list.none { it.songId == songId }) return
        saveAll(list.map { if (it.songId == songId) it.copy(songId = null) else it })
    }

    private fun saveAll(alarms: List<AlarmItem>) {
        prefs.edit().putString(KEY_ALARMS, encode(alarms)).apply()
    }

    private fun migrateIfNeeded() {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return

        val legacy = AriaAlarmPreferences(appContext)
        if (legacy.hasAlarm) {
            val alarm = AlarmItem(
                id = 1L,
                name = "Alarm",
                hour = legacy.hour,
                minute = legacy.minute,
                enabled = legacy.enabled,
                songId = null
            )
            prefs.edit()
                .putLong(KEY_NEXT_ID, 2L)
                .putString(KEY_ALARMS, encode(listOf(alarm)))
                .apply()
        }

        // The legacy single-alarm PendingIntent is replaced by per-alarm
        // scheduling; cancelling it prevents a ghost alarm from firing.
        AriaAlarmScheduler.cancelLegacy(appContext)
        prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
    }

    private fun encode(alarms: List<AlarmItem>): String {
        val array = JSONArray()
        alarms.forEach { alarm ->
            val repeatDays = JSONArray()
            alarm.repeatDays.forEach { repeatDays.put(it) }
            array.put(JSONObject().apply {
                put("id", alarm.id)
                put("name", alarm.name)
                put("hour", alarm.hour)
                put("minute", alarm.minute)
                put("enabled", alarm.enabled)
                if (alarm.songId != null) put("songId", alarm.songId)
                put("repeatDays", repeatDays)
            })
        }
        return array.toString()
    }

    private fun decode(json: String): List<AlarmItem> = try {
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val repeatDays = if (obj.has("repeatDays") && !obj.isNull("repeatDays")) {
                val days = obj.getJSONArray("repeatDays")
                (0 until days.length()).mapTo(mutableSetOf()) { days.getInt(it) }
            } else {
                // Alarms persisted before repeat days existed ring every day.
                AlarmItem.ALL_DAYS
            }
            AlarmItem(
                id = obj.getLong("id"),
                name = obj.optString("name", obj.optString("label", "Alarm")).ifBlank { "Alarm" },
                hour = obj.getInt("hour"),
                minute = obj.getInt("minute"),
                enabled = obj.getBoolean("enabled"),
                songId = if (obj.has("songId") && !obj.isNull("songId")) obj.getString("songId") else null,
                repeatDays = repeatDays
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    companion object {
        private const val PREFS_NAME = "aria_alarm_multi"
        private const val KEY_ALARMS = "alarms_json"
        private const val KEY_NEXT_ID = "next_id"
        private const val KEY_MIGRATED = "migrated_v2"
    }
}
