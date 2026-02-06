package com.aditya1875.pokeverse.presentation.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.data.local.dao.FavouritesDao
import com.aditya1875.pokeverse.data.local.dao.TeamDao
import com.aditya1875.pokeverse.data.local.dao.TeamWithMembers
import com.aditya1875.pokeverse.data.local.entity.FavouriteEntity
import com.aditya1875.pokeverse.data.local.entity.TeamEntity
import com.aditya1875.pokeverse.data.local.entity.TeamMemberEntity
import com.aditya1875.pokeverse.data.remote.model.PokemonFilter
import com.aditya1875.pokeverse.data.remote.model.PokemonResponse
import com.aditya1875.pokeverse.data.remote.model.PokemonResult
import com.aditya1875.pokeverse.data.remote.model.PokemonType
import com.aditya1875.pokeverse.data.remote.model.PokemonVariety
import com.aditya1875.pokeverse.data.remote.model.Region
import com.aditya1875.pokeverse.data.remote.model.evolutionModels.EvolutionChainUi
import com.aditya1875.pokeverse.domain.repository.DescriptionRepo
import com.aditya1875.pokeverse.domain.repository.PokemonRepo
import com.aditya1875.pokeverse.domain.repository.PokemonSearchRepository
import com.aditya1875.pokeverse.utils.EvolutionChainMapper
import com.aditya1875.pokeverse.utils.SearchUiState
import com.aditya1875.pokeverse.utils.UiError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import kotlin.collections.filter

class PokemonViewModel(
    private val context: Context,
    internal val repository: PokemonRepo,
    private val teamDao: TeamDao,
    private val favouritesDao: FavouritesDao,
    private val descriptionLocalRepository: DescriptionRepo,
    private val searchRepository: PokemonSearchRepository
) : ViewModel() {

    private val _showTagline = MutableStateFlow(false)
    val showTagline: StateFlow<Boolean> = _showTagline
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val pokemonTypeCache = mutableMapOf<String, List<String>>()

    private val _pokemonList = MutableStateFlow<List<PokemonResult>>(emptyList())
    val pokemonList: StateFlow<List<PokemonResult>> = _pokemonList
    private val _uiState = MutableStateFlow(PokemonDetailUiState())
    val uiState: StateFlow<PokemonDetailUiState> = _uiState

    private val _searchUiState = MutableStateFlow(SearchUiState())

    private val _searchQuery = MutableStateFlow("")
    @OptIn(FlowPreview::class)
    val debouncedSearchQuery = _searchQuery
        .debounce(300) // 300ms delay
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchUiState: StateFlow<SearchUiState> = _searchQuery
        .debounce(300)
        .onEach { _isSearching.value = it.isNotBlank() }
        .flatMapLatest { query ->
            flow {
                val cleaned = query.trim().lowercase()
                if (cleaned.isEmpty()) {
                    emit(SearchUiState(query = cleaned))
                } else {
                    try {
                        val results = searchRepository.searchPokemon(cleaned, limit = 10)
                        emit(SearchUiState(
                            query = cleaned,
                            suggestions = results,
                            showSuggestions = results.isNotEmpty(),
                            isLoading = false
                        ))
                    } catch (e: Exception) {
                        Log.e("PokeVM", "Search failed", e)
                        emit(SearchUiState(
                            query = cleaned,
                            error = "Search failed"
                        ))
                    }
                }
                _isSearching.value = false
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SearchUiState()
        )

    var isLoading by mutableStateOf(false)
    var endReached by mutableStateOf(false)

    var listError by mutableStateOf<String?>(null)
        private set

    val favorites: StateFlow<List<FavouriteEntity>> = favouritesDao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

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
        val selectedType = filters.value.selectedType
        val regionRange = selectedRegion?.range
        val regionStart = selectedRegion?.offset ?: 0
        val regionEnd = regionStart + (selectedRegion?.limit ?: Int.MAX_VALUE)

        if (isNewRegion) currentOffset = regionStart

        if (!isNetworkAvailable(context)) {
            _uiState.value = PokemonDetailUiState(
                error = UiError.Network("No Internet Connection"),
                isLoading = false
            )
            isLoading = false
            return
        }

        isLoading = true
        viewModelScope.launch {
            try {
                val result = repository.getPokemonList(limit = limit, offset = currentOffset)
                var newList = result.results

                // Filter by region first
                newList = newList
                    .map { it to extractIdFromUrl(it.url) }
                    .filter { (_, id) -> regionRange?.contains(id) ?: true }
                    .map { it.first }

                // Filter by type if selected
                if (selectedType != null) {
                    newList = newList.filter { pokemonResult ->
                        try {
                            val pokemon = repository.getPokemonByName(pokemonResult.name)
                            pokemon.types.any {
                                it.type.name.equals(selectedType.name, ignoreCase = true)
                            }
                        } catch (e: Exception) {
                            false // Skip if we can't fetch details
                        }
                    }
                }

                if (selectedType != null) {
                    newList = newList.filter { pokemonResult ->
                        val cachedTypes = pokemonTypeCache[pokemonResult.name]
                        cachedTypes?.any { it.equals(selectedType.name, ignoreCase = true) }
                            ?: try {
                                val pokemon = repository.getPokemonByName(pokemonResult.name)
                                val types = pokemon.types.map { it.type.name }
                                pokemonTypeCache[pokemonResult.name] = types
                                types.any { it.equals(selectedType.name, ignoreCase = true) }
                            } catch (e: Exception) {
                                false
                            }
                    }
                }

                if (isNewRegion) {
                    _pokemonList.value = newList
                } else {
                    _pokemonList.value += newList
                }

                currentOffset += limit
                if (newList.isEmpty() || newList.size < limit || currentOffset >= regionEnd) {
                    endReached = true
                }

                _uiState.value = PokemonDetailUiState(isLoading = false, error = null)

            } catch (e: UnknownHostException) {
                _uiState.value = PokemonDetailUiState(error = UiError.Network(e.localizedMessage))
            } catch (e: Exception) {
                Log.e("PokeVM", "Failed to load Pokemon list", e)
                _uiState.value = PokemonDetailUiState(error = UiError.Unexpected(e.localizedMessage))
            } finally {
                isLoading = false
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
            val pokemon = repository.getPokemonByName(name).copy()

            var description = "Description not available."
            var varieties: List<PokemonVariety> = _uiState.value.varieties

            if (includeSpecies) {
                val species = repository.getPokemonSpeciesByName(name).copy()

                description = species.flavorTextEntries.firstOrNull {
                    it.language.name == "en"
                }?.flavorText
                    ?.replace("\n", " ")
                    ?.replace("\u000c", " ")
                    ?: description

                varieties = species.varieties

                fetchEvolutionChain(name)
            }

            _uiState.update {
                it.copy(
                    pokemon = pokemon,
                    description = description,
                    varieties = varieties,
                    isLoading = false,
                    error = null
                )
            }
            Log.d(
                "EvolutionDebug",
                "FINAL evolutionUi = ${_uiState.value.evolutionUi != null}"
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
            teamId = "",
            name = this.name,
            imageUrl = this.sprites.front_default ?: ""
        )
    }

    // -------------------------
    // Filters
    // -------------------------
    fun setRegionFilter(region: Region?) {
        _filters.update { it.copy(selectedRegion = region) }

        currentOffset = region?.offset ?: 0
        endReached = false
        isLoading = false
        _pokemonList.value = emptyList()

        loadPokemonList(isNewRegion = true)
    }

    fun setTypeFilter(type: PokemonType?) {
        _filters.update { it.copy(selectedType = type) }

        // Reset and reload with new filter
        currentOffset = _filters.value.selectedRegion?.offset ?: 0
        endReached = false
        isLoading = false
        _pokemonList.value = emptyList()

        loadPokemonList(isNewRegion = true)
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun fetchEvolutionChain(pokemonName: String) {
        try {
            val species = repository.getPokemonSpeciesByName(pokemonName)
            val chainId = extractIdFromUrl(species.evolutionChain?.url ?: "")

            val evolutionChain = repository.getEvolutionChain(chainId)

            val linear = EvolutionChainMapper.extractLinearChain(evolutionChain.chain)
            val uiChain = EvolutionChainMapper.toUiChain(linear, pokemonName)

            Log.d("EvolutionDebug", "Fetching evolution for $pokemonName")
            Log.d("EvolutionDebug", "Linear chain = ${linear.joinToString { it.name }}")
            Log.d(
                "EvolutionDebug",
                "Matched index for $pokemonName = ${
                    linear.indexOfFirst { it.name.equals(pokemonName, ignoreCase = true) }
                }"
            )

            _uiState.update {
                it.copy(evolutionUi = uiChain)
            }


        } catch (e: Exception) {
            Log.e("PokeVM", "Failed to load evolution chain", e)
            // silently fail — evolution is non-critical UI
        }
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

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addToFavorites(pokemonResult: PokemonResult) = viewModelScope.launch {
        val pokemonResponse = repository.getPokemonByName(pokemonResult.name)
        val entity = FavouriteEntity(
            name = pokemonResponse.name,
            imageUrl = pokemonResponse.sprites.front_default ?: ""
        )
        favouritesDao.addToFavorites(entity)
    }

    fun removeFromFavorites(favorite: FavouriteEntity) = viewModelScope.launch {
        favouritesDao.removeFromFavorites(favorite)
    }

    fun removeFromFavoritesByName(name: String) = viewModelScope.launch {
        favouritesDao.removeFromFavoritesByName(name)
    }

    fun isInFavorites(name: String): Flow<Boolean> = favouritesDao.isInFavorites(name)

    private val _selectedTeamId = MutableStateFlow<String?>(null)
    val selectedTeamId: StateFlow<String?> = _selectedTeamId

    val allTeams: StateFlow<List<TeamEntity>> = teamDao.getAllTeams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTeamsWithMembers: StateFlow<List<TeamWithMembers>> = teamDao.getAllTeamsWithMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current selected team (defaults to first team if none selected)
    val currentTeam: StateFlow<TeamEntity?> = combine(
        allTeams,
        selectedTeamId
    ) { teams, selectedId ->
        teams.firstOrNull { it.teamId == selectedId } ?: teams.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Members of current team
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTeamMembers: StateFlow<List<TeamMemberEntity>> = currentTeam
        .flatMapLatest { team ->
            if (team != null) {
                teamDao.getTeamMembers(team.teamId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initialize selected team to default or first team
        viewModelScope.launch {
            val defaultTeam = teamDao.getDefaultTeam()
            _selectedTeamId.value = defaultTeam?.teamId ?: allTeams.value.firstOrNull()?.teamId
        }
    }

    // ========== TEAM MANAGEMENT ==========

    fun selectTeam(teamId: String) {
        _selectedTeamId.value = teamId
    }

    fun createTeam(teamName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (teamDao.isTeamNameExists(teamName)) {
                    onError("A team with this name already exists!")
                    return@launch
                }

                val newTeam = TeamEntity(
                    teamName = teamName.trim(),
                    isDefault = false
                )

                teamDao.createTeam(newTeam)
                _selectedTeamId.value = newTeam.teamId
                onSuccess()
            } catch (e: Exception) {
                Log.e("PokeVM", "Failed to create team", e)
                onError("Failed to create team: ${e.localizedMessage}")
            }
        }
    }

    fun updateTeamName(teamId: String, newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val team = teamDao.getTeamByIdOnce(teamId)
                if (team == null) {
                    onError("Team not found")
                    return@launch
                }

                // Check if new name already exists (excluding current team)
                if (team.teamName.lowercase() != newName.lowercase() &&
                    teamDao.isTeamNameExists(newName)) {
                    onError("A team with this name already exists!")
                    return@launch
                }

                teamDao.updateTeam(team.copy(teamName = newName.trim()))
                onSuccess()
            } catch (e: Exception) {
                Log.e("PokeVM", "Failed to update team name", e)
                onError("Failed to update team name")
            }
        }
    }

    fun deleteTeam(teamId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val team = teamDao.getTeamByIdOnce(teamId)
                if (team == null) {
                    onError("Team not found")
                    return@launch
                }

                if (team.isDefault) {
                    onError("Cannot delete default team")
                    return@launch
                }

                teamDao.deleteTeam(team)

                // Select another team after deletion
                val remainingTeams = allTeams.value.filter { it.teamId != teamId }
                _selectedTeamId.value = remainingTeams.firstOrNull()?.teamId

                onSuccess()
            } catch (e: Exception) {
                Log.e("PokeVM", "Failed to delete team", e)
                onError("Failed to delete team")
            }
        }
    }

    // ========== SMART POKEMON ADDITION ==========

    /**
     * Smartly adds a Pokemon to a team.
     * - If only one team exists, add to that team
     * - If multiple teams exist, find first team with space
     * - If all teams are full, show error
     */
    fun addToTeamSmart(
        pokemonResult: PokemonResult,
        onSuccess: (teamName: String) -> Unit,
        onTeamFull: () -> Unit,
        onMultipleTeamsAvailable: (List<TeamEntity>) -> Unit
    ) = viewModelScope.launch {
        try {
            val pokemonResponse = repository.getPokemonByName(pokemonResult.name)
            val teams = allTeams.value

            when {
                teams.isEmpty() -> {
                    // No teams exist - create default
                    val defaultTeam = TeamEntity(teamName = "My Team", isDefault = true)
                    teamDao.createTeam(defaultTeam)
                    addPokemonToTeam(pokemonResponse, defaultTeam.teamId)
                    onSuccess(defaultTeam.teamName)
                }

                teams.size == 1 -> {
                    // Only one team - add to it or show full message
                    val team = teams.first()
                    val memberCount = teamDao.getTeamMemberCountOnce(team.teamId)

                    if (memberCount >= 6) {
                        onTeamFull()
                    } else {
                        addPokemonToTeam(pokemonResponse, team.teamId)
                        onSuccess(team.teamName)
                    }
                }

                else -> {
                    // Multiple teams - find first available
                    val availableTeamId = teamDao.getFirstAvailableTeamId()

                    if (availableTeamId != null) {
                        val team = teamDao.getTeamByIdOnce(availableTeamId)
                        addPokemonToTeam(pokemonResponse, availableTeamId)
                        onSuccess(team?.teamName ?: "Team")
                    } else {
                        // All teams full
                        onTeamFull()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PokeVM", "Failed to add Pokemon to team", e)
        }
    }

    private suspend fun addPokemonToTeam(pokemonResponse: PokemonResponse, teamId: String) {
        val entity = TeamMemberEntity(
            teamId = teamId,
            name = pokemonResponse.name,
            imageUrl = pokemonResponse.sprites.front_default ?: ""
        )
        teamDao.addToTeam(entity)
    }

    fun addToSpecificTeam(pokemonResult: PokemonResult, teamId: String) = viewModelScope.launch {
        val pokemonResponse = repository.getPokemonByName(pokemonResult.name)
        addPokemonToTeam(pokemonResponse, teamId)
    }

    fun removeFromTeam(pokemon: TeamMemberEntity) = viewModelScope.launch {
        teamDao.removeFromTeam(pokemon)
    }

    fun removeFromTeamByName(name: String, teamId: String) = viewModelScope.launch {
        teamDao.removeFromTeamByName(name, teamId)
    }

    fun isInTeam(name: String, teamId: String): Flow<Boolean> = teamDao.isInTeam(name, teamId)

    fun isInAnyTeam(name: String): Flow<Boolean> = allTeamsWithMembers
        .map { teams ->
            teams.any { team ->
                team.members.any { it.name.equals(name, ignoreCase = true) }
            }
        }

    @Deprecated("Use currentTeamMembers instead")
    val team: StateFlow<List<TeamMemberEntity>> = currentTeamMembers

    sealed class TeamAdditionResult {
        data class Success(val teamName: String, val wasAdded: Boolean) : TeamAdditionResult() // Add wasAdded flag
        object TeamFull : TeamAdditionResult()
        object AlreadyInTeam : TeamAdditionResult()
        data class Error(val message: String) : TeamAdditionResult()
    }

    fun togglePokemonInTeam(
        pokemonResult: PokemonResult,
        teamId: String,
        onResult: (TeamAdditionResult) -> Unit
    ) = viewModelScope.launch {
        try {
            val team = teamDao.getTeamByIdOnce(teamId)
            if (team == null) {
                onResult(TeamAdditionResult.Error("Team not found"))
                return@launch
            }

            val members = teamDao.getTeamMembers(teamId).first()
            val isInTeam = members.any { it.name.equals(pokemonResult.name, ignoreCase = true) }

            if (isInTeam) {
                // Remove from team
                teamDao.removeFromTeamByName(pokemonResult.name, teamId)
                onResult(TeamAdditionResult.Success(team.teamName, wasAdded = false)) // FIXED
            } else {
                // Check if team is full
                if (members.size >= 6) {
                    onResult(TeamAdditionResult.TeamFull)
                    return@launch
                }

                // Add to team
                val pokemonResponse = repository.getPokemonByName(pokemonResult.name)
                val entity = TeamMemberEntity(
                    teamId = teamId,
                    name = pokemonResponse.name,
                    imageUrl = pokemonResponse.sprites.front_default ?: ""
                )
                teamDao.addToTeam(entity)
                onResult(TeamAdditionResult.Success(team.teamName, wasAdded = true)) // FIXED
            }
        } catch (e: Exception) {
            Log.e("PokeVM", "Failed to toggle Pokemon in team", e)
            onResult(TeamAdditionResult.Error(e.localizedMessage ?: "Unknown error"))
        }
    }

    // Check if Pokemon is in specific team
    fun isInSpecificTeam(pokemonName: String, teamId: String): Flow<Boolean> {
        return teamDao.isInTeam(pokemonName, teamId)
    }

    fun getTeamsForPokemon(pokemonName: String): Flow<List<TeamEntity>> {
        return allTeamsWithMembers.map { teams ->
            teams.filter { teamWithMembers ->
                teamWithMembers.members.any {
                    it.name.equals(pokemonName, ignoreCase = true)
                }
            }.map { it.team }
        }
    }
}


data class PokemonDetailUiState(
    val pokemon: PokemonResponse? = null,
    val description: String = "",
    val isLoading: Boolean = true,
    val error: UiError? = null,
    val varieties: List<PokemonVariety> = emptyList(),
    val evolutionUi: EvolutionChainUi? = null
)