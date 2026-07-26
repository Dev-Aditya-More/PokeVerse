package com.aditya1875.pokeverse.feature.game.pokequiz.domain.model

const val QUIZ_MAX_LIVES = 3

data class QuizGameState(
    val questions: List<QuizQuestion>,
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val questionsAnswered: Int = 0,
    val lives: Int = QUIZ_MAX_LIVES,
    val timeRemaining: Int,
    val totalTimePerQuestion: Int,
    val difficulty: QuizDifficulty,
    val combo: Int = 0,
    val bestScore: Int = 0
)
