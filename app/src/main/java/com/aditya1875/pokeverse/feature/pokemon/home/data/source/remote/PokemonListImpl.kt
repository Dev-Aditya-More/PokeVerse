package com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.TypeResponse
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonListResponse
import com.aditya1875.pokeverse.feature.pokemon.home.domain.repository.PokemonListRepo

class PokemonListImpl(
    private val api: PokemonListApi
) : PokemonListRepo {
    override suspend fun getPokemonList(limit: Int, offset: Int): PokemonListResponse {
        return api.getPokemonList(limit, offset)
    }

    override suspend fun getPokemonByType(type: String): TypeResponse {
        return api.getPokemonByType(type)
    }
}