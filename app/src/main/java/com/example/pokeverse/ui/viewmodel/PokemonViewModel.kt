package com.example.pokeverse.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeverse.data.local.dao.TeamDao
import com.example.pokeverse.data.local.entity.TeamMemberEntity
import com.example.pokeverse.data.remote.model.PokemonFilter
import com.example.pokeverse.data.remote.model.PokemonResponse
import com.example.pokeverse.data.remote.model.PokemonResult
import com.example.pokeverse.data.remote.model.PokemonVariety
import com.example.pokeverse.data.remote.model.Region
import com.example.pokeverse.data.remote.model.evolutionModels.Chain
import com.example.pokeverse.data.remote.model.evolutionModels.EvolutionChainResponse
import com.example.pokeverse.data.remote.model.evolutionModels.EvolutionStage
import com.example.pokeverse.domain.repository.DescriptionRepo
import com.example.pokeverse.domain.repository.PokemonRepo
import com.example.pokeverse.utils.TeamMapper
import com.example.pokeverse.utils.UiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import java.util.Locale
import kotlin.collections.filter

class PokemonViewModel(
    private val repository: PokemonRepo,
    private val teamDao: TeamDao,
    private val descriptionLocalRepository: DescriptionRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(PokemonDetailUiState())
    val uiState: StateFlow<PokemonDetailUiState> = _uiState

    private val _evolutionStages = MutableStateFlow<List<EvolutionStage>>(emptyList())
    val evolutionStages: StateFlow<List<EvolutionStage>> = _evolutionStages

    private val _pokemonList = MutableStateFlow<List<PokemonResult>>(emptyList())
    val pokemonList: StateFlow<List<PokemonResult>> = _pokemonList

    var isLoading by mutableStateOf(false)
    var endReached by mutableStateOf(false)

    var listError by mutableStateOf<String?>(null)
        private set

    private var currentOffset = 0
    private val limit = 20

    private val _filters = MutableStateFlow(PokemonFilter())
    val filters: StateFlow<PokemonFilter> = _filters

    // -------------------------
    // Local description support
    // -------------------------
    fun getLocalDescription(pokemonId: Int): String {
        return descriptionLocalRepository.getDescriptionById(pokemonId)
    }

    // -------------------------
    // Pokémon List Loading
    // -------------------------
    fun loadPokemonList(isNewRegion: Boolean = false) {
        if (isLoading || endReached) return

        val selectedRegion = filters.value.selectedRegion
        val regionRange = selectedRegion?.range

        val regionStart = selectedRegion?.offset ?: 0
        val regionEnd = regionStart + (selectedRegion?.limit ?: Int.MAX_VALUE)

        isLoading = true
        viewModelScope.launch {
            try {
                val result = repository.getPokemonList(limit = limit, offset = currentOffset)
                val newList = result.results

                val filteredList = newList
                    .map { it to extractIdFromUrl(it.url) }
                    .filter { (_, id) -> regionRange?.contains(id) ?: true }
                    .map { it.first }

                if (isNewRegion) {
                    _pokemonList.value = filteredList
                } else {
                    _pokemonList.value += filteredList
                }

                currentOffset += limit

                if (newList.isEmpty() || newList.size < limit || currentOffset >= regionEnd) {
                    endReached = true
                }

            } catch (e: Exception) {
                val error = if (e is UnknownHostException) {
                    UiError.NoInternet
                } else {
                    UiError.Unexpected(e.localizedMessage)
                }
                _uiState.value = PokemonDetailUiState(error = error)
            }
        }
    }

    // -------------------------
    // Pokémon Detail Loading
    // -------------------------
    fun fetchPokemonData(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            fetchPokemonInternal(name, includeSpecies = true)

            uiState.value.pokemon?.id?.let { id ->
                fetchEvolutionChain(id)
            }
        }
    }


    fun fetchVarietyPokemon(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            fetchPokemonInternal(name, includeSpecies = false)
        }
    }

    private suspend fun fetchPokemonInternal(
        name: String,
        includeSpecies: Boolean
    ) {
        try {
            val pokemon = repository.getPokemonByName(name)

            var description = "Description not available."
            var varieties: List<PokemonVariety> = emptyList()
            var evolutionChainId: Int? = null

            if (includeSpecies) {
                val species = repository.getPokemonSpeciesByName(name)

                description = species.flavorTextEntries.firstOrNull {
                    it.language.name == "en"
                }?.flavorText
                    ?.replace("\n", " ")
                    ?.replace("\u000c", " ")
                    ?: description

                varieties = species.varieties

                // Extract evolution chain ID once
                val evolutionChainId = species.evolutionChain?.id
            }

            _uiState.value = PokemonDetailUiState(
                pokemon = pokemon,
                description = description,
                varieties = varieties,
                evolutionChainId = null,
                isLoading = false,
                error = null
            )

            Log.d("PokeVM", "Loaded $name successfully")

        } catch (e: Exception) {
            Log.e("PokeVM", "Failed to load $name", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = UiError.Unexpected(e.localizedMessage)
            )
        }
    }

    // -------------------------
    // Team Management
    // -------------------------
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

    // -------------------------
    // Filters
    // -------------------------
    fun setRegionFilter(region: Region?) {
        _filters.update { it.copy(selectedRegion = region) }

        currentOffset = region?.offset ?: 0
        endReached = false
        _pokemonList.value = emptyList()

        loadPokemonList(isNewRegion = true)
    }

    // -------------------------
    // Evolution Chain
    // -------------------------
    fun fetchEvolutionChain(chainId: Int) {
        viewModelScope.launch {
            try {
                val chainResponse = repository.getEvolutionChain(chainId)

                val stages = mutableListOf<EvolutionStage>()
                collectSpecies(chainResponse.chain, stages)

                _evolutionStages.value = stages.mapIndexed { index, stage ->
                    stage.copy(
                        hasPrev = index > 0,
                        hasNext = index < stages.lastIndex
                    )
                }
            } catch (e: Exception) {
                Log.e("PokeVM", "Failed to fetch evolution chain for $chainId", e)
            }
        }
    }

    private fun collectSpecies(chain: Chain, list: MutableList<EvolutionStage>) {
        val id = extractIdFromUrl(chain.species.url)
        list.add(
            EvolutionStage(
                id = id,
                name = chain.species.name,
                imageUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png",
                hasNext = chain.evolvesTo.isNotEmpty(),
                hasPrev = chain.evolvesFrom.isNotEmpty()
            )
        )
        chain.evolvesTo.forEach { evo -> collectSpecies(evo, list) }
    }

    // -------------------------
    // Utility
    // -------------------------
    fun extractIdFromUrl(url: String): Int {
        return url.trimEnd('/')
            .split("/")
            .last()
            .toIntOrNull() ?: -1
    }
}


data class PokemonDetailUiState(
    val pokemon: PokemonResponse? = null,
    val description: String = "",
    val isLoading: Boolean = true,
    val error: UiError? = null,
    val varieties: List<PokemonVariety> = emptyList(),
    val evolutionChainId: Int? = null
)