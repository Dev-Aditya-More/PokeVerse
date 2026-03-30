package com.aditya1875.pokeverse.feature.game.pokequiz.domain.model

sealed class QuizUiState {
    object Idle : QuizUiState()
    object Loading : QuizUiState()
    data class Playing(val gameState: QuizGameState) : QuizUiState()
    data class ShowingAnswer(
        val gameState: QuizGameState,
        val selectedAnswerIndex: Int,
        val isCorrect: Boolean,
        val explanation: String
    ) : QuizUiState()
    data class Finished(
        val score: Int,
        val correctAnswers: Int,
        val totalQuestions: Int,
        val difficulty: QuizDifficulty,
        val stars: Int
    ) : QuizUiState()
}