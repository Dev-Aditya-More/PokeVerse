package com.aditya1875.pokeverse.feature.game.pokeguess.domain.state

import com.aditya1875.pokeverse.feature.game.pokeguess.domain.model.GuessDifficulty
import com.aditya1875.pokeverse.feature.game.pokeguess.domain.model.PokeGuessQuestion

sealed class GuessGameState {
    object Idle : GuessGameState()
    object Loading : GuessGameState()

    data class ShowingSilhouette(
        val question: PokeGuessQuestion,
        val currentQuestionIndex: Int,
        val totalQuestions: Int,
        val score: Int,
        val timeRemaining: Int
    ) : GuessGameState()

    data class Revealing(
        val question: PokeGuessQuestion,
        val selectedAnswer: String,
        val isCorrect: Boolean,
        val isTimeUp: Boolean,
        val currentQuestionIndex: Int,
        val totalQuestions: Int,
        val score: Int
    ) : GuessGameState()

    data class Finished(
        val score: Int,
        val correctAnswers: Int,
        val totalQuestions: Int,
        val difficulty: GuessDifficulty
    ) : GuessGameState()
}
