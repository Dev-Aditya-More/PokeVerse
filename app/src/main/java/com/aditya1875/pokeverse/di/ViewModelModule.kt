package com.aditya1875.pokeverse.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import com.aditya1875.pokeverse.presentation.ui.viewmodel.*
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel

val viewModelModule = module {

    viewModelOf(::PokemonViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::MatchViewModel)
    viewModelOf(::QuizViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::LeaderboardViewModel)
    viewModelOf(::BillingViewModel)
    viewModelOf(::DailyTriviaViewModel)
    viewModelOf(::ItemViewModel)
    viewModelOf(::PokeGuessViewModel)
    viewModelOf(::TypeRushViewModel)

}