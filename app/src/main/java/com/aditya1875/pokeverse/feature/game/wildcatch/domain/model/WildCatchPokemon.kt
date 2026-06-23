package com.aditya1875.pokeverse.feature.game.wildcatch.domain.model

data class WildCatchPokemon(
    val id: Int,
    val name: String,
    val spriteUrl: String,
    val types: List<String>
)
