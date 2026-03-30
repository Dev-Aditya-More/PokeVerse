package com.aditya1875.pokeverse.feature.game.pokequiz.domain.usecase

fun calculateQuestionScore(
    isCorrect: Boolean,
    timeRemaining: Int,
    totalTimeForQuestion: Int
): Int {
    if (!isCorrect) return 0

    val baseScore = 50
    val timeBonus = (timeRemaining.toFloat() / totalTimeForQuestion * 50).toInt()
    return baseScore + timeBonus
}