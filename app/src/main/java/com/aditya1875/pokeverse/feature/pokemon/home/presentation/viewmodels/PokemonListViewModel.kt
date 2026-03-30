package com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonFilter
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonResult
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonType
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.Region
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetPokemonByTypeUseCase
import com.aditya1875.pokeverse.feature.pokemon.home.domain.usecase.GetPokemonListUseCase
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.viewmodels.PokemonDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PokemonListViewModel(
    private val getPokemonListUseCase: GetPokemonListUseCase,
    private val getPokemonByTypeUseCase: GetPokemonByTypeUseCase,
) : ViewModel() {

    // -------------------------
    // State
    // -------------------------

    private val _pokemonList = MutableStateFlow<List<PokemonResult>>(emptyList())
    val pokemonList: StateFlow<List<PokemonResult>> = _pokemonList

    private val _uiState = MutableStateFlow(PokemonDetailUiState())
    val uiState: StateFlow<PokemonDetailUiState> = _uiState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isTypeFiltering = MutableStateFlow(false)
    val isTypeFiltering: StateFlow<Boolean> = _isTypeFiltering

    private val _filters = MutableStateFlow(PokemonFilter())
    val filters: StateFlow<PokemonFilter> = _filters

    var endReached by mutableStateOf(false)
        private set

    var listError by mutableStateOf<String?>(null)
        private set

    // -------------------------
    // Pagination
    // -------------------------

    private var currentOffset = 0
    private val limit = 20

    // -------------------------
    // Caching
    // -------------------------

    private val typePokemonCache = mutableMapOf<PokemonType, Set<Int>>()
    private var allPokemonInRegion: List<PokemonResult> = emptyList()

    // -------------------------
    // Load List
    // -------------------------

    fun loadPokemonList(isNewRegion: Boolean = false) {
        if (_isLoading.value || (endReached && !isNewRegion)) return

        val selectedRegion = _filters.value.selectedRegion
        val regionRange = selectedRegion?.range
        val regionStart = selectedRegion?.offset ?: 0
        val regionEnd = regionStart + (selectedRegion?.limit ?: Int.MAX_VALUE)

        if (isNewRegion) {
            currentOffset = regionStart
            allPokemonInRegion = emptyList()
            endReached = false
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = getPokemonListUseCase(limit, currentOffset)

                val newList = result.results
                    .map { it to extractIdFromUrl(it.url) }
                    .filter { (_, id) -> regionRange?.contains(id) ?: true }
                    .map { it.first }

                allPokemonInRegion = allPokemonInRegion + newList
                currentOffset += limit

                if (newList.isEmpty() || newList.size < limit || currentOffset >= regionEnd) {
                    endReached = true
                }

                applyCurrentFilters()

                listError = null

            } catch (e: Exception) {
                listError = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // -------------------------
    // Filters
    // -------------------------

    fun setRegionFilter(region: Region?) {
        _filters.update { it.copy(selectedRegion = region, selectedType = null) }

        currentOffset = region?.offset ?: 0
        endReached = false
        allPokemonInRegion = emptyList()
        _pokemonList.value = emptyList()

        loadPokemonList(isNewRegion = true)
    }

    fun setTypeFilter(type: PokemonType?) {
        _filters.update { it.copy(selectedType = type) }

        if (type == null) {
            _pokemonList.value = allPokemonInRegion
            return
        }

        val cached = typePokemonCache[type]
        if (cached != null) {
            applyTypeFilterFromCache(type, cached)
            return
        }

        viewModelScope.launch {
            _isTypeFiltering.value = true

            try {
                val typeResponse = getPokemonByTypeUseCase(type.name.lowercase())

                val pokemonIds = typeResponse.pokemon
                    .map { extractIdFromUrl(it.pokemon.url) }
                    .toSet()

                typePokemonCache[type] = pokemonIds

                val filtered = typeResponse.pokemon
                    .map { it.pokemon }
                    .filter { pokemon ->
                        val id = extractIdFromUrl(pokemon.url)
                        val region = _filters.value.selectedRegion
                        region == null || region.range.contains(id)
                    }

                _pokemonList.value = filtered

            } catch (e: Exception) {
                val fallback = typePokemonCache[type]
                if (fallback != null) applyTypeFilterFromCache(type, fallback)
            } finally {
                _isTypeFiltering.value = false
            }
        }
    }

    private fun applyCurrentFilters() {
        val selectedType = _filters.value.selectedType

        _pokemonList.value = if (selectedType == null) {
            allPokemonInRegion
        } else {
            val typeIds = typePokemonCache[selectedType] ?: emptySet()
            allPokemonInRegion.filter {
                val id = extractIdFromUrl(it.url)
                typeIds.contains(id)
            }
        }

        if (shouldAutoLoadMoreForFilter()) {
            loadPokemonList()
        }
    }

    private fun applyTypeFilterFromCache(type: PokemonType, typeIds: Set<Int>) {
        val region = _filters.value.selectedRegion

        _pokemonList.value = allPokemonInRegion.filter { pokemon ->
            val id = extractIdFromUrl(pokemon.url)
            typeIds.contains(id) &&
                    (region == null || region.range.contains(id))
        }
    }

    private fun shouldAutoLoadMoreForFilter(): Boolean {
        val selectedType = _filters.value.selectedType ?: return false
        return _pokemonList.value.size < limit && !endReached && !_isLoading.value
    }

    // -------------------------
    // Refresh
    // -------------------------

    fun refreshList() {
        currentOffset = _filters.value.selectedRegion?.offset ?: 0
        endReached = false
        allPokemonInRegion = emptyList()
        _pokemonList.value = emptyList()
        loadPokemonList(isNewRegion = true)
    }

    // -------------------------
    // Utils (TEMP — will move later)
    // -------------------------

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun extractIdFromUrl(url: String): Int {
        return url.trimEnd('/')
            .split("/")
            .lastOrNull()
            ?.toIntOrNull() ?: -1
    }
}