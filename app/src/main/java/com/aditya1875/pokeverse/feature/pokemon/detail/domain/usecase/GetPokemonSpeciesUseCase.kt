package com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonSpeciesResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo

class GetPokemonSpeciesUseCase(
    private val repo: PokemonDetailRepo
) {
    suspend operator fun invoke(name: String): PokemonSpeciesResponse {
        return repo.getPokemonSpeciesByName(name)
    }
}