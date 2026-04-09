package com.aditya1875.pokeverse.feature.pokemon.home.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.pokemon.home.domain.model.DailyTriviaState
import com.aditya1875.pokeverse.feature.pokemon.home.domain.trivia.DailyTriviaManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class TriviaUiState {
    object Idle : TriviaUiState()
    object Loading : TriviaUiState()
    data class Ready(val trivia: DailyTriviaState) : TriviaUiState()
    data class Error(val message: String) : TriviaUiState()
}

class DailyTriviaViewModel(
    private val triviaManager: DailyTriviaManager,
    private val xpManager: XPManager,
) : ViewModel() {

    private val _state = MutableStateFlow<TriviaUiState>(TriviaUiState.Idle)
    val state: StateFlow<TriviaUiState> = _state

    // FAB badge — true when today's trivia is not yet answered
    private val _showBadge = MutableStateFlow(false)
    val showBadge: StateFlow<Boolean> = _showBadge

    private val _xpResult = MutableSharedFlow<XPResult>(extraBufferCapacity = 4)
    val xpResult: SharedFlow<XPResult> = _xpResult

    init {
        checkBadge()
    }

    fun loadTrivia() {
        viewModelScope.launch {
            _state.value = TriviaUiState.Loading
            triviaManager.getDailyTrivia()
                .onSuccess { _state.value = TriviaUiState.Ready(it) }
                .onFailure { _state.value = TriviaUiState.Error(it.message ?: "Failed to load") }
        }
    }

    fun dismiss() {
        _state.value = TriviaUiState.Idle
    }

    // Called when user answers the "Who's That Pokémon?" question
    fun submitTriviaAnswer(correct: Boolean) {
        viewModelScope.launch {
            triviaManager.markAnswered(correct)
            _showBadge.value = false

            // Award XP for participating (bonus for correct)
            val xpEvent = if (correct) XPEvent.QuizAnswer(correct = true) else XPEvent.QuizAnswer(correct = false)
            val result = xpManager.awardGameXP(xpEvent)

            // Extra flat bonus just for doing the daily trivia
            val triviaBonus = if (correct) 30 else 10
            val bonusResult = xpManager.awardGameXP(XPEvent.QuizComplete(score = if (correct) 1 else 0, total = 1))

            if (result.xpGained > 0) _xpResult.emit(result)

            // Refresh state to show answered UI
            triviaManager.getDailyTrivia()
                .onSuccess { _state.value = TriviaUiState.Ready(it) }
        }
    }

    private fun checkBadge() {
        viewModelScope.launch {
            _showBadge.value = !triviaManager.isTodayAnswered()
        }
    }
}