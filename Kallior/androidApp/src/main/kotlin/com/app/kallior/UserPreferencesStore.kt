package com.app.kallior

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kallos.engine.RadarChartEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_preferences")

private val ACTIVITY_LEVEL_KEY = stringPreferencesKey("activity_level")
private val SCREEN_USE_KEY = stringPreferencesKey("screen_use")

/**
 * DataStore-backed persistence for the user's Activity Level and Screen Use
 * preferences. On read the corresponding [RadarChartEngine] fields are updated
 * so the scoring engine always reflects the chosen targets.
 */
class UserPreferencesStore(context: Context) {

    private val dataStore = context.applicationContext.userPrefsDataStore

    val activityLevelFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[ACTIVITY_LEVEL_KEY] ?: "Casual"
    }

    val screenUseFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[SCREEN_USE_KEY] ?: "1-2 hours"
    }

    suspend fun currentActivityLevel(): String = activityLevelFlow.first()
    suspend fun currentScreenUse(): String = screenUseFlow.first()

    suspend fun setActivityLevel(level: String) {
        dataStore.edit { it[ACTIVITY_LEVEL_KEY] = level }
        applyActivityLevel(level)
    }

    suspend fun setScreenUse(use: String) {
        dataStore.edit { it[SCREEN_USE_KEY] = use }
        applyScreenUse(use)
    }

    /** Call once at app start to restore persisted preferences into the engine. */
    suspend fun restoreIntoEngine() {
        applyActivityLevel(currentActivityLevel())
        applyScreenUse(currentScreenUse())
    }

    companion object {
        fun applyActivityLevel(level: String) {
            RadarChartEngine.stepTarget = when (level) {
                "Sedentary" -> 5000.0
                "Casual" -> 8000.0
                "Highly Active" -> 10000.0
                else -> 8000.0
            }
        }

        fun applyScreenUse(use: String) {
            RadarChartEngine.entertainmentAllowanceMinutes = when (use) {
                "1-2 hours" -> 45.0
                "3-4 hours" -> 60.0
                "5-6 hours" -> 75.0
                "6+ hours" -> 90.0
                else -> 45.0
            }
        }
    }
}
