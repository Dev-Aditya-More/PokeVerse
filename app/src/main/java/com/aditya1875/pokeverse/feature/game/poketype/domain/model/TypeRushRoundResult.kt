package com.aditya1875.pokeverse.feature.game.poketype.domain.model

data class TypeRushRoundResult(
    val question: TypeRushQuestion,
    val selectedTypes: Set<String>,
    val isFullyCorrect: Boolean,          // got all correct, no wrong picks
    val isPartiallyCorrect: Boolean,      // got at least one correct
    val pointsEarned: Int,
    val timeBonus: Int,
)