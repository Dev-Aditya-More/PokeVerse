package com.example.pokeverse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeverse.data.remote.model.PokemonListResponse
import com.example.pokeverse.data.remote.model.PokemonResponse
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

    private val _pokemonList = MutableStateFlow<PokemonListResponse?>(null)
    val pokemonList: StateFlow<PokemonListResponse?> = _pokemonList

    fun loadPokemonList(limit: Int = 50, offset: Int = 0) {
        viewModelScope.launch {
            try {
                val result = repository.getPokemonList(limit, offset)
                _pokemonList.value = result
            } catch (e: Exception) {
                _pokemonList.value = null
            }
        }
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
