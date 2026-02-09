package com.aditya1875.pokeverse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.aditya1875.pokeverse.data.local.entity.TeamEntity
import com.aditya1875.pokeverse.data.local.entity.TeamMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createTeam(team: TeamEntity)

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Delete
    suspend fun deleteTeam(team: TeamEntity)

    @Query("SELECT * FROM teams ORDER BY isDefault DESC, createdAt ASC")
    fun getAllTeams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE teamId = :teamId")
    fun getTeamById(teamId: String): Flow<TeamEntity?>

    @Query("SELECT * FROM teams WHERE teamId = :teamId")
    suspend fun getTeamByIdOnce(teamId: String): TeamEntity?

    @Query("SELECT COUNT(*) FROM teams")
    suspend fun getTeamCount(): Int

    @Query("SELECT * FROM teams WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultTeam(): TeamEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM teams WHERE LOWER(teamName) = LOWER(:teamName))")
    suspend fun isTeamNameExists(teamName: String): Boolean

    // ========== TEAM MEMBER OPERATIONS ==========

    @Upsert
    suspend fun addToTeam(pokemon: TeamMemberEntity)

    @Delete
    suspend fun removeFromTeam(pokemon: TeamMemberEntity)

    @Query("DELETE FROM team_members WHERE name = :name AND teamId = :teamId")
    suspend fun removeFromTeamByName(name: String, teamId: String)

    @Query("SELECT * FROM team_members WHERE teamId = :teamId ORDER BY addedAt ASC")
    fun getTeamMembers(teamId: String): Flow<List<TeamMemberEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM team_members WHERE name = :name AND teamId = :teamId)")
    fun isInTeam(name: String, teamId: String): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM team_members WHERE teamId = :teamId")
    fun getTeamMemberCount(teamId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM team_members WHERE teamId = :teamId")
    suspend fun getTeamMemberCountOnce(teamId: String): Int

    // ========== RELATION QUERIES ==========

    @Transaction
    @Query("SELECT * FROM teams WHERE teamId = :teamId")
    fun getTeamWithMembers(teamId: String): Flow<TeamWithMembers?>

    @Transaction
    @Query("SELECT * FROM teams ORDER BY isDefault DESC, createdAt ASC")
    fun getAllTeamsWithMembers(): Flow<List<TeamWithMembers>>

    // ========== UTILITY ==========

    @Query("SELECT teamId FROM teams WHERE (SELECT COUNT(*) FROM team_members WHERE team_members.teamId = teams.teamId) < 6 ORDER BY isDefault DESC, createdAt ASC LIMIT 1")
    suspend fun getFirstAvailableTeamId(): String?
}

// Data class for team with members
data class TeamWithMembers(
    @Embedded val team: TeamEntity,
    @Relation(
        parentColumn = "teamId",
        entityColumn = "teamId"
    )
    val members: List<TeamMemberEntity>
)