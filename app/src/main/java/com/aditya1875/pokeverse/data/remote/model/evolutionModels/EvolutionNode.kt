package com.aditya1875.pokeverse.data.remote.model.evolutionModels

import com.aditya1875.pokeverse.data.remote.model.PokemonResponse

data class EvolutionNode(
    val name: String,
    val url: String,
    val isBaby: Boolean = false,
    val evolutionDetails: List<EvolutionDetail> = emptyList(),
    val evolvesTo: List<EvolutionNode>
)

data class EvolutionDetail(
    val minLevel: Int?,
    val knownMove: String?,
    val knownMoveType: String?,
    val location: String?,
)

data class EvolutionDisplayItem(
    val name: String,
    val imageUrl: String?
)
