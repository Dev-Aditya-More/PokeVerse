package com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonSpeciesResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.TypeResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.EvolutionChainResponse

interface PokemonDetailRepo{
    suspend fun getPokemonByName(name: String): PokemonResponse
    suspend fun getPokemonSpeciesByName(name: String): PokemonSpeciesResponse

    suspend fun getEvolutionChain(id: Int): EvolutionChainResponse
}