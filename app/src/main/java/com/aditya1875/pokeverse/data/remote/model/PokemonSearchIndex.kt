package com.aditya1875.pokeverse.data.remote.model

data class PokemonSearchIndex(
    val id: Int,
    val name: String,
    val normalizedName: String,
    val types: List<String>,
    val genera: String?,        // "Seed Pokémon"
    val habitat: String?,       // forest, cave, sea
    val color: String?,         // red, blue, green
    val shape: String?,         // quadruped, wings
    val keywords: Set<String>,  // generated
    val isLegendary: Boolean,
    val isMythical: Boolean
)
