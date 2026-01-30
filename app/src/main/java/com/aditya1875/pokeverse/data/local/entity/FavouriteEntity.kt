package com.aditya1875.pokeverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavouriteEntity(
    @PrimaryKey
    val name: String,
    val imageUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)