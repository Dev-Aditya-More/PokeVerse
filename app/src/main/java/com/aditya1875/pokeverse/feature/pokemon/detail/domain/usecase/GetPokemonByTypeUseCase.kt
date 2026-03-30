package com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.TypeResponse
import com.aditya1875.pokeverse.feature.pokemon.home.domain.repository.PokemonListRepo

class GetPokemonByTypeUseCase(
    private val repo: PokemonListRepo
) {
    suspend operator fun invoke(type: String): TypeResponse {
        return repo.getPokemonByType(type)
    }
}