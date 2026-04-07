package com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels

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
import com.aditya1875.pokeverse.utils.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

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

    // FIX: Track hasError as a StateFlow so screenState reacts to it correctly
    private val _hasError = MutableStateFlow(false)

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

    // FIX: screenState now includes _hasError and endReached awareness.
    //
    // THE BUG THAT CAUSED THE GLITCH:
    // The old logic was:
    //   isLoading && list.isEmpty() -> LOADING
    //   error != null && list.isEmpty() -> ERROR
    //   else -> CONTENT
    //
    // The problem: when loadPokemonList() is first called, there is a brief
    // moment where isLoading=false AND list=[] AND error=null — before
    // _isLoading.value = true is set inside the coroutine.
    // This caused the state machine to emit CONTENT with an empty list,
    // making the loading indicator disappear and reappear (the glitch).
    //
    // THE FIX: An empty list with no confirmed error should always resolve
    // to LOADING, not CONTENT. Only show CONTENT when the list is non-empty.
    // Only show ERROR when the error is confirmed AND the list is empty.
    val screenState: StateFlow<ScreenState> = combine(
        _isLoading,
        _uiState,
        _pokemonList,
        _hasError
    ) { isLoading, uiState, list, hasError ->
        when {
            // Show error screen only when we've confirmed an error with nothing to show
            hasError && list.isEmpty() -> ScreenState.ERROR

            // Show content as soon as we have something to display
            list.isNotEmpty() -> ScreenState.CONTENT

            // Everything else (including the brief isLoading=false moment before
            // the coroutine starts, and the normal loading state) stays as LOADING
            else -> ScreenState.LOADING
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenState.LOADING)

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

                // FIX: Clear error state together so there's no frame where
                // _hasError=false but _uiState.error is still set (or vice versa)
                _hasError.value = false
                _uiState.update { it.copy(error = null) }
                listError = null

            } catch (e: IOException) {
                // FIX: Set _hasError flow (not a plain var) so screenState reacts
                _hasError.value = true
                _uiState.update { it.copy(error = UiError.Network(e.message)) }
            } catch (e: Exception) {
                _hasError.value = true
                _uiState.update { it.copy(error = UiError.Unexpected(e.message)) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retry() {
        // FIX: Reset error state atomically before triggering load,
        // so screenState goes LOADING immediately rather than staying on ERROR
        _hasError.value = false
        listError = null
        _uiState.update { it.copy(error = null) }
        loadPokemonList()
    }

    // -------------------------
    // Filters
    // -------------------------

    fun setRegionFilter(region: Region?) {
        // FIX: Reset error state when changing region
        _hasError.value = false
        _filters.update { it.copy(selectedRegion = region, selectedType = null) }

        currentOffset = region?.offset ?: 0
        endReached = false
        allPokemonInRegion = emptyList()
        _pokemonList.value = emptyList()

        loadPokemonList(isNewRegion = true)
    }

    fun setTypeFilter(type: PokemonType?) {
        _filters.update { it.copy(selectedType = type) }
        if (type == null) { _pokemonList.value = allPokemonInRegion; return }
        val cached = typePokemonCache[type]
        if (cached != null) { applyTypeFilterFromCache(type, cached); return }

        viewModelScope.launch {
            _isTypeFiltering.value = true
            val capturedRegion = _filters.value.selectedRegion
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
                        capturedRegion == null || capturedRegion.range.contains(id)
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
        // FIX: Use _hasError.value instead of the old plain `hasError` var
        if (_hasError.value || endReached || _isLoading.value) return false
        val selectedType = _filters.value.selectedType ?: return false
        val currentSize = _pokemonList.value.size
        return currentSize == 0
    }

    // -------------------------
    // Refresh
    // -------------------------

    fun refreshList() {
        // FIX: Reset error state atomically before triggering refresh
        _hasError.value = false
        currentOffset = _filters.value.selectedRegion?.offset ?: 0
        endReached = false
        allPokemonInRegion = emptyList()
        _pokemonList.value = emptyList()
        _uiState.update { it.copy(error = null) }
        loadPokemonList(isNewRegion = true)
    }

    // -------------------------
    // Utils (TEMP — will move later)
    // -------------------------
    private fun extractIdFromUrl(url: String): Int {
        return url.trimEnd('/')
            .split("/")
            .lastOrNull()
            ?.toIntOrNull() ?: -1
    }
}

enum class ScreenState {
    LOADING,
    ERROR,
    CONTENT
}