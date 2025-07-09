package com.example.pokeverse.data.remote

import com.example.pokeverse.data.remote.model.PokemonListResponse
import com.example.pokeverse.data.remote.model.PokemonResponse
import com.example.pokeverse.data.remote.model.PokemonSpeciesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokeApi {
    @GET("pokemon")
    suspend fun getPokemonList(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): PokemonListResponse

    @GET("pokemon/{name}")
    suspend fun getPokemonByName(@Path("name") name: String): PokemonResponse

    @GET("pokemon-species/{name}")
    suspend fun getPokemonSpeciesByName(@Path("name") name: String): PokemonSpeciesResponse
}