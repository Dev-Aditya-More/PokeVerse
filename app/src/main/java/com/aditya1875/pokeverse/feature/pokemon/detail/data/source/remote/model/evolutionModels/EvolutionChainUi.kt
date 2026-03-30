package com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels

data class EvolutionChainUi(
    val previous: EvolutionNode?,
    val current: EvolutionNode,
    val next: EvolutionNode?
)
