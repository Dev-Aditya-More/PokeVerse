package com.aditya1875.pokeverse.feature.game.pokematch.domain.model

enum class Difficulty(
    val displayName: String,
    val gridColumns: Int,
    val gridRows: Int,
    val timeSeconds: Int,
    val pairs: Int,
    val previewSeconds: Int
) {
    EASY("Easy", 2, 4, 60, 4, previewSeconds = 3),
    MEDIUM("Medium", 4, 4, 100, 8, previewSeconds = 4),
    HARD("Hard", 4, 6, 140, 12, previewSeconds = 5)
}
