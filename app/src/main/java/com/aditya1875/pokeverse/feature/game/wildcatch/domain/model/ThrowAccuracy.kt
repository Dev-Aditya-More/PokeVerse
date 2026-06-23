package com.aditya1875.pokeverse.feature.game.wildcatch.domain.model

enum class ThrowAccuracy(
    val label: String,
    val catchChance: Float,
    val scoreBonus: Int
) {
    PERFECT("Perfect!", 0.97f, 100),
    GREAT("Great!", 0.72f, 60),
    NICE("Nice!", 0.42f, 30),
    MISS("Missed...", 0.08f, 0)
}
