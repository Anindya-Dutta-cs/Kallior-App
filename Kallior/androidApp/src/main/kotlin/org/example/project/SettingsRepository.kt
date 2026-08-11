package org.example.project

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rateDataStore by preferencesDataStore(name = "screen_time_settings")

private val RATE_PER_SECOND = floatPreferencesKey("rate_per_second")

/** Persists the user's "money wasted per second" rate so it survives app restarts. */
class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.rateDataStore

    val ratePerSecondFlow: Flow<Float> = dataStore.data.map { prefs ->
        prefs[RATE_PER_SECOND] ?: 0.005f
    }

    suspend fun setRatePerSecond(rate: Float) {
        dataStore.edit { it[RATE_PER_SECOND] = rate.coerceIn(0.001f, 1.00f) }
    }
}
