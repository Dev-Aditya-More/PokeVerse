package com.aditya1875.pokeverse.di

import com.aditya1875.pokeverse.feature.game.pokematch.domain.engine.MatchGameEngine
import com.aditya1875.pokeverse.feature.game.poketype.data.generator.TypeRushQuestionGenerator
import com.aditya1875.pokeverse.feature.game.poketype.domain.engine.TypeRushEngine
import org.koin.dsl.module

val gameModule = module {
    single { TypeRushQuestionGenerator(get()) }
    single { TypeRushEngine() }
    single { MatchGameEngine() }
}