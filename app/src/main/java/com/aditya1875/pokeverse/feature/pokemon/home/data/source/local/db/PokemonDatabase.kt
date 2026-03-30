package com.aditya1875.pokeverse.feature.pokemon.home.data.source.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.local.dao.PokemonDetailDao
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.local.entity.PokemonDetailEntity
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.local.dao.PokemonListDao
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.local.entity.PokemonListEntity

@Database(
    entities = [
        PokemonListEntity::class,
        PokemonDetailEntity::class
    ],
    version = 1
)
abstract class PokemonDatabase : RoomDatabase() {
    abstract fun pokemonlistDao(): PokemonListDao
    abstract fun pokemonDetailDao(): PokemonDetailDao
}