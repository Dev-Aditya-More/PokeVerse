package com.aditya1875.pokeverse.feature.team.domain.usecase

import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.team.data.local.dao.TeamDao
import com.aditya1875.pokeverse.feature.team.data.local.entity.TeamMemberEntity

class AddPokemonToTeamUseCase(
    private val repo: PokemonDetailRepo,
    private val teamDao: TeamDao
) {
    suspend operator fun invoke(name: String, teamId: String) {
        val pokemon = repo.getPokemonByName(name)

        val entity = TeamMemberEntity(
            teamId = teamId,
            name = pokemon.name,
            imageUrl = pokemon.sprites.front_default ?: ""
        )

        teamDao.addToTeam(entity)
    }
}