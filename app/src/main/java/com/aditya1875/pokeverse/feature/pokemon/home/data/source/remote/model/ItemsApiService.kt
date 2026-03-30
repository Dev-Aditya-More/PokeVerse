package com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model

import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.itemModels.ItemDetail
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.itemModels.ItemListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ItemApiService {
    @GET("item")
    suspend fun getItems(
        @Query("offset") offset: Int = 0,
        @Query("limit")  limit: Int  = 40
    ): ItemListResponse

    @GET("item/{nameOrId}")
    suspend fun getItemDetail(@Path("nameOrId") nameOrId: String): ItemDetail
}