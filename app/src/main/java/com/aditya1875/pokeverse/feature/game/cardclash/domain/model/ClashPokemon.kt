package com.aditya1875.pokeverse.feature.game.cardclash.domain.model

data class ClashPokemon(
    val id: Int,
    val name: String,
    val types: List<String>,
    val bst: Int,
    val spriteUrl: String
)
