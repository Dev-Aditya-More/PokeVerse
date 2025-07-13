package com.example.pokeverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.pokeverse.data.remote.model.StatSlot
import com.example.pokeverse.data.remote.model.TypeSlot

@Entity(tableName = "team_members")
data class TeamMemberEntity(
    @PrimaryKey val name: String,
    val imageUrl: String,
)
