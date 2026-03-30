package com.aditya1875.pokeverse.feature.game.poketype.domain.model

data class TypeRushQuestion(
    val pokemonId: Int,
    val pokemonName: String,
    val spriteUrl: String,
    val correctTypes: List<String>,       // 1 or 2
    val options: List<String>,            // shuffled pool incl. correct ones
)