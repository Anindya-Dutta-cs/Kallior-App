package org.example.project.alarm

import android.content.Context

class AriaAlarmPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var hour: Int
        get() = prefs.getInt(KEY_HOUR, DEFAULT_HOUR)
        set(value) = prefs.edit().putInt(KEY_HOUR, value).apply()

    var minute: Int
        get() = prefs.getInt(KEY_MINUTE, DEFAULT_MINUTE)
        set(value) = prefs.edit().putInt(KEY_MINUTE, value).apply()

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    companion object {
        private const val PREFS_NAME = "aria_alarm"
        private const val KEY_HOUR = "aria_alarm_hour"
        private const val KEY_MINUTE = "aria_alarm_minute"
        private const val KEY_ENABLED = "aria_alarm_enabled"

        private const val DEFAULT_HOUR = 7
        private const val DEFAULT_MINUTE = 0
    }
}
