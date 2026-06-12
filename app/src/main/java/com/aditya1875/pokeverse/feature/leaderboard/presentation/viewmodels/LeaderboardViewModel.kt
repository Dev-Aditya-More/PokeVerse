package com.aditya1875.pokeverse.feature.leaderboard.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.leaderboard.data.remote.model.LeaderboardEntry
import com.aditya1875.pokeverse.feature.leaderboard.data.repository.LeaderboardRepository
import com.aditya1875.pokeverse.feature.leaderboard.data.repository.LeaderboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _lastWeekEntries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val lastWeekEntries: StateFlow<List<LeaderboardEntry>> = _lastWeekEntries.asStateFlow()

    private val _lastWeekOf = MutableStateFlow(0L)
    val lastWeekOf: StateFlow<Long> = _lastWeekOf.asStateFlow()

    private val _lastWeekLoading = MutableStateFlow(false)
    val lastWeekLoading: StateFlow<Boolean> = _lastWeekLoading.asStateFlow()

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
        if (type == LeaderboardType.LAST_WEEK) {
            loadLastWeek()
        } else {
            load()
        }
    }

    private fun loadLastWeek() {
        viewModelScope.launch {
            _lastWeekLoading.value = true
            val (weekOf, entries) = repository.getLastWeekSnapshot()
            _lastWeekEntries.value = entries
            _lastWeekOf.value = weekOf
            _lastWeekLoading.value = false
        }
    }

    fun saveLastWeekSnapshot(weekOf: Long, entries: List<LeaderboardEntry>) {
        viewModelScope.launch {
            repository.saveLastWeekSnapshot(weekOf, entries)
        }
    }

    private var isPaginating = false

    fun loadNextPage() {
        if (isPaginating) return
        viewModelScope.launch {
            val current = _state.value
            if (current is LeaderboardState.Success && current.canLoadMore) {
                isPaginating = true
                try {
                    _state.value = repository.loadNextPage(_type.value)
                } finally {
                    isPaginating = false
                }
            }
        }
    }
}

enum class LeaderboardType {
    GLOBAL,
    WEEKLY,
    LAST_WEEK
}
