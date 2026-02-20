package com.aditya1875.pokeverse.presentation.screens.game.pokeguess.components

data class PokeGuessQuestion(
    val pokemonId: Int,
    val pokemonName: String,
    val spriteUrl: String,
    val options: List<String>,
    val correctIndex: Int,
    val generation: Int,
    val types: List<String>
)

enum class GuessDifficulty(
    val displayName: String,
    val questionsPerGame: Int,
    val timePerQuestion: Int,
    val optionCount: Int
) {
    EASY("Easy", 10, 20, 3),      // 3 options, 20s
    MEDIUM("Medium", 10, 15, 4),  // 4 options, 15s
    HARD("Hard", 10, 10, 4)       // 4 options, 10s, harder Pokemon
}

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

data class GuessScore(
    val correct: Int,
    val total: Int,
    val timeBonus: Int,
    val finalScore: Int
)