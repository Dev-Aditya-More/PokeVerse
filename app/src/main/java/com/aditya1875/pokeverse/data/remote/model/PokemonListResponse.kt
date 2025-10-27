package com.aditya1875.pokeverse.data.remote.model

data class PokemonListResponse(
    val results: List<PokemonResult>
)

data class PokemonResult(
    val name: String,
    val url: String = ""
)