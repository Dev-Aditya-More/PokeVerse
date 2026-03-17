package com.aditya1875.pokeverse.di

import com.aditya1875.pokeverse.data.repository.*
import com.aditya1875.pokeverse.domain.repository.*
import com.aditya1875.pokeverse.data.preferences.ThemePreferences
import com.aditya1875.pokeverse.utils.TeamMapper
import org.koin.dsl.module

val repositoryModule = module {

    single { ItemRepository(get()) }

    single<PokemonRepo> {
        PokemonRepoImpl(get())
    }

    single { PokemonSearchRepository(api = get()) }

    single { ThemePreferences(get()) }

    single { DescriptionRepo(get()) }

    single { LeaderboardRepository() }

    single { TeamMapper }

}