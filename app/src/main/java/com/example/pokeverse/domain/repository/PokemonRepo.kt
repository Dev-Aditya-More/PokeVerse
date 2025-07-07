package com.example.pokeverse.domain.repository

import com.example.pokeverse.data.remote.model.PokemonListResponse

interface PokemonRepo {
    suspend fun getPokemonList(limit: Int, offset: Int): PokemonListResponse
}