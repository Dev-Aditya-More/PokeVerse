package com.example.pokeverse.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeverse.data.local.dao.TeamDao
import com.example.pokeverse.data.local.entity.TeamMemberEntity
import com.example.pokeverse.data.remote.model.PokemonResponse
import com.example.pokeverse.data.remote.model.PokemonResult
import com.example.pokeverse.data.remote.model.PokemonVariety
import com.example.pokeverse.domain.repository.PokemonRepo
import com.example.pokeverse.utils.TeamMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PokemonViewModel(
    private val repository: PokemonRepo,
    private val teamDao: TeamDao,
    private val mapper: TeamMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(PokemonDetailUiState())
    val uiState: StateFlow<PokemonDetailUiState> = _uiState

    val megaForms: List<PokemonVariety>
        get() = _uiState.value.varieties.filter { !it.isDefault }

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

                if (newList.isEmpty() || newList.size < limit) {
                    endReached = true
                }

                _pokemonList.value = _pokemonList.value + newList
            } catch (e: Exception) {
                // Handle error if needed
            } finally {
                isLoading = false
            }
        }
    }

    /** Main entry to fetch full Pokémon info + species */
    fun fetchPokemonData(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            fetchPokemonInternal(name, includeSpecies = true)
        }
    }

    /** Called when a variety like 'charizard-mega-x' is selected */
    fun fetchVarietyPokemon(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            fetchPokemonInternal(name, includeSpecies = false)
        }
    }

    private suspend fun fetchPokemonInternal(name: String, includeSpecies: Boolean) {
        try {
            val pokemon = repository.getPokemonByName(name)

            val description = if (includeSpecies) {
                val species = repository.getPokemonSpeciesByName(name)
                species.flavorTextEntries.firstOrNull {
                    it.language.name == "en"
                }?.flavorText?.replace("\n", " ")?.replace("\u000c", " ")
                    ?: "Description not available."
            } else {
                "Variety form of ${pokemon.name.capitalize()}"
            }

            val varieties = if (includeSpecies) {
                repository.getPokemonSpeciesByName(name).varieties
            } else {
                _uiState.value.varieties // retain existing
            }

            _uiState.value = PokemonDetailUiState(
                pokemon = pokemon,
                description = description,
                varieties = varieties,
                isLoading = false,
                error = null
            )

            Log.d("PokeVM", "Loaded $name successfully")

        } catch (e: Exception) {
            Log.e("PokeVM", "Failed to load $name", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to load Pokémon"
            )
        }
    }

    suspend fun fetchPokemonDataAndWait(name: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                fetchPokemonData(name)
                delay(500)
                uiState.value.pokemon != null
            } catch (e: Exception) {
                false
            }
        }
    }

    fun addToTeam(pokemonResult: PokemonResult) = viewModelScope.launch {
        val pokemonResponse = repository.getPokemonByName(pokemonResult.name)
        val entity = pokemonResponse.toEntity()
        teamDao.addToTeam(entity)
    }

    private fun PokemonResponse.toEntity(): TeamMemberEntity {
        return TeamMemberEntity(
            name = this.name,
            imageUrl = this.sprites.front_default ?: ""
        )
    }

    fun removeFromTeam(pokemon: TeamMemberEntity) = viewModelScope.launch {
        teamDao.removeFromTeam(pokemon)
    }

    fun isInTeam(name: String): Flow<Boolean> = teamDao.isInTeam(name)

    val team: StateFlow<List<TeamMemberEntity>> = teamDao.getTeam()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
}

data class PokemonDetailUiState(
    val pokemon: PokemonResponse? = null,
    val description: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val varieties: List<PokemonVariety> = emptyList()
)