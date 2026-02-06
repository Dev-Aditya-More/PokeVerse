package com.aditya1875.pokeverse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "team_members",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["teamId"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["teamId"])]
)
data class TeamMemberEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val teamId: String, // Foreign key
    val name: String,
    val imageUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)