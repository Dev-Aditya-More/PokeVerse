package com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model

import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonResult

data class TypeResponse(
    val id: Int,
    val name: String,
    val pokemon: List<TypePokemonEntry>
)

data class TypePokemonEntry(
    val pokemon: PokemonResult,
    val slot: Int
)