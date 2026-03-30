package com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.pokemon.home.domain.usecase.SearchPokemonUseCase
import com.aditya1875.pokeverse.utils.SearchUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(
    private val searchPokemonUseCase: SearchPokemonUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchUiState: StateFlow<SearchUiState> =
        _query
            .debounce(300)
            .onEach { _isSearching.value = it.isNotBlank() }
            .flatMapLatest { query ->
                flow {
                    val cleaned = query.trim().lowercase()

                    if (cleaned.isEmpty()) {
                        emit(SearchUiState(query = cleaned))
                        return@flow
                    }

                    try {
                        val results = searchPokemonUseCase(cleaned)

                        emit(
                            SearchUiState(
                                query = cleaned,
                                suggestions = results,
                                showSuggestions = results.isNotEmpty()
                            )
                        )

                    } catch (e: Exception) {
                        emit(SearchUiState(query = cleaned, error = "Search failed"))
                    }

                    _isSearching.value = false
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun onQueryChange(query: String) {
        _query.value = query
    }
}