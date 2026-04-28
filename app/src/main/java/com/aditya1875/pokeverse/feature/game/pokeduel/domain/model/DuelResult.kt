package com.aditya1875.pokeverse.feature.game.pokeduel.domain.model

enum class DuelOutcome { LEFT_WINS, RIGHT_WINS, DRAW }

data class DuelResult(
    val outcome: DuelOutcome,
    val leftAdvantage: Float,   // e.g. 2.0, 0.5, 1.0
    val rightAdvantage: Float,
    val explanation: String     // e.g. "Water is super effective against Fire"
)