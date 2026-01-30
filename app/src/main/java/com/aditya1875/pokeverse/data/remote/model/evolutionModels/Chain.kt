package com.aditya1875.pokeverse.data.remote.model.evolutionModels

data class Chain(
    val evolution_details: Any,
    val evolves_to: List<EvolvesTo>,
    val is_baby: Boolean,
    val species: EvolutionNode
)