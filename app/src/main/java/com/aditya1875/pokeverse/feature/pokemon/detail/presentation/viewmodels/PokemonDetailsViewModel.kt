package com.aditya1875.pokeverse.feature.pokemon.detail.presentation.viewmodels

import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.FlavorTextEntry
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonVariety
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.EvolutionChainUi
import com.aditya1875.pokeverse.feature.pokemon.home.domain.repository.DescriptionRepo
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetEvolutionChainUiUseCase
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetPokemonByNameUseCase
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetPokemonDetailUseCase
import com.aditya1875.pokeverse.utils.LoadErrorKind
import com.aditya1875.pokeverse.utils.UiError
import com.aditya1875.pokeverse.utils.toLoadErrorKind
import com.aditya1875.pokeverse.utils.toUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class PokemonDetailsViewModel(
    private val getPokemonDetailUseCase: GetPokemonDetailUseCase,
    private val getPokemonByNameUseCase: GetPokemonByNameUseCase,
    private val getEvolutionChainUiUseCase: GetEvolutionChainUiUseCase,
    private val descriptionRepo: DescriptionRepo,
    private val xpManager: XPManager,
    private val pokedex: PokedexRepository
) : ViewModel() {

    /** Emits the dex number when this detail page registers a Pokémon for the first time. */
    private val _newEntry = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val newEntry: SharedFlow<Int> = _newEntry.asSharedFlow()

    private val _uiState = MutableStateFlow(PokemonDetailUiState())
    val uiState: StateFlow<PokemonDetailUiState> = _uiState

    private var lastName: String? = null

    /** Tries the last Pokémon again (the error screen's Retry, and the auto-retry when back online). */
    fun retry() {
        lastName?.let { loadPokemon(it) }
    }

    fun loadPokemon(name: String) {
        lastName = name
        viewModelScope.launch {
            xpManager.awardGameXP(XPEvent.FirstExplorationOfDay)
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val result = getPokemonDetailUseCase(name)

                _uiState.update {
                    it.copy(
                        pokemon = result.pokemon,
                        description = result.description,
                        varieties = result.varieties,
                        flavorTextEntries = result.flavorTextEntries,
                        isLoading = false,
                        evolutionError = null
                    )
                }

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keeps the real reason (offline, slow, server) so the screen can say so.
                _uiState.update { it.copy(isLoading = false, error = e.toUiError()) }
                return@launch
            }

            loadEvolution(name)
        }
    }

    /** The evolution chain is a separate request: if it fails the page stays, with a retry in its place. */
    private suspend fun loadEvolution(name: String) {
        try {
            val evolution = getEvolutionChainUiUseCase(name)
            _uiState.update { it.copy(evolutionUi = evolution, evolutionError = null) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update { it.copy(evolutionError = e.toLoadErrorKind()) }
        }
    }

    fun retryEvolution() {
        val name = lastName ?: return
        _uiState.update { it.copy(evolutionError = null) }
        viewModelScope.launch { loadEvolution(name) }
    }

    fun consumeVarietyError() {
        _uiState.update { it.copy(varietyFailed = false) }
    }

    fun loadVarietyPokemon(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val pokemon = getPokemonByNameUseCase(name)
                _uiState.update { it.copy(pokemon = pokemon, isLoading = false) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // The page the player is reading is fine: keep it, and just say the form didn't load.
                _uiState.update { it.copy(isLoading = false, varietyFailed = true) }
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
    val evolutionUi: EvolutionChainUi? = null,
    val flavorTextEntries: List<FlavorTextEntry> = emptyList(),
    /** Set when only the evolution chain failed to load; the rest of the page is fine. */
    val evolutionError: LoadErrorKind? = null,
    /** Set when switching to another form failed; the current page stays up. */
    val varietyFailed: Boolean = false
)