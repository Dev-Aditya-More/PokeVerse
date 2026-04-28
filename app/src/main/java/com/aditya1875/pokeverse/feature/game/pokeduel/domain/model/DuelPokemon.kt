package com.aditya1875.pokeverse.feature.game.pokeduel.domain.model

data class DuelPokemon(
    val id: Int,
    val name: String,
    val spriteUrl: String,
    val types: List<String>
)