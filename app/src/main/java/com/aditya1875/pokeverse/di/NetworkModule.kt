package com.aditya1875.pokeverse.di

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.PokemonDetailsApi
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.PokemonListApi
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.ItemApiService
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

    single<PokemonListApi> {
        get<Retrofit>(named("pokeapi")).create(PokemonListApi::class.java)
    }

    single<PokemonDetailsApi> {
        get<Retrofit>(named("pokeapi")).create(PokemonDetailsApi::class.java)
    }

    single { get<Retrofit>(named("pokeapi")).create(ItemApiService::class.java) }

}