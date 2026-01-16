package com.aditya1875.pokeverse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aditya1875.pokeverse.data.local.dao.FavouritesDao
import com.aditya1875.pokeverse.data.local.dao.TeamDao
import com.aditya1875.pokeverse.data.local.entity.FavouriteEntity
import com.aditya1875.pokeverse.data.local.entity.TeamMemberEntity

@Database(entities = [TeamMemberEntity::class, FavouriteEntity::class], version = 2)
abstract class TeamDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
    abstract fun favoritesDao(): FavouritesDao
}