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
import com.aditya1875.pokeverse.data.local.entity.FavouriteEntity
import com.aditya1875.pokeverse.data.local.entity.TeamMemberEntity
import com.aditya1875.pokeverse.data.remote.model.PokemonFilter
import com.aditya1875.pokeverse.data.remote.model.PokemonResponse
import com.aditya1875.pokeverse.data.remote.model.PokemonResult
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
        val regionRange = selectedRegion?.range
        val regionStart = selectedRegion?.offset ?: 0
        val regionEnd = regionStart + (selectedRegion?.limit ?: Int.MAX_VALUE)

        if (isNewRegion) currentOffset = regionStart

        // --- NEW: Check for connectivity ---
        if (!isNetworkAvailable(context)) {
            _uiState.value = PokemonDetailUiState(
                error = UiError.Network("No Internet Connection"),
                isLoading = false
            )
            isLoading = false
            return
        }

        // Proceed only if online
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

                // reset any previous error
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
            name = this.name,
            imageUrl = this.sprites.front_default ?: ""
        )
    }

    fun removeFromTeam(pokemon: TeamMemberEntity) = viewModelScope.launch {
        teamDao.removeFromTeam(pokemon)
    }

    fun removeFromTeamByName(name: String) = viewModelScope.launch {
        teamDao.removeFromTeamByName(name)
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
}


data class PokemonDetailUiState(
    val pokemon: PokemonResponse? = null,
    val description: String = "",
    val isLoading: Boolean = true,
    val error: UiError? = null,
    val varieties: List<PokemonVariety> = emptyList(),
    val evolutionUi: EvolutionChainUi? = null
)