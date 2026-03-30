package com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.EvolutionChainResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo

class GetEvolutionChainUseCase(
    private val repo: PokemonDetailRepo
) {
    suspend operator fun invoke(id: Int): EvolutionChainResponse {
        return repo.getEvolutionChain(id)
    }
}