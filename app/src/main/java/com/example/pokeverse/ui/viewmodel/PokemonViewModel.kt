package com.example.pokeverse.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeverse.data.local.dao.TeamDao
import com.example.pokeverse.data.local.entity.TeamMemberEntity
import com.example.pokeverse.data.remote.model.PokemonResponse
import com.example.pokeverse.data.remote.model.PokemonResult
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
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load Pokémon")
                }
            }
        }
    }
    suspend fun fetchPokemonDataAndWait(name: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                fetchPokemonData(name)
                delay(500) // Adjust if needed; wait for state to be updated
                uiState.value.pokemon != null
            } catch (e: Exception) {
                false
            }
        }
    }


    fun addToTeam(pokemonResult: PokemonResult) = viewModelScope.launch {
        val pokemonResponse = repository.getPokemonByName(pokemonResult.name) // Fetch full data
        val entity = pokemonResponse.toEntity() // Use extension on PokemonResponse
        teamDao.addToTeam(entity)
    }

    fun PokemonResponse.toEntity(): TeamMemberEntity {
        return TeamMemberEntity(
            name = this.name,
            imageUrl = this.sprites.front_default ?: "" // Use the actual image URL
        )
    }

    fun removeFromTeam(pokemon: TeamMemberEntity) = viewModelScope.launch {
        teamDao.removeFromTeam(pokemon)
    }
    fun isInTeam(name: String): Flow<Boolean> = teamDao.isInTeam(name)

    val team: StateFlow<List<TeamMemberEntity>> = teamDao.getTeam()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
}
