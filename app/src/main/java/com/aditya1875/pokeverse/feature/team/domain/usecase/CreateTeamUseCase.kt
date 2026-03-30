package com.aditya1875.pokeverse.feature.team.domain.usecase

import com.aditya1875.pokeverse.feature.team.data.local.dao.TeamDao
import com.aditya1875.pokeverse.feature.team.data.local.entity.TeamEntity

class CreateTeamUseCase(
    private val teamDao: TeamDao
) {
    suspend operator fun invoke(name: String): Result<Unit> {
        return try {
            if (teamDao.isTeamNameExists(name)) {
                Result.failure(Exception("Team already exists"))
            } else {
                teamDao.createTeam(TeamEntity(teamName = name.trim()))
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}