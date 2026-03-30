package com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo

class GetPokemonByNameUseCase(
    private val repo: PokemonDetailRepo
) {
    suspend operator fun invoke(name: String): PokemonResponse {
        return repo.getPokemonByName(name)
    }
}