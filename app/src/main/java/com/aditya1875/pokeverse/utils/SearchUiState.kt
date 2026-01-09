package com.aditya1875.pokeverse.utils

import com.aditya1875.pokeverse.data.remote.model.PokemonSearchIndex

data class SearchUiState(
    val query: String = "",
    val suggestions: List<PokemonSearchIndex> = emptyList(),
    val showSuggestions: Boolean = false
)
