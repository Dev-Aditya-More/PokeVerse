package com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.TypeResponse
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokemonListApi {

    @GET("pokemon")
    suspend fun getPokemonList(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): PokemonListResponse

    @GET("type/{type}")
    suspend fun getPokemonByType(@Path("type") type: String): TypeResponse

}