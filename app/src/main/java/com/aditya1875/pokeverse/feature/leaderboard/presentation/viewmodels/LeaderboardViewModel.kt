package com.aditya1875.pokeverse.feature.leaderboard.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.leaderboard.data.repository.LeaderboardRepository
import com.aditya1875.pokeverse.feature.leaderboard.data.repository.LeaderboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val repository: LeaderboardRepository
) : ViewModel() {

    private val _type = MutableStateFlow(LeaderboardType.GLOBAL)
    val type: StateFlow<LeaderboardType> = _type

    private val _state = MutableStateFlow<LeaderboardState>(LeaderboardState.Loading)
    val state: StateFlow<LeaderboardState> = _state

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            if (_state.value !is LeaderboardState.Success) {
                _state.value = LeaderboardState.Loading
            }
            _state.value = repository.getLeaderboard(type = _type.value)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                _state.value = repository.getLeaderboard(
                    type = _type.value,
                    forceRefresh = true
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun switchType(type: LeaderboardType) {
        if (_type.value == type) return
        _type.value = type
        load()
    }

    fun loadNextPage() {
        viewModelScope.launch {
            val current = _state.value
            if (current is LeaderboardState.Success && current.canLoadMore) {
                _state.value = repository.loadNextPage(_type.value)
            }
        }
    }
}

enum class LeaderboardType {
    GLOBAL,
    WEEKLY
}