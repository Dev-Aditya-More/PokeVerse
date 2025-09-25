package com.aditya1875.pokeverse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aditya1875.pokeverse.data.local.dao.TeamDao
import com.aditya1875.pokeverse.data.local.entity.TeamMemberEntity

@Database(entities = [TeamMemberEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
}