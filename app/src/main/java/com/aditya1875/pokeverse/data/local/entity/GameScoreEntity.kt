package com.aditya1875.pokeverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "game_scores")
data class GameScoreEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val difficulty: String,
    val score: Int,
    val moves: Int,
    val timeSeconds: Int,
    val stars: Int,
    val playedAt: Long = System.currentTimeMillis()
)