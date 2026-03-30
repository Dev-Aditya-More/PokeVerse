package com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model

data class PokemonListResponse(
    val results: List<PokemonResult>
)

data class PokemonResult(
    val name: String,
    val url: String = ""
)