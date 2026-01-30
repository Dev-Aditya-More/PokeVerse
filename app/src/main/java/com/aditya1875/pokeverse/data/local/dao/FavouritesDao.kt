package com.aditya1875.pokeverse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aditya1875.pokeverse.data.local.entity.FavouriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouritesDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavouriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToFavorites(favorite: FavouriteEntity)

    @Delete
    suspend fun removeFromFavorites(favorite: FavouriteEntity)

    @Query("DELETE FROM favorites WHERE name = :name")
    suspend fun removeFromFavoritesByName(name: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE name = :name)")
    fun isInFavorites(name: String): Flow<Boolean>

    @Query("DELETE FROM favorites")
    suspend fun clearAllFavorites()
}