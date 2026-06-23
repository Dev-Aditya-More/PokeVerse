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
        val lives: Int
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
        val shakeCount: Int
    ) : WildCatchGameState()

    data class Finished(
        val catches: Int,
        val pokemonCount: Int,
        val score: Int
    ) : WildCatchGameState()
}
