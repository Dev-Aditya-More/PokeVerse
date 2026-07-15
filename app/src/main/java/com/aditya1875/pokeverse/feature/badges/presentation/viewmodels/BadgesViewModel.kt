package com.aditya1875.pokeverse.feature.badges.presentation.viewmodels

import androidx.lifecycle.ViewModel
import com.aditya1875.pokeverse.feature.badges.domain.BadgeRepository
import com.aditya1875.pokeverse.feature.badges.domain.GymBadge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted

class BadgesViewModel(
    private val repository: BadgeRepository
) : ViewModel() {

    val regions: List<String> = repository.getRegions()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedRegion = MutableStateFlow<String?>(null)
    val selectedRegion: StateFlow<String?> = _selectedRegion.asStateFlow()

    val badges: StateFlow<List<GymBadge>> =
        combine(_searchQuery, _selectedRegion) { query, region ->
            val q = query.trim().lowercase()
            repository.getBadges().filter { badge ->
                (region == null || badge.region == region) &&
                        (q.isEmpty() ||
                                badge.name.lowercase().contains(q) ||
                                badge.leader.lowercase().contains(q) ||
                                badge.location.lowercase().contains(q) ||
                                badge.type.lowercase().contains(q) ||
                                badge.region.lowercase().contains(q))
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.getBadges())

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }

    fun onRegionSelect(region: String?) {
        _selectedRegion.value = if (_selectedRegion.value == region) null else region
    }
}
