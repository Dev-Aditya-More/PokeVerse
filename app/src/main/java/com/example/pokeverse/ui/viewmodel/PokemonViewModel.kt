package com.example.pokeverse.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeverse.data.remote.model.PokemonListResponse
import com.example.pokeverse.data.remote.model.PokemonResponse
import com.example.pokeverse.data.remote.model.PokemonResult
import com.example.pokeverse.domain.repository.PokemonRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PokemonViewModel(
    private val repository: PokemonRepo
) : ViewModel() {

    data class PokemonDetailUiState(
        val pokemon: PokemonResponse? = null,
        val description: String = "",
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(PokemonDetailUiState())
    val uiState: StateFlow<PokemonDetailUiState> = _uiState

    private val _pokemonList = MutableStateFlow<List<PokemonResult>>(emptyList())

    val pokemonList: StateFlow<List<PokemonResult>> = _pokemonList


    var isLoading by mutableStateOf(false)
    var endReached by mutableStateOf(false)

    private var currentOffset = 0
    private val limit = 20

    fun loadPokemonList() {
        if (isLoading || endReached) return

        isLoading = true
        viewModelScope.launch {
            try {
                val result = repository.getPokemonList(limit = limit, offset = currentOffset)
                val newList = result.results
                currentOffset += limit

                // If fetched less than requested, assume end reached
                if (newList.isEmpty() || newList.size < limit) {
                    endReached = true
                }

                // Append new results to the list
                _pokemonList.value = _pokemonList.value + newList
            } catch (e: Exception) {
                // handle error or emit error state if needed
            } finally {
                isLoading = false
            }
        }
    }

    fun refreshPokemonList() {
        currentOffset = 0
        endReached = false
        _pokemonList.value = emptyList()
        loadPokemonList()
    }

    fun fetchPokemonData(name: String) {
        viewModelScope.launch {
            try {
                val pokemon = repository.getPokemonByName(name)
                val species = repository.getPokemonSpeciesByName(name)
                val desc = species.flavorTextEntries.firstOrNull {
                    it.language.name == "en"
                }?.flavorText?.replace("\n", " ")?.replace("\u000c", " ")
                    ?: "Description not available."

                _uiState.value = PokemonDetailUiState(
                    pokemon = pokemon,
                    description = desc,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }
}
