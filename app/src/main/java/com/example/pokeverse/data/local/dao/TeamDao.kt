package com.example.pokeverse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.pokeverse.data.local.entity.TeamMemberEntity
import com.example.pokeverse.data.remote.model.PokemonResult
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Upsert
    suspend fun addToTeam(pokemon: TeamMemberEntity)

    @Delete
    suspend fun removeFromTeam(pokemon: TeamMemberEntity)

    @Query("SELECT * FROM team_members")
    fun getTeam(): Flow<List<TeamMemberEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM team_members WHERE name = :name)")
    fun isInTeam(name: String): Flow<Boolean>
}