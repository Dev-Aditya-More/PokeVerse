package com.aditya1875.pokeverse.feature.game.poketype.domain.model

enum class TypeRushDifficulty(
    val label: String,
    val rounds: Int,
    val timePerRound: Int,    // seconds
    val optionCount: Int,     // how many type bubbles shown (correct + wrong)
    val showName: Boolean,    // false = silhouette mode for hard
) {
    EASY("Easy", 8, 12, 9, true),
    MEDIUM("Medium", 10, 9, 12, true),
    HARD("Hard", 12, 7, 14, false),  // silhouette + premium
}
