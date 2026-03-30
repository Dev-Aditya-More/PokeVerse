package com.aditya1875.pokeverse.feature.pokemon.home.domain.model

data class DailyTriviaState(
    val pokemonId: Int,
    val pokemonName: String,
    val spriteUrl: String,
    val types: List<String>,
    val height: Int,
    val weight: Int,
    val baseStats: Map<String, Int>,
    val generation: Int,
    val date: String,
    val isAnswered: Boolean = false,
    val wasCorrect: Boolean = false,
    val options: List<String> = emptyList(),
)
