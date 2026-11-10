package com.aditya1875.pokeverse.feature.analysis.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.analysis.data.CoachResult
import com.aditya1875.pokeverse.feature.analysis.data.TeamCoach
import com.aditya1875.pokeverse.feature.analysis.presentation.components.TeamAnalysis
import com.aditya1875.pokeverse.feature.analysis.presentation.components.TeamMemberWithTypes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CoachUiState {
    data class Idle(val remaining: Int) : CoachUiState
    data object Loading : CoachUiState
    data class Ready(val text: String, val remaining: Int, val fresh: Boolean) : CoachUiState
    data object Limit : CoachUiState
    data class Error(val remaining: Int, val network: Boolean, val detail: String?) : CoachUiState
}

class TeamCoachViewModel(private val coach: TeamCoach) : ViewModel() {

    private val _state = MutableStateFlow<CoachUiState>(CoachUiState.Idle(coach.remainingToday(premium = false)))
    val state: StateFlow<CoachUiState> = _state.asStateFlow()

    /** Shows a saved read for this team if there is one (free), otherwise the ask button. */
    fun load(team: List<TeamMemberWithTypes>, premium: Boolean = false) {
        if (_state.value is CoachUiState.Loading) return
        val saved = coach.cached(team)
        val left = coach.remainingToday(premium)
        _state.value = when {
            saved != null -> CoachUiState.Ready(saved, left, fresh = false)
            !premium && left <= 0 -> CoachUiState.Limit
            else -> CoachUiState.Idle(left)
        }
    }

    fun ask(team: List<TeamMemberWithTypes>, analysis: TeamAnalysis, allCaught: Boolean = false, premium: Boolean = false) {
        if (_state.value is CoachUiState.Loading) return
        _state.value = CoachUiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = coach.read(team, analysis, allCaught, premium)) {
                is CoachResult.Success -> CoachUiState.Ready(result.text, result.remaining, fresh = true)
                CoachResult.LimitReached -> CoachUiState.Limit
                is CoachResult.Failed -> CoachUiState.Error(coach.remainingToday(premium), result.network, result.detail)
            }
        }
    }
}
