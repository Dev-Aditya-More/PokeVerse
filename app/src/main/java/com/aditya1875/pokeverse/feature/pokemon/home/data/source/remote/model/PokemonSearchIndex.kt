package com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model

data class PokemonSearchIndex(
    val id: Int,
    val name: String,
    val types: List<String>,
    val genera: String?,        // "Seed Pokémon"
    val keywords: Set<String>,  // generated
    val isLegendary: Boolean,
    val isMythical: Boolean
)
