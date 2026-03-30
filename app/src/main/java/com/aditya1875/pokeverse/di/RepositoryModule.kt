package com.aditya1875.pokeverse.di

import com.aditya1875.pokeverse.feature.leaderboard.data.repository.LeaderboardRepository
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.PokemonDetailImpl
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.pokemon.home.data.repository.ItemRepository
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.PokemonListImpl
import com.aditya1875.pokeverse.feature.pokemon.home.domain.repository.DescriptionRepo
import com.aditya1875.pokeverse.feature.pokemon.home.domain.repository.PokemonListRepo
import com.aditya1875.pokeverse.feature.pokemon.home.domain.repository.PokemonSearchRepository
import com.aditya1875.pokeverse.feature.pokemon.theme_selector.data.preferences.ThemePreferences
import com.aditya1875.pokeverse.utils.TeamMapper
import org.koin.dsl.module

val repositoryModule = module {

    single { ItemRepository(get()) }

    single<PokemonListRepo> {
        PokemonListImpl(get())
    }

    single { PokemonSearchRepository(get()) }

    single<PokemonDetailRepo> {
        PokemonDetailImpl(get())
    }

    single { ThemePreferences(get()) }

    single { DescriptionRepo(get()) }

    single { LeaderboardRepository() }

    single { TeamMapper }

}
