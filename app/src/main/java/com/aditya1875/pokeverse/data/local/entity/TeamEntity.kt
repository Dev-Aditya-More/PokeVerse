package com.aditya1875.pokeverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey val teamId: String = UUID.randomUUID().toString(),
    val teamName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)
