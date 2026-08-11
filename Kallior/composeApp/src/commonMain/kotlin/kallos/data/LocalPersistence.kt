package kallos.data

import kallos.repository.GameState

interface LocalPersistence {
    suspend fun saveGameState(state: GameState)
    suspend fun loadGameState(): GameState?
}
