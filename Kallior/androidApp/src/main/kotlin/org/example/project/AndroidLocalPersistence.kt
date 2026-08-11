package org.example.project

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kallos.data.LocalPersistence
import kallos.repository.GameState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.gameDataStore by preferencesDataStore(name = "kallos_game_state")

class AndroidLocalPersistence(private val context: Context) : LocalPersistence {
    private val dataStore = context.gameDataStore
    private val key = stringPreferencesKey("game_state_json")

    override suspend fun saveGameState(state: GameState) {
        val json = Json.encodeToString(state)
        dataStore.edit { prefs ->
            prefs[key] = json
        }
    }

    override suspend fun loadGameState(): GameState? {
        val json = dataStore.data.map { it[key] }.first()
        return json?.let {
            try {
                Json.decodeFromString<GameState>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}
