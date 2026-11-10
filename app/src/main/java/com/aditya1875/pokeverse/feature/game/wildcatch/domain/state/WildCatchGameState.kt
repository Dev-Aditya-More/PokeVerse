package com.aditya1875.pokeverse.feature.game.wildcatch.domain.state

import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.ThrowAccuracy
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.WildCatchPokemon

sealed class WildCatchGameState {

    object Idle : WildCatchGameState()

    object Loading : WildCatchGameState()

    data class Throwing(
        val pokemon: WildCatchPokemon,
        val cycleDurationMs: Long,
        val pokemonCount: Int,
        val catches: Int,
        val score: Int,
        val streak: Int,
        val lives: Int,
        val bestScore: Int = 0
    ) : WildCatchGameState()

    data class ShakeResult(
        val pokemon: WildCatchPokemon,
        val caught: Boolean,
        val accuracy: ThrowAccuracy,
        val lifeRecovered: Boolean,
        val pokemonCount: Int,
        val catches: Int,
        val score: Int,
        val lives: Int,
        val shakeCount: Int,
        /** First time this Pokémon is caught, so the result screen can say so. */
        val newCatch: Boolean = false,
        val bestScore: Int = 0,
        /** Points this throw earned (0 when it got away), for the "+150" pop. */
        val scoreGain: Int = 0,
        /** Catch streak after this throw. */
        val streak: Int = 0,
        /** Pokémon caught in total after this throw, for the new-entry moment. */
        val caughtTotal: Int = 0,
        /** Evo Shards this catch earned (0 when it got away). */
        val shardsGained: Int = 0,
        /** True when today's shard limit cut this catch's reward short. */
        val shardsCapped: Boolean = false,
        /** The ball that was thrown, so a catch that beat the odds (a Master Ball) is not reported as a miss. */
        val ball: com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType =
            com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType.POKE
    ) : WildCatchGameState()

    /**
     * The next wild Pokémon couldn't be loaded. The run is kept as it was: the player can retry, or end
     * the run with their score ([inGame]) instead of losing everything to a dropped connection.
     */
    data class LoadFailed(
        val kind: com.aditya1875.pokeverse.utils.LoadErrorKind,
        val inGame: Boolean
    ) : WildCatchGameState()

    data class Finished(
        val catches: Int,
        val pokemonCount: Int,
        val score: Int,
        val isNewBest: Boolean = false
    ) : WildCatchGameState()
}
