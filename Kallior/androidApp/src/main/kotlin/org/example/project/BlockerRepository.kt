package org.example.project

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "blocker_prefs")

class BlockerRepository @Inject constructor(private val context: Context) {

    private val BLOCKED_APPS_KEY = stringSetPreferencesKey("blocked_apps")
    private val IS_BLOCKING_ENABLED_KEY = booleanPreferencesKey("is_blocking_enabled")
    // packageName -> epoch millis until which the app is temporarily allowed (JSON-encoded)
    private val ALLOW_UNTIL_KEY = stringPreferencesKey("allow_until_json")
    private val json = Json

    val blockedAppsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[BLOCKED_APPS_KEY] ?: emptySet()
    }

    val allowUntilFlow: Flow<Map<String, Long>> = context.dataStore.data.map { preferences ->
        preferences[ALLOW_UNTIL_KEY]?.let { decodeAllowUntil(it) } ?: emptyMap()
    }

    val isBlockingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_BLOCKING_ENABLED_KEY] ?: false
    }

    suspend fun addBlockedApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[BLOCKED_APPS_KEY] ?: emptySet()
            if (!current.contains(packageName)) {
                preferences[BLOCKED_APPS_KEY] = current + packageName
            }
        }
    }

    suspend fun removeBlockedApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[BLOCKED_APPS_KEY] ?: emptySet()
            if (current.contains(packageName)) {
                preferences[BLOCKED_APPS_KEY] = current - packageName
            }
        }
    }

    suspend fun setBlockingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_BLOCKING_ENABLED_KEY] = enabled
        }
    }

    suspend fun setAllowUntil(packageName: String, timestamp: Long) {
        context.dataStore.edit { preferences ->
            val current = preferences[ALLOW_UNTIL_KEY]?.let { decodeAllowUntil(it) } ?: emptyMap()
            preferences[ALLOW_UNTIL_KEY] = json.encodeToString(current + (packageName to timestamp))
        }
    }

    suspend fun clearAllowUntil(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[ALLOW_UNTIL_KEY]?.let { decodeAllowUntil(it) } ?: emptyMap()
            if (current.containsKey(packageName)) {
                preferences[ALLOW_UNTIL_KEY] = json.encodeToString(current - packageName)
            }
        }
    }

    /** True when the app has an active temporary unblock (now < allowUntil). */
    suspend fun isAppAllowed(packageName: String): Boolean {
        val timestamp = context.dataStore.data.first()[ALLOW_UNTIL_KEY]
            ?.let { decodeAllowUntil(it) }
            ?.get(packageName)
        return timestamp != null && timestamp > System.currentTimeMillis()
    }

    private fun decodeAllowUntil(jsonString: String): Map<String, Long> =
        runCatching { json.decodeFromString<Map<String, Long>>(jsonString) }.getOrDefault(emptyMap())
}
