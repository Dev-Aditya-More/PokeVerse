package com.example.pokeverse.data.repository

import com.example.pokeverse.data.remote.PokeApi
import com.example.pokeverse.data.remote.model.PokemonListResponse
import com.example.pokeverse.data.remote.model.PokemonResponse
import com.example.pokeverse.data.remote.model.PokemonSpeciesResponse
import com.example.pokeverse.domain.repository.PokemonRepo

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
}
