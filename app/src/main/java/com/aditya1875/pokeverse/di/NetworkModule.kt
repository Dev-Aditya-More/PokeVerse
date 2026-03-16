package com.aditya1875.pokeverse.di

import com.aditya1875.pokeverse.data.remote.PokeApi
import com.aditya1875.pokeverse.data.repository.ItemApiService
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val networkModule = module {

    single(named("pokeapi")) {
        Retrofit.Builder()
            .baseUrl("https://pokeapi.co/api/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<PokeApi> {
        get<Retrofit>(named("pokeapi")).create(PokeApi::class.java)
    }

    single { get<Retrofit>(named("pokeapi")).create(ItemApiService::class.java) }

}