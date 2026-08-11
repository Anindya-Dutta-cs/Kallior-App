package org.example.project

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.timeWastingDataStore: DataStore<Preferences> by preferencesDataStore(name = "time_wasting_prefs")

class TimeWastingAppsRepository @Inject constructor(private val context: Context) {

    private val TIME_WASTING_APPS_KEY = stringSetPreferencesKey("time_wasting_apps")

    val timeWastingAppsFlow: Flow<Set<String>> = context.timeWastingDataStore.data.map { preferences ->
        preferences[TIME_WASTING_APPS_KEY] ?: emptySet()
    }

    suspend fun addTimeWastingApp(packageName: String) {
        context.timeWastingDataStore.edit { preferences ->
            val current = preferences[TIME_WASTING_APPS_KEY] ?: emptySet()
            if (!current.contains(packageName)) {
                preferences[TIME_WASTING_APPS_KEY] = current + packageName
            }
        }
    }

    suspend fun removeTimeWastingApp(packageName: String) {
        context.timeWastingDataStore.edit { preferences ->
            val current = preferences[TIME_WASTING_APPS_KEY] ?: emptySet()
            if (current.contains(packageName)) {
                preferences[TIME_WASTING_APPS_KEY] = current - packageName
            }
        }
    }
}
