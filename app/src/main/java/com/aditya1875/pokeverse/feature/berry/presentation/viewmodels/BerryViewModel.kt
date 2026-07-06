package com.aditya1875.pokeverse.feature.berry.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.berry.data.repository.BerryRepository
import com.aditya1875.pokeverse.feature.berry.data.source.remote.model.BerryUiModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class BerryListState {
    object Loading : BerryListState()
    data class Success(val berries: List<BerryUiModel>) : BerryListState()
    data class Error(val message: String) : BerryListState()
}

sealed class BerryDetailState {
    object Idle : BerryDetailState()
    object Loading : BerryDetailState()
    data class Success(val berry: BerryUiModel) : BerryDetailState()
    object NotFound : BerryDetailState()
}

@OptIn(FlowPreview::class)
class BerryViewModel(private val repository: BerryRepository) : ViewModel() {

    private val _listState = MutableStateFlow<BerryListState>(BerryListState.Loading)
    val listState: StateFlow<BerryListState> = _listState

    private val _detailState = MutableStateFlow<BerryDetailState>(BerryDetailState.Idle)
    val detailState: StateFlow<BerryDetailState> = _detailState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredBerries: StateFlow<List<BerryUiModel>> = _searchQuery
        .debounce(300)
        .combine(_listState) { query, state ->
            if (state is BerryListState.Success) repository.searchBerries(query)
            else emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { loadBerries() }

    fun loadBerries() {
        viewModelScope.launch {
            _listState.value = BerryListState.Loading
            repository.getBerries()
                .onSuccess { _listState.value = BerryListState.Success(it) }
                .onFailure { _listState.value = BerryListState.Error(it.message ?: "Failed to load berries") }
        }
    }

    fun onSearchChange(query: String) { _searchQuery.value = query }

    fun loadBerryDetail(name: String) {
        viewModelScope.launch {
            _detailState.value = BerryDetailState.Loading
            // If list is still loading, wait for it
            if (_listState.value is BerryListState.Loading) {
                repository.getBerries().onSuccess { }
            }
            val berry = repository.getBerryByName(name)
            _detailState.value = if (berry != null) BerryDetailState.Success(berry)
                                  else BerryDetailState.NotFound
        }
    }

    fun clearDetail() { _detailState.value = BerryDetailState.Idle }
}
