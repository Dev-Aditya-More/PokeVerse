package com.aditya1875.pokeverse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aditya1875.pokeverse.data.local.dao.PokemonDao
import com.aditya1875.pokeverse.data.local.entity.PokemonDetailEntity
import com.aditya1875.pokeverse.data.local.entity.PokemonListEntity

@Database(
    entities = [
        PokemonListEntity::class,
        PokemonDetailEntity::class
    ],
    version = 1
)
abstract class PokemonDatabase : RoomDatabase() {
    abstract fun pokemonDao(): PokemonDao
}