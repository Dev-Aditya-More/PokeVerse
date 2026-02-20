package com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components

data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val difficulty: QuizDifficulty,
    val category: QuizCategory,
    val explanation: String
)

enum class QuizDifficulty(
    val displayName: String,
    val timePerQuestion: Int,
    val questionCount: Int
) {
    EASY("Easy", 30, 10),
    MEDIUM("Medium", 25, 10),
    HARD("Hard", 20, 10)
}

enum class QuizCategory(val displayName: String) {
    TYPES("Type Knowledge"),
    EVOLUTIONS("Evolutions"),
    ABILITIES("Abilities"),
    REGIONS("Regions"),
    LEGENDARIES("Legendary Pokémon"),
    MOVES("Moves & Attacks"),
    POKEDEX("Pokédex Facts"),
    STATS("Stats & Numbers")
}

data class QuizGameState(
    val questions: List<QuizQuestion>,
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val timeRemaining: Int,
    val totalTimePerQuestion: Int,
    val difficulty: QuizDifficulty,
    val answers: MutableList<Int?> = MutableList(10) { null }
)

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

fun calculateQuestionScore(
    isCorrect: Boolean,
    timeRemaining: Int,
    totalTimeForQuestion: Int
): Int {
    if (!isCorrect) return 0

    val baseScore = 50
    val timeBonus = (timeRemaining.toFloat() / totalTimeForQuestion * 50).toInt()
    return baseScore + timeBonus
}

fun calculateStars(score: Int, totalQuestions: Int): Int {
    val maxScore = totalQuestions * 100
    val percentage = (score.toFloat() / maxScore)
    return when {
        percentage >= 0.9f -> 3
        percentage >= 0.7f -> 2
        else -> 1
    }
}