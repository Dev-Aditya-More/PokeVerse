package com.example.pokeverse.data.remote.model.evolutionModels

import com.google.gson.annotations.SerializedName

data class EvolutionChainResponse(
    val id: Int,
    val chain: Chain
)

data class Chain(
    val species: Species,
    @SerializedName("evolves_to")
    val evolvesTo: List<Chain>,
    @SerializedName("evolves_from")
    val evolvesFrom: List<Chain>
)

data class Species(
    val name: String,
    val url: String
)
