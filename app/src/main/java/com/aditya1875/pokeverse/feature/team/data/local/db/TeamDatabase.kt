package com.aditya1875.pokeverse.feature.team.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aditya1875.pokeverse.feature.game.core.data.local.dao.GameScoreDao
import com.aditya1875.pokeverse.feature.game.core.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.feature.team.data.local.dao.FavouritesDao
import com.aditya1875.pokeverse.feature.team.data.local.dao.TeamDao
import com.aditya1875.pokeverse.feature.team.data.local.entity.FavouriteEntity
import com.aditya1875.pokeverse.feature.team.data.local.entity.TeamEntity
import com.aditya1875.pokeverse.feature.team.data.local.entity.TeamMemberEntity

@Database(
    entities = [TeamEntity::class, TeamMemberEntity::class, FavouriteEntity::class, GameScoreEntity::class],
    version = 4,
    exportSchema = false
)
abstract class TeamDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
    abstract fun favoritesDao(): FavouritesDao

    abstract fun gameScoreDao(): GameScoreDao
}