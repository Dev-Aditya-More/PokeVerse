package com.aditya1875.pokeverse.di

import androidx.room.Room
import com.aditya1875.pokeverse.data.local.AppDatabase
import com.aditya1875.pokeverse.data.local.entity.PokemonDatabase
import org.koin.androidx.viewmodel.dsl.viewModel
import com.aditya1875.pokeverse.data.remote.PokeApi
import com.aditya1875.pokeverse.data.repository.PokemonRepoImpl
import com.aditya1875.pokeverse.domain.repository.DescriptionRepo
import com.aditya1875.pokeverse.domain.repository.PokemonRepo
import com.aditya1875.pokeverse.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.ui.viewmodel.SettingsViewModel
import com.aditya1875.pokeverse.utils.TeamMapper
import com.google.gson.Gson
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {

    // Retrofit for PokéAPI
    single(named("pokeapi")) {
        Retrofit.Builder()
            .baseUrl("https://pokeapi.co/api/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // API Services
    single<PokeApi> {
        get<Retrofit>(named("pokeapi")).create(PokeApi::class.java)
    }

    // Repos
    single<PokemonRepo> {
        PokemonRepoImpl(get())
    }

    // Room, Dao, Mapper
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "pokeverse_db"
        ).build()
    }

    single { get<AppDatabase>().teamDao() }
    single { TeamMapper }

    // ViewModels
    viewModel {
        PokemonViewModel(get(), get(), get())
    }

    viewModel {
        SettingsViewModel(androidContext())
    }

    single { DescriptionRepo(androidContext()) }

    single {
        Room.databaseBuilder(
            get(),
            PokemonDatabase::class.java,
            "pokemon.db"
        ).build()
    }

    single { get<PokemonDatabase>().pokemonDao() }

    single { Gson() }

}
