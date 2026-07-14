package com.aditya1875.pokeverse.di

import com.aditya1875.pokeverse.feature.game.cardclash.data.repository.CardClashRepository
import com.aditya1875.pokeverse.feature.game.cardclash.data.repository.CardClashRepositoryImpl
import com.aditya1875.pokeverse.feature.badges.domain.BadgeRepository
import com.aditya1875.pokeverse.feature.characters.domain.CharacterRepository
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.battlestats.GoStatsRepository
// TODO(friends): re-add when the feature ships
// import com.aditya1875.pokeverse.feature.friends.data.repository.FriendsRepository
import com.aditya1875.pokeverse.feature.inbox.data.repository.InboxRepository
import com.aditya1875.pokeverse.feature.leaderboard.data.repository.LeaderboardRepository
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.PokemonDetailImpl
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.berry.data.repository.BerryRepository
import com.aditya1875.pokeverse.feature.item.data.repository.ItemRepository
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.PokemonListImpl
import com.aditya1875.pokeverse.feature.pokemon.home.domain.repository.DescriptionRepo
import com.aditya1875.pokeverse.feature.pokemon.home.domain.repository.PokemonListRepo
import com.aditya1875.pokeverse.feature.pokemon.home.domain.repository.PokemonSearchRepository
import com.aditya1875.pokeverse.feature.pokemon.theme_selector.data.preferences.ThemePreferences
import com.aditya1875.pokeverse.utils.TeamMapper
import org.koin.dsl.module

val repositoryModule = module {

    single { ItemRepository(get()) }
    single { BerryRepository(get()) }

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

    single { InboxRepository() }

    // TODO(friends): re-add when the feature ships
    // single { FriendsRepository() }

    single { BadgeRepository(get()) }

    single { CharacterRepository(get()) }

    single { GoStatsRepository(get()) }

    single<CardClashRepository> { CardClashRepositoryImpl(get()) }

    single { TeamMapper }

}
