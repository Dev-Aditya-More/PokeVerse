package com.aditya1875.pokeverse.feature.game.survivor.domain.model

/**
 * Controls the teach -> tempt -> test ramp: early streaks stay simple and
 * generous so the loop reads as fun immediately, then both modifier odds and
 * the timer tighten as the streak grows.
 */
data class DifficultyTier(
    val weatherChance: Float,
    val optionCount: Int,
    val timeBudgetMs: Int
) {
    companion object {
        fun forStreak(streak: Int): DifficultyTier = when {
            streak < 6 -> DifficultyTier(weatherChance = 0f, optionCount = 3, timeBudgetMs = 3500)
            streak < 15 -> DifficultyTier(weatherChance = 0.35f, optionCount = 4, timeBudgetMs = 2800)
            streak < 30 -> DifficultyTier(weatherChance = 0.55f, optionCount = 4, timeBudgetMs = 2300)
            else -> DifficultyTier(weatherChance = 0.7f, optionCount = 5, timeBudgetMs = 1900)
        }
    }
}
