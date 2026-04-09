package com.aditya1875.pokeverse.feature.pokemon.detail.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonVariety
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.EvolutionChainUi
import com.aditya1875.pokeverse.feature.pokemon.home.domain.repository.DescriptionRepo
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetEvolutionChainUiUseCase
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetPokemonByNameUseCase
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetPokemonDetailUseCase
import com.aditya1875.pokeverse.utils.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class PokemonDetailsViewModel(
    private val getPokemonDetailUseCase: GetPokemonDetailUseCase,
    private val getPokemonByNameUseCase: GetPokemonByNameUseCase,
    private val getEvolutionChainUiUseCase: GetEvolutionChainUiUseCase,
    private val descriptionRepo: DescriptionRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(PokemonDetailUiState())
    val uiState: StateFlow<PokemonDetailUiState> = _uiState

    fun loadPokemon(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val result = getPokemonDetailUseCase(name)

                _uiState.update {
                    it.copy(
                        pokemon = result.pokemon,
                        description = result.description,
                        varieties = result.varieties,
                        isLoading = false
                    )
                }

            } catch (e: IOException) {
                _uiState.update {
                    it.copy(isLoading = false, error = UiError.Network(e.message))
                }
                return@launch
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = UiError.Unexpected(e.message))
                }
                return@launch
            }

            try {
                val evolution = getEvolutionChainUiUseCase(name)
                _uiState.update { it.copy(evolutionUi = evolution) }
            } catch (_: Exception) {
                // ignore or log → don't break UI
            }
        }
    }

    fun loadVarietyPokemon(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = false) }

            val pokemon = getPokemonByNameUseCase(name)

            _uiState.update {
                it.copy(pokemon = pokemon)
            }

            try {
                _uiState.update {
                    it.copy(pokemon = pokemon, isLoading = false)
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UiError.Unexpected(e.message)
                )
            }
        }
    }

    suspend fun getPokemonByName(name: String): PokemonResponse {
        return getPokemonByNameUseCase(name)
    }

    fun getLocalDescription(pokemonId: Int): String {
        return descriptionRepo.getDescriptionById(pokemonId)
    }
}

data class PokemonDetailUiState(
    val pokemon: PokemonResponse? = null,
    val description: String = "",
    val isLoading: Boolean = false,
    val error: UiError? = null,
    val varieties: List<PokemonVariety> = emptyList(),
    val evolutionUi: EvolutionChainUi? = null
)