package com.aditya1875.pokeverse.feature.pokemon.home.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.local.entity.PokemonDetailEntity
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.local.entity.PokemonListEntity

@Dao
interface PokemonListDao {

    // List
    @Query("SELECT * FROM pokemon_list WHERE page = :page")
    suspend fun getPokemonList(page: Int): PokemonListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPokemonList(entity: PokemonListEntity)

}