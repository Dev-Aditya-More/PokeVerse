package com.aditya1875.pokeverse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aditya1875.pokeverse.data.local.entity.GameScoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameScoreDao {
    @Insert
    suspend fun insertScore(score: GameScoreEntity)

    @Query("SELECT * FROM game_scores ORDER BY score DESC LIMIT 10")
    fun getTopScores(): Flow<List<GameScoreEntity>>

    @Query("SELECT * FROM game_scores WHERE difficulty = :diff ORDER BY score DESC LIMIT 1")
    suspend fun getBestScore(diff: String): GameScoreEntity?

    @Query("SELECT COUNT(*) FROM game_scores")
    suspend fun getTotalGamesPlayed(): Int

    @Query("SELECT * FROM game_scores ORDER BY playedAt DESC LIMIT 20")
    fun getRecentScores(): Flow<List<GameScoreEntity>>
}