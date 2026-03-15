package com.aditya1875.pokeverse.presentation.screens.game.poketype.components

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

val ALL_TYPES = listOf(
    "normal", "fire", "water", "electric", "grass", "ice",
    "fighting", "poison", "ground", "flying", "psychic", "bug",
    "rock", "ghost", "dragon", "dark", "steel", "fairy"
)

data class TypeRushQuestion(
    val pokemonId: Int,
    val pokemonName: String,
    val spriteUrl: String,
    val correctTypes: List<String>,       // 1 or 2
    val options: List<String>,            // shuffled pool incl. correct ones
)

data class TypeRushRoundResult(
    val question: TypeRushQuestion,
    val selectedTypes: Set<String>,
    val isFullyCorrect: Boolean,          // got all correct, no wrong picks
    val isPartiallyCorrect: Boolean,      // got at least one correct
    val pointsEarned: Int,
    val timeBonus: Int,
)

sealed class TypeRushState {
    object Idle : TypeRushState()
    object Loading : TypeRushState()
    data class Playing(
        val question: TypeRushQuestion,
        val questionIndex: Int,
        val totalQuestions: Int,
        val score: Int,
        val timeRemaining: Int,
        val selectedTypes: Set<String> = emptySet(),
        val isLocked: Boolean = false,        // true after time up or all correct tapped
    ) : TypeRushState()

    data class RoundResult(
        val result: TypeRushRoundResult,
        val questionIndex: Int,
        val totalQuestions: Int,
        val score: Int,
    ) : TypeRushState()

    data class Finished(
        val score: Int,
        val correctRounds: Int,
        val totalRounds: Int,
        val difficulty: TypeRushDifficulty,
        val results: List<TypeRushRoundResult>,
    ) : TypeRushState()
}