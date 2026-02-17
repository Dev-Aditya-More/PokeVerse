package com.aditya1875.pokeverse.utils
// GameModels.kt
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

data class CardState(
    val index: Int,
    val pokemonName: String,
    val spriteUrl: String,
    val pairId: Int,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)

sealed class GameState {
    object Idle : GameState()
    object Loading : GameState()
    data class Playing(
        val cards: List<CardState>,
        val flippedIndices: List<Int> = emptyList(),
        val matchedPairs: Set<Int> = emptySet(),
        val moves: Int = 0,
        val timeRemaining: Int,
        val score: Int = 0,
        val difficulty: Difficulty
    ) : GameState()
    data class Paused(val playing: Playing) : GameState()
    data class Victory(
        val score: Int,
        val moves: Int,
        val timeTaken: Int,
        val stars: Int,
        val difficulty: Difficulty,
        val isNewBest: Boolean
    ) : GameState()
    data class TimeUp(
        val matchesFound: Int,
        val totalPairs: Int,
        val difficulty: Difficulty
    ) : GameState()
}

// Subscription state
// Update in GameModels.kt
sealed class SubscriptionState {
    object Loading : SubscriptionState()
    object Free : SubscriptionState()
    data class Premium(
        val plan: PremiumPlan = PremiumPlan.MONTHLY
    ) : SubscriptionState()
}

enum class PremiumPlan {
    MONTHLY,
    YEARLY
}