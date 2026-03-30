package com.aditya1875.pokeverse.feature.game.poketype.domain.model

sealed class TypeRushState {
    object Idle : TypeRushState()
    object Loading : TypeRushState()
    data class Playing(
        val question: TypeRushQuestion,
        val questionIndex: Int,
        val totalQuestions: Int,
        val score: Int,
        val timeRemaining: Int,
        val selectedTypes: Set<String> = emptySet(),
        val isLocked: Boolean = false,        // true after time up or all correct tapped
    ) : TypeRushState()

    data class RoundResult(
        val result: TypeRushRoundResult,
        val questionIndex: Int,
        val totalQuestions: Int,
        val score: Int,
    ) : TypeRushState()

    data class Finished(
        val score: Int,
        val correctRounds: Int,
        val totalRounds: Int,
        val difficulty: TypeRushDifficulty,
        val results: List<TypeRushRoundResult>,
    ) : TypeRushState()
}