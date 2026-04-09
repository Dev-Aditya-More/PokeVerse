package com.aditya1875.pokeverse.feature.item.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.item.data.source.remote.model.itemModels.ItemUiModel
import com.aditya1875.pokeverse.feature.item.data.repository.ItemRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── List screen state ─────────────────────────────────────────────────────────
sealed class ItemListState {
    object Loading : ItemListState()
    data class Success(
        val items: List<ItemUiModel>,
        val canLoadMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : ItemListState()

    data class Error(val message: String) : ItemListState()
}

// ── Detail screen state ───────────────────────────────────────────────────────
sealed class ItemDetailState {
    object Idle : ItemDetailState()
    object Loading : ItemDetailState()
    data class Success(val item: ItemUiModel) : ItemDetailState()
    data class Error(val message: String) : ItemDetailState()
}

@OptIn(FlowPreview::class)
class ItemViewModel(
    private val repository: ItemRepository
) : ViewModel() {

    private val _listState = MutableStateFlow<ItemListState>(ItemListState.Loading)
    val listState: StateFlow<ItemListState> = _listState

    private val _detailState = MutableStateFlow<ItemDetailState>(ItemDetailState.Idle)
    val detailState: StateFlow<ItemDetailState> = _detailState

    // Search query with debounce
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredItems: StateFlow<List<ItemUiModel>> = _searchQuery
        .debounce(300)
        .combine(_listState) { query, state ->
            if (state is ItemListState.Success) {
                repository.searchItems(query)
            } else emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            _listState.value = ItemListState.Loading
            when (val result = repository.getItems()) {
                is ItemRepository.ItemListResult.Success ->
                    _listState.value = ItemListState.Success(result.items, result.canLoadMore)
                is ItemRepository.ItemListResult.Error ->
                    _listState.value = ItemListState.Error(result.message)
            }
        }
    }

    fun loadMore() {
        val current = _listState.value as? ItemListState.Success ?: return
        if (!current.canLoadMore || current.isLoadingMore) return

        viewModelScope.launch {
            _listState.value = current.copy(isLoadingMore = true)
            when (val result = repository.loadMore()) {
                is ItemRepository.ItemListResult.Success ->
                    _listState.value = ItemListState.Success(result.items, result.canLoadMore)
                is ItemRepository.ItemListResult.Error ->
                    _listState.value = current.copy(isLoadingMore = false)
            }
        }
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }

    fun loadItemDetail(nameOrId: String) {
        viewModelScope.launch {
            _detailState.value = ItemDetailState.Loading
            when (val result = repository.getItemDetail(nameOrId)) {
                is ItemRepository.ItemDetailResult.Success ->
                    _detailState.value = ItemDetailState.Success(result.item)
                is ItemRepository.ItemDetailResult.Error ->
                    _detailState.value = ItemDetailState.Error(result.message)
            }
        }
    }

    fun clearDetail() {
        _detailState.value = ItemDetailState.Idle
    }
}