package com.aditya1875.pokeverse.feature.game.pokequiz.domain.model

enum class QuizDifficulty(
    val displayName: String,
    val timePerQuestion: Int,
    val questionCount: Int
) {
    EASY("Easy", 30, 10),
    MEDIUM("Medium", 25, 10),
    HARD("Hard", 20, 10)
}