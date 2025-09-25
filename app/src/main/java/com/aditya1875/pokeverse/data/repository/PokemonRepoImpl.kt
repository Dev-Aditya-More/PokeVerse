package com.aditya1875.pokeverse.data.repository

import com.aditya1875.pokeverse.data.remote.PokeApi
import com.aditya1875.pokeverse.data.remote.model.PokemonListResponse
import com.aditya1875.pokeverse.data.remote.model.PokemonResponse
import com.aditya1875.pokeverse.data.remote.model.PokemonSpeciesResponse
import com.aditya1875.pokeverse.data.remote.model.evolutionModels.EvolutionChainResponse
import com.aditya1875.pokeverse.domain.repository.PokemonRepo

class PokemonRepoImpl(
    private val api: PokeApi
) : PokemonRepo {
    override suspend fun getPokemonList(limit: Int, offset: Int): PokemonListResponse {
        return api.getPokemonList(limit, offset)
    }

    override suspend fun getPokemonByName(name: String): PokemonResponse {
        return api.getPokemonByName(name)
    }

    override suspend fun getPokemonSpeciesByName(name: String): PokemonSpeciesResponse {
        return api.getPokemonSpeciesByName(name)
    }

    override suspend fun getEvolutionChain(id: Int): EvolutionChainResponse {
        return api.getEvolutionChain(id)
    }
}
