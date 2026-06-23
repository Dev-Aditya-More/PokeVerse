package com.aditya1875.pokeverse.feature.game.wildcatch.domain.model

enum class ThrowAccuracy(
    val label: String,
    val catchChance: Float,
    val scoreBonus: Int
) {
    PERFECT("Perfect!", 1.00f, 100),
    GREAT("Great!", 0.85f, 60),
    NICE("Nice!", 0.60f, 30),
    MISS("Missed...", 0.25f, 0)
}
