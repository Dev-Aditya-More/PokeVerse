package com.aditya1875.pokeverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "team_members")
data class TeamMemberEntity(
    @PrimaryKey val id: Int = UUID.randomUUID().hashCode(),
    val name: String,
    val imageUrl: String,
)
