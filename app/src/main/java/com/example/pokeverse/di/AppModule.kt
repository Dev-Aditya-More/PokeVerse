package com.example.pokeverse.di

import androidx.appcompat.widget.AppCompatDrawableManager.get
import org.koin.androidx.viewmodel.dsl.viewModel
import com.example.pokeverse.data.remote.PokeApi
import com.example.pokeverse.data.repository.PokemonRepoImpl
import com.example.pokeverse.domain.repository.PokemonRepo
import com.example.pokeverse.ui.viewmodel.PokemonViewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {

    // Retrofit instance
    single {
        Retrofit.Builder()
            .baseUrl("https://pokeapi.co/api/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // API Service
    single<PokeApi> {
        get<Retrofit>().create(PokeApi::class.java)
    }

    // Repository
    single<PokemonRepo> {
        PokemonRepoImpl(get())
    }

    // ViewModel
    viewModel {
        PokemonViewModel(get())
    }
}