package com.aditya1875.pokeverse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.aditya1875.pokeverse.data.local.entity.TeamMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Upsert
    suspend fun addToTeam(pokemon: TeamMemberEntity)

    @Delete
    suspend fun removeFromTeam(pokemon: TeamMemberEntity)

    @Query("DELETE FROM team_members WHERE name = :name")
    suspend fun removeFromTeamByName(name: String)

    @Query("SELECT * FROM team_members")
    fun getTeam(): Flow<List<TeamMemberEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM team_members WHERE name = :name)")
    fun isInTeam(name: String): Flow<Boolean>
}