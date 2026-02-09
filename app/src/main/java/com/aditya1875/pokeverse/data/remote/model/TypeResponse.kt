package com.aditya1875.pokeverse.data.remote.model

data class TypeResponse(
    val id: Int,
    val name: String,
    val pokemon: List<TypePokemonEntry>
)

data class TypePokemonEntry(
    val pokemon: PokemonResult,
    val slot: Int
)