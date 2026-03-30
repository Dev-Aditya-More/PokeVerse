package com.aditya1875.pokeverse.feature.pokemon.home.domain.usecase

import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonListResponse
import com.aditya1875.pokeverse.feature.pokemon.home.domain.repository.PokemonListRepo

class GetPokemonListUseCase(
    private val repo: PokemonListRepo
) {
    suspend operator fun invoke(limit: Int, offset: Int): PokemonListResponse {
        return repo.getPokemonList(limit, offset)
    }
}