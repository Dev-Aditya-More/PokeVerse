package com.aditya1875.pokeverse.feature.pokemon.home.domain.usecase

import com.aditya1875.pokeverse.feature.pokemon.home.domain.repository.PokemonSearchRepository
import com.aditya1875.pokeverse.utils.SearchResult

class SearchPokemonUseCase(
    private val searchRepo: PokemonSearchRepository
) {
    suspend operator fun invoke(query: String): List<SearchResult> {
        return searchRepo.searchPokemon(query, limit = 10)
    }
}