package kallos.engine

import kallos.domain.PlayerProfile

/**
 * Stub avatar engine. The current call sites use [onTaskCompleted] as a
 * hook after a task is marked completed so that the avatar can react.
 */
object AvatarEngine {
    /** No-op hook invoked after the player completes a task. */
    fun onTaskCompleted(player: PlayerProfile) {
        // intentionally empty — avatar reactions are not implemented yet.
    }
}
