package com.aditya1875.pokeverse.feature.compare.presentation.viewmodels

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

enum class Side { LEFT, RIGHT }

class CompareViewModel(
    private val searchPokemonUseCase: SearchPokemonUseCase,
    private val pokemonRepo: PokemonDetailRepo
) : ViewModel() {

    private val _queryLeft = MutableStateFlow("")
    val queryLeft: StateFlow<String> = _queryLeft.asStateFlow()

    private val _queryRight = MutableStateFlow("")
    val queryRight: StateFlow<String> = _queryRight.asStateFlow()

    private val _leftPokemon = MutableStateFlow<PokemonResponse?>(null)
    val leftPokemon: StateFlow<PokemonResponse?> = _leftPokemon.asStateFlow()

    private val _rightPokemon = MutableStateFlow<PokemonResponse?>(null)
    val rightPokemon: StateFlow<PokemonResponse?> = _rightPokemon.asStateFlow()

    private val _leftLoading = MutableStateFlow(false)
    val leftLoading: StateFlow<Boolean> = _leftLoading.asStateFlow()

    private val _rightLoading = MutableStateFlow(false)
    val rightLoading: StateFlow<Boolean> = _rightLoading.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val leftSearch: StateFlow<SearchUiState> = searchFlow(_queryLeft)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val rightSearch: StateFlow<SearchUiState> = searchFlow(_queryRight)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun searchFlow(query: MutableStateFlow<String>): StateFlow<SearchUiState> =
        query
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

    fun onQueryChange(side: Side, query: String) {
        when (side) {
            Side.LEFT -> _queryLeft.value = query
            Side.RIGHT -> _queryRight.value = query
        }
    }

    fun select(side: Side, name: String) {
        viewModelScope.launch {
            when (side) {
                Side.LEFT -> _leftLoading.value = true
                Side.RIGHT -> _rightLoading.value = true
            }
            try {
                val pokemon = pokemonRepo.getPokemonByName(name)
                when (side) {
                    Side.LEFT -> {
                        _leftPokemon.value = pokemon
                        _queryLeft.value = ""
                    }
                    Side.RIGHT -> {
                        _rightPokemon.value = pokemon
                        _queryRight.value = ""
                    }
                }
            } catch (_: Exception) {
                // Leave the previous selection (if any) in place on failure.
            } finally {
                when (side) {
                    Side.LEFT -> _leftLoading.value = false
                    Side.RIGHT -> _rightLoading.value = false
                }
            }
        }
    }

    fun clear(side: Side) {
        when (side) {
            Side.LEFT -> _leftPokemon.value = null
            Side.RIGHT -> _rightPokemon.value = null
        }
    }
}
