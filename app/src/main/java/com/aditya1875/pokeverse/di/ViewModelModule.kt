package com.aditya1875.pokeverse.di

import com.aditya1875.pokeverse.feature.game.pokeguess.presentation.viewmodels.PokeGuessViewModel
import com.aditya1875.pokeverse.feature.game.pokematch.presentation.viewmodels.MatchViewModel
import com.aditya1875.pokeverse.feature.game.pokequiz.presentation.viewmodels.QuizViewModel
import com.aditya1875.pokeverse.feature.game.poketype.presentation.viewmodels.TypeRushViewModel
import com.aditya1875.pokeverse.feature.item.presentation.viewmodels.ItemViewModel
import com.aditya1875.pokeverse.feature.leaderboard.presentation.viewmodels.LeaderboardViewModel
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.viewmodels.PokemonDetailsViewModel
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels.DailyTriviaViewModel
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels.PokemonListViewModel
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels.SearchViewModel
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels.ProfileViewModel
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.viewmodels.SettingsViewModel
import com.aditya1875.pokeverse.feature.team.presentation.viewmodels.FavouritesViewModel
import com.aditya1875.pokeverse.feature.team.presentation.viewmodels.TeamViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel

val viewModelModule = module {

    viewModelOf(::PokemonListViewModel)
    viewModelOf(::PokemonDetailsViewModel)
    viewModelOf(::FavouritesViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::TeamViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::QuizViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::LeaderboardViewModel)
    viewModelOf(::BillingViewModel)
    viewModelOf(::DailyTriviaViewModel)
    viewModelOf(::ItemViewModel)
    viewModelOf(::PokeGuessViewModel)
    viewModelOf(::TypeRushViewModel)
    viewModelOf(::MatchViewModel)
}