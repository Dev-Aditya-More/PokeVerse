package com.example.pokeverse.data.remote.model

import com.google.gson.annotations.SerializedName

data class PokemonSpeciesResponse(
    @SerializedName("flavor_text_entries")
    val flavorTextEntries: List<FlavorTextEntry>,

    @SerializedName("varieties")
    val varieties: List<PokemonVariety> = emptyList()
)

data class FlavorTextEntry(
    @SerializedName("flavor_text") val flavorText: String,
    val language: Language
)

data class Language(
    val name: String
)

data class PokemonVariety(
    @SerializedName("is_default") val isDefault: Boolean,
    val pokemon: VarietyPokemon
)

data class VarietyPokemon(
    val name: String,
    val url: String
)
