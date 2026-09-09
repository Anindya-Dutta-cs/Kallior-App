package org.example.project.alarm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Manages multiple alarms, persisted as a JSON array in SharedPreferences.
 *
 * The legacy single-alarm [AriaAlarmPreferences] is left untouched so that
 * [AriaAlarmReceiver], [AriaBootReceiver], and [AriaAlarmScheduler] continue
 * to compile. The new screen uses this store instead.
 */
class AriaAlarmStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Read all alarms, sorted by creation order. */
    fun loadAll(): List<AlarmItem> {
        val json = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                AlarmItem(
                    id = obj.getString("id"),
                    hour = obj.getInt("hour"),
                    minute = obj.getInt("minute"),
                    enabled = obj.getBoolean("enabled"),
                    label = obj.optString("label", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Persist the full list. */
    fun saveAll(alarms: List<AlarmItem>) {
        val array = JSONArray()
        alarms.forEach { a ->
            array.put(JSONObject().apply {
                put("id", a.id)
                put("hour", a.hour)
                put("minute", a.minute)
                put("enabled", a.enabled)
                put("label", a.label)
            })
        }
        prefs.edit().putString(KEY_ALARMS, array.toString()).apply()

        // Mirror the "next enabled" alarm to the legacy prefs so the
        // receiver/boot logic still works without modification.
        syncLegacyPrefs()
    }

    /** Push changes to the single-alarm [AriaAlarmPreferences] keys. */
    private fun syncLegacyPrefs() {
        val legacyPrefs = appContext.getSharedPreferences("aria_alarm", Context.MODE_PRIVATE)
        val alarms = loadAll()
        val nextEnabled = alarms.firstOrNull { it.enabled }
        legacyPrefs.edit()
            .putBoolean("aria_alarm_enabled", nextEnabled != null)
            .putInt("aria_alarm_hour", nextEnabled?.hour ?: 7)
            .putInt("aria_alarm_minute", nextEnabled?.minute ?: 0)
            .apply()
    }

    /** Convenience to update a single alarm in-place. */
    fun update(alarm: AlarmItem) {
        val list = loadAll().toMutableList()
        val idx = list.indexOfFirst { it.id == alarm.id }
        if (idx >= 0) {
            list[idx] = alarm
        } else {
            list.add(alarm)
        }
        saveAll(list)
    }

    fun delete(id: String) {
        saveAll(loadAll().filter { it.id != id })
    }

    companion object {
        private const val PREFS_NAME = "aria_alarm_multi"
        private const val KEY_ALARMS = "alarms_json"
    }
}

/**
 * Immutable data-class for a single alarm.
 * [id] is a stable UUID used for pager keys and request-code derivation.
 */
data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int = 7,
    val minute: Int = 0,
    val enabled: Boolean = false,
    val label: String = ""
) {
    /** Derive a stable PendingIntent request code from the ID. */
    val requestCode: Int get() = id.hashCode() and 0x7FFFFFFF
}
