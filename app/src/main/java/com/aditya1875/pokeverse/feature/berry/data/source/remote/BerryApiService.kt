package com.aditya1875.pokeverse.feature.berry.data.source.remote

import com.aditya1875.pokeverse.feature.berry.data.source.remote.model.BerryDetail
import com.aditya1875.pokeverse.feature.berry.data.source.remote.model.BerryListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BerryApiService {
    @GET("berry")
    suspend fun getBerries(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 64
    ): BerryListResponse

    @GET("berry/{nameOrId}")
    suspend fun getBerryDetail(@Path("nameOrId") nameOrId: String): BerryDetail
}
