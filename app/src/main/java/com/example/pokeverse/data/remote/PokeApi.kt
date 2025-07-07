package com.example.pokeverse.data.remote

import com.example.pokeverse.data.remote.model.PokemonListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PokeApi {
    @GET("pokemon")
    suspend fun getPokemonList(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): PokemonListResponse
}