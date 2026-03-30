package com.aditya1875.pokeverse.feature.game.pokematch.domain.model

data class CardState(
    val index: Int,
    val pokemonName: String,
    val spriteUrl: String,
    val pairId: Int,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)
