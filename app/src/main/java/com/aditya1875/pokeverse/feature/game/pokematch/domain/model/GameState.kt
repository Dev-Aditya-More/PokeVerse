package com.aditya1875.pokeverse.feature.game.pokematch.domain.model

sealed class GameState {
    object Idle : GameState()
    object Loading : GameState()
    data class Playing(
        val cards: List<CardState>,
        val flippedIndices: List<Int> = emptyList(),
        val matchedPairs: Set<Int> = emptySet(),
        val moves: Int = 0,
        val timeRemaining: Int,
        val score: Int = 0,
        val difficulty: Difficulty
    ) : GameState()
    data class Paused(val playing: Playing) : GameState()
    data class Victory(
        val score: Int,
        val moves: Int,
        val timeTaken: Int,
        val stars: Int,
        val difficulty: Difficulty,
        val isNewBest: Boolean
    ) : GameState()
    data class TimeUp(
        val matchesFound: Int,
        val totalPairs: Int,
        val difficulty: Difficulty
    ) : GameState()
}