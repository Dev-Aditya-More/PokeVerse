package com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels

data class EvolvesTo(
    val evolution_details: List<EvolutionDetail>,
    val evolves_to: List<EvolvesTo>,
    val is_baby: Boolean,
    val species: EvolutionNode
)