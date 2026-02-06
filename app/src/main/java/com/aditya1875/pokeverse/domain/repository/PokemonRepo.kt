package com.aditya1875.pokeverse.domain.repository

import com.aditya1875.pokeverse.data.remote.model.PokemonListResponse
import com.aditya1875.pokeverse.data.remote.model.PokemonResponse
import com.aditya1875.pokeverse.data.remote.model.PokemonSpeciesResponse
import com.aditya1875.pokeverse.data.remote.model.TypeResponse
import com.aditya1875.pokeverse.data.remote.model.evolutionModels.EvolutionChainResponse

interface PokemonRepo {
    suspend fun getPokemonList(limit: Int, offset: Int): PokemonListResponse
    suspend fun getPokemonByName(name: String): PokemonResponse
    suspend fun getPokemonSpeciesByName(name: String): PokemonSpeciesResponse

    suspend fun getEvolutionChain(id: Int): EvolutionChainResponse

    // Add this method
    suspend fun getPokemonByType(type: String): TypeResponse

}
