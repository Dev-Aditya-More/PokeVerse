package com.aditya1875.pokeverse.feature.team.domain.usecase

import com.aditya1875.pokeverse.feature.team.data.local.dao.TeamDao

class RemovePokemonFromTeamUseCase(
    private val teamDao: TeamDao
) {
    suspend operator fun invoke(name: String, teamId: String) {
        teamDao.removeFromTeamByName(name, teamId)
    }
}