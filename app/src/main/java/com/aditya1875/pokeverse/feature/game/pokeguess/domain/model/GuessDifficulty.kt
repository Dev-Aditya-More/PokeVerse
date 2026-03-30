package com.aditya1875.pokeverse.feature.game.pokeguess.domain.model

enum class GuessDifficulty(
    val displayName: String,
    val questionsPerGame: Int,
    val timePerQuestion: Int,
    val optionCount: Int
) {
    EASY("Easy", 10, 20, 4),
    MEDIUM("Medium", 10, 25, 4),
    HARD("Hard", 10, 30, 4)
}