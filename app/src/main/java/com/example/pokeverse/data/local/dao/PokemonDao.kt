package com.example.pokeverse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pokeverse.data.local.entity.PokemonDetailEntity
import com.example.pokeverse.data.local.entity.PokemonListEntity

@Dao
interface PokemonDao {

    // List
    @Query("SELECT * FROM pokemon_list WHERE page = :page")
    suspend fun getPokemonList(page: Int): PokemonListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPokemonList(entity: PokemonListEntity)

    // Detail
    @Query("SELECT * FROM pokemon_detail WHERE name = :name")
    suspend fun getPokemonDetail(name: String): PokemonDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPokemonDetail(entity: PokemonDetailEntity)

}
