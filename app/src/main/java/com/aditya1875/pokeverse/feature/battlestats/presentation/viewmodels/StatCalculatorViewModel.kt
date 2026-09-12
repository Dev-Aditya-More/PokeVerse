package com.aditya1875.pokeverse.feature.battlestats.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.pokemon.home.domain.usecase.SearchPokemonUseCase
import com.aditya1875.pokeverse.utils.SearchUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single-Pokémon collapse of [com.aditya1875.pokeverse.feature.compare.presentation.viewmodels.CompareViewModel] —
 * same debounced search pattern, just one side instead of two, for the
 * standalone Stat Calculator tool.
 */
class StatCalculatorViewModel(
    private val searchPokemonUseCase: SearchPokemonUseCase,
    private val pokemonRepo: PokemonDetailRepo
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _pokemon = MutableStateFlow<PokemonResponse?>(null)
    val pokemon: StateFlow<PokemonResponse?> = _pokemon.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val search: StateFlow<SearchUiState> = _query
        .debounce(300)
        .flatMapLatest { q ->
            flow {
                val cleaned = q.trim().lowercase()
                if (cleaned.isEmpty()) {
                    emit(SearchUiState(query = cleaned))
                    return@flow
                }
                try {
                    val results = searchPokemonUseCase(cleaned)
                    emit(SearchUiState(query = cleaned, suggestions = results, showSuggestions = results.isNotEmpty()))
                } catch (e: Exception) {
                    emit(SearchUiState(query = cleaned, error = "Search failed"))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun select(name: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _pokemon.value = pokemonRepo.getPokemonByName(name)
                _query.value = ""
            } catch (_: Exception) {
                // Leave the previous selection (if any) in place on failure.
            } finally {
                _loading.value = false
            }
        }
    }

    fun clear() {
        _pokemon.value = null
    }
}
