package com.aditya1875.pokeverse.di

import com.aditya1875.pokeverse.feature.game.pokeguess.domain.usecases.GeneratePokeGuessQuestionsUseCase
import com.aditya1875.pokeverse.feature.game.pokequiz.domain.usecase.calculateQuestionScore
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetEvolutionChainUiUseCase
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetEvolutionChainUseCase
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetPokemonByNameUseCase
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetPokemonByTypeUseCase
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetPokemonDetailUseCase
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetPokemonSpeciesUseCase
import com.aditya1875.pokeverse.feature.pokemon.home.domain.usecase.GetPokemonListUseCase
import com.aditya1875.pokeverse.feature.pokemon.home.domain.usecase.SearchPokemonUseCase
import com.aditya1875.pokeverse.feature.team.domain.usecase.AddPokemonToTeamUseCase
import com.aditya1875.pokeverse.feature.team.domain.usecase.AddToFavoritesUseCase
import com.aditya1875.pokeverse.feature.team.domain.usecase.CreateTeamUseCase
import com.aditya1875.pokeverse.feature.team.domain.usecase.RemoveFromFavoritesUseCase
import com.aditya1875.pokeverse.feature.team.domain.usecase.RemovePokemonFromTeamUseCase
import org.koin.dsl.module

val useCaseModule = module {

    single { GetPokemonListUseCase(get()) }

    single {
        println("Creating GetPokemonByTypeUseCase")
        GetPokemonByTypeUseCase(get())
    }

    single { SearchPokemonUseCase(get()) }

    single { GetPokemonDetailUseCase(get()) }

    single { GetPokemonSpeciesUseCase(get()) }

    single { GetEvolutionChainUseCase(get()) }

    single { GetEvolutionChainUiUseCase(get()) }

    single { GetPokemonByNameUseCase(get()) }

    single { AddPokemonToTeamUseCase(get(), get()) }

    single { AddToFavoritesUseCase(get(), get()) }

    single { RemoveFromFavoritesUseCase(get()) }

    single { CreateTeamUseCase(get()) }

    single { RemovePokemonFromTeamUseCase(get()) }

    single { GeneratePokeGuessQuestionsUseCase(get()) }
}