package com.example.pokeverse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pokeverse.data.local.dao.TeamDao
import com.example.pokeverse.data.local.entity.TeamMemberEntity

@Database(entities = [TeamMemberEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
}