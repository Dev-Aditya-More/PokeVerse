package com.aditya1875.pokeverse.feature.game.pokequiz.domain.model

data class QuizGameState(
    val questions: List<QuizQuestion>,
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val timeRemaining: Int,
    val totalTimePerQuestion: Int,
    val difficulty: QuizDifficulty,
    val answers: List<Int?> = List(10) { null }
)
