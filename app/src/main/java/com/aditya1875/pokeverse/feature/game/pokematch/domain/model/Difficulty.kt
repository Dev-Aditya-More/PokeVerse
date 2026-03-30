package com.aditya1875.pokeverse.feature.game.pokematch.domain.model

enum class Difficulty(
    val displayName: String,
    val gridColumns: Int,
    val gridRows: Int,
    val timeSeconds: Int,
    val pairs: Int
) {
    EASY("Easy", 2, 4, 60, 4),
    MEDIUM("Medium", 4, 4, 90, 8),
    HARD("Hard", 4, 6, 120, 12)
}
