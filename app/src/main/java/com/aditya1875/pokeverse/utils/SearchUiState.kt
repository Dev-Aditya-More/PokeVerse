package com.aditya1875.pokeverse.utils

data class SearchUiState(
    val query: String = "",
    val suggestions: List<SearchResult> = emptyList(),
    val showSuggestions: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)