package com.aditya1875.pokeverse.feature.pokemon.home.domain.repository

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.TypeResponse
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonListResponse

interface PokemonListRepo {
    suspend fun getPokemonList(limit: Int, offset: Int): PokemonListResponse

    suspend fun getPokemonByType(type: String): TypeResponse
}