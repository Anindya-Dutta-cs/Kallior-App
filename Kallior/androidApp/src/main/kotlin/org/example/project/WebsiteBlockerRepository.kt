package org.example.project

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.websiteDataStore: DataStore<Preferences> by preferencesDataStore(name = "website_blocker_prefs")

class WebsiteBlockerRepository @Inject constructor(private val context: Context) {

    private val BLOCKED_WEBSITES_KEY = stringPreferencesKey("blocked_websites")
    private val VPN_ENABLED_KEY = booleanPreferencesKey("is_vpn_enabled")
    private val ALWAYS_ON_NUDGE_SHOWN_KEY = booleanPreferencesKey("always_on_nudge_shown")
    private val ALLOW_UNTIL_KEY = stringPreferencesKey("website_allow_until")

    val blockedWebsitesFlow: Flow<List<String>> = context.websiteDataStore.data.map { preferences ->
        val jsonString = preferences[BLOCKED_WEBSITES_KEY] ?: "[]"
        try {
            Json.decodeFromString<List<String>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val allowUntilFlow: Flow<Map<String, Long>> = context.websiteDataStore.data.map { preferences ->
        decodeAllowUntil(preferences[ALLOW_UNTIL_KEY] ?: "{}")
    }

    val isVpnEnabledFlow: Flow<Boolean> = context.websiteDataStore.data.map { preferences ->
        preferences[VPN_ENABLED_KEY] ?: false
    }

    val alwaysOnNudgeShownFlow: Flow<Boolean> = context.websiteDataStore.data.map { preferences ->
        preferences[ALWAYS_ON_NUDGE_SHOWN_KEY] ?: false
    }

    suspend fun addBlockedWebsite(url: String) {
        context.websiteDataStore.edit { preferences ->
            val currentJson = preferences[BLOCKED_WEBSITES_KEY] ?: "[]"
            val currentList = try {
                Json.decodeFromString<List<String>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }
            if (!currentList.contains(url)) {
                val newList = currentList + url
                preferences[BLOCKED_WEBSITES_KEY] = Json.encodeToString(newList)
            }
        }
    }

    suspend fun removeBlockedWebsite(url: String) {
        context.websiteDataStore.edit { preferences ->
            val currentJson = preferences[BLOCKED_WEBSITES_KEY] ?: "[]"
            val currentList = try {
                Json.decodeFromString<List<String>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }
            if (currentList.contains(url)) {
                val newList = currentList - url
                preferences[BLOCKED_WEBSITES_KEY] = Json.encodeToString(newList)
            }
        }
    }

    suspend fun setAllowUntil(domain: String, expiry: Long) {
        context.websiteDataStore.edit { preferences ->
            val currentMap = decodeAllowUntil(preferences[ALLOW_UNTIL_KEY] ?: "{}")
            val newMap = currentMap + (domain.lowercase() to expiry)
            preferences[ALLOW_UNTIL_KEY] = Json.encodeToString(newMap)
        }
    }

    suspend fun clearAllowUntil(domain: String) {
        context.websiteDataStore.edit { preferences ->
            val currentMap = decodeAllowUntil(preferences[ALLOW_UNTIL_KEY] ?: "{}")
            val newMap = currentMap - domain.lowercase()
            preferences[ALLOW_UNTIL_KEY] = Json.encodeToString(newMap)
        }
    }

    private fun decodeAllowUntil(json: String): Map<String, Long> {
        return try {
            Json.decodeFromString(json)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun setVpnEnabled(enabled: Boolean) {
        context.websiteDataStore.edit { preferences ->
            preferences[VPN_ENABLED_KEY] = enabled
        }
    }

    /**
     * Atomically marks the always-on VPN reminder as consumed and reports whether
     * this caller is the first one to do so.
     */
    suspend fun claimAlwaysOnNudge(): Boolean {
        var claimed = false
        context.websiteDataStore.edit { preferences ->
            if (preferences[ALWAYS_ON_NUDGE_SHOWN_KEY] != true) {
                preferences[ALWAYS_ON_NUDGE_SHOWN_KEY] = true
                claimed = true
            }
        }
        return claimed
    }

    suspend fun setAlwaysOnNudgeShown() {
        context.websiteDataStore.edit { preferences ->
            preferences[ALWAYS_ON_NUDGE_SHOWN_KEY] = true
        }
    }
}
