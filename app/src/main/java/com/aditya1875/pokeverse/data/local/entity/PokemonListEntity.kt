package com.aditya1875.pokeverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pokemon_list")
data class PokemonListEntity(
    @PrimaryKey val page: Int,
    val resultsJson: String
)
