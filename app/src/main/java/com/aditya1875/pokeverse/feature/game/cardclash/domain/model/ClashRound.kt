package com.aditya1875.pokeverse.feature.game.cardclash.domain.model

data class ClashRound(
    val roundNumber: Int,
    val myCard: ClashPokemon,
    val opponentCard: ClashPokemon,
    val myScore: Float,
    val opponentScore: Float,
    val winner: RoundWinner
)

enum class RoundWinner { ME, OPPONENT, DRAW }
