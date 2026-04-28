package com.aditya1875.pokeverse.feature.game.pokeduel.domain.model

sealed class DuelGameState {
    object Idle : DuelGameState()
    object Loading : DuelGameState()

    data class Dueling(
        val left: DuelPokemon,
        val right: DuelPokemon,
        val round: Int,
        val score: Int,
        val streak: Int,
        val lives: Int = 3,
        val result: DuelResult? = null,       // null = not answered yet
        val userChoice: DuelOutcome? = null,  // null = not answered yet
        val isCorrect: Boolean? = null
    ) : DuelGameState()

    data class GameOver(
        val score: Int,
        val round: Int,
        val isNewBest: Boolean
    ) : DuelGameState()
}