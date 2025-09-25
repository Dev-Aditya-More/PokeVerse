package com.aditya1875.pokeverse.data.local.entity

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aditya1875.pokeverse.data.local.dao.PokemonDao

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
