package com.aditya1875.pokeverse.feature.game.survivor.domain.model

data class MatchupPokemon(
    val id: Int,
    val name: String,
    val spriteUrl: String,
    val types: List<String>
)
