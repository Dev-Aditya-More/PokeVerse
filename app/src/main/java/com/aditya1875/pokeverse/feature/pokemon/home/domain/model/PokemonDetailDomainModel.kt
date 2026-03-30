package com.aditya1875.pokeverse.feature.pokemon.home.domain.model

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonVariety

data class PokemonDetailDomainModel(
    val pokemon: PokemonResponse,
    val description: String,
    val varieties: List<PokemonVariety>,
    val evolutionChainUrl: String?
)