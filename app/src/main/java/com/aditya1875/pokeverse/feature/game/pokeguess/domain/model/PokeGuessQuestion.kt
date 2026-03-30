package com.aditya1875.pokeverse.feature.game.pokeguess.domain.model

data class PokeGuessQuestion(
    val pokemonId: Int,
    val pokemonName: String,
    val spriteUrl: String,
    val options: List<String>,
    val correctIndex: Int,
    val generation: Int,
    val types: List<String>
)