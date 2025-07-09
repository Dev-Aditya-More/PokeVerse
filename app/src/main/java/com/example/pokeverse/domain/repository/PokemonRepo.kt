package com.example.pokeverse.domain.repository

import com.example.pokeverse.data.remote.model.PokemonListResponse
import com.example.pokeverse.data.remote.model.PokemonResponse
import com.example.pokeverse.data.remote.model.PokemonSpeciesResponse

interface PokemonRepo {
    suspend fun getPokemonList(limit: Int, offset: Int): PokemonListResponse
    suspend fun getPokemonByName(name: String): PokemonResponse
    suspend fun getPokemonSpeciesByName(name: String): PokemonSpeciesResponse
}
