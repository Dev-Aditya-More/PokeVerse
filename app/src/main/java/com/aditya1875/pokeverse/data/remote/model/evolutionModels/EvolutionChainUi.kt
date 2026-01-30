package com.aditya1875.pokeverse.data.remote.model.evolutionModels

data class EvolutionChainUi(
    val previous: EvolutionNode?,
    val current: EvolutionNode,
    val next: EvolutionNode?
)
