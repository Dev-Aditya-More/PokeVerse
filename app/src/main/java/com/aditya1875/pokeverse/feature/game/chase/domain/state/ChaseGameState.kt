package com.aditya1875.pokeverse.feature.game.chase.domain.state

/**
 * The screen-level phase of a run. Deliberately does NOT carry the per-frame
 * [com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseWorld] — that
 * lives in its own flow so a phase change, not every frame, recomposes the screen.
 */
sealed interface ChaseGameState {
    data object Idle : ChaseGameState
    data object Loading : ChaseGameState
    /** Sprites loaded and the track is drawn; waiting for the first tap. */
    data class Ready(val bestScore: Int) : ChaseGameState
    data class Playing(val isPaused: Boolean = false) : ChaseGameState
    data class Caught(val canRevive: Boolean) : ChaseGameState
    data class Finished(
        val score: Int,
        val meters: Int,
        val berries: Int,
        val isNewBest: Boolean
    ) : ChaseGameState {
        val stars: Int get() = when {
            meters >= 1500 -> 3
            meters >= 600 -> 2
            else -> 1
        }
    }
}
