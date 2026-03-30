package com.aditya1875.pokeverse.feature.game.pokequiz.domain.model

data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val difficulty: QuizDifficulty,
    val category: QuizCategory,
    val explanation: String
)