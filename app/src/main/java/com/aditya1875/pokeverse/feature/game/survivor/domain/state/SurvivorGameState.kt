package com.aditya1875.pokeverse.feature.game.survivor.domain.state

import com.aditya1875.pokeverse.feature.game.survivor.domain.model.SurvivorRound

sealed class SurvivorGameState {

    object Idle : SurvivorGameState()

    object Loading : SurvivorGameState()

    data class Playing(
        val round: SurvivorRound,
        val roundsPlayed: Int,
        val lives: Int,
        val streak: Int,
        val score: Int,
        val timeRemainingMs: Int,
        val bestScore: Int = 0
    ) : SurvivorGameState()

    data class RoundResult(
        val round: SurvivorRound,
        val selectedType: String?, // null = timed out
        val wasCorrect: Boolean,
        val roundsPlayed: Int,
        val lives: Int,
        val streak: Int,
        val score: Int
    ) : SurvivorGameState()

    data class Finished(
        val score: Int,
        val bestStreak: Int,
        val roundsPlayed: Int,
        val isNewBest: Boolean = false
    ) : SurvivorGameState()
}
