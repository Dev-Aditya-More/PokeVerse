package com.aditya1875.pokeverse.feature.feedback.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.feedback.data.FeedbackRepository
import com.aditya1875.pokeverse.feature.feedback.data.FeedbackWish
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val selected: Set<FeedbackWish> = emptySet(),
    val message: String = "",
    val sending: Boolean = false,
    val sent: Boolean = false,
    val failed: Boolean = false,
    val tooSoon: Boolean = false,
    val xpAwarded: Int = 0
) {
    val canSend: Boolean get() = !sending && (selected.isNotEmpty() || message.isNotBlank())
}

class FeedbackViewModel(
    private val repo: FeedbackRepository,
    private val xpManager: XPManager
) : ViewModel() {

    private val _state = MutableStateFlow(FeedbackUiState())
    val state: StateFlow<FeedbackUiState> = _state.asStateFlow()

    private val _xp = MutableSharedFlow<XPResult>(extraBufferCapacity = 2)
    val xp: SharedFlow<XPResult> = _xp.asSharedFlow()

    /** Fresh form each time the sheet opens. */
    fun reset() {
        _state.value = FeedbackUiState(tooSoon = !repo.canSubmitNow)
    }

    fun toggle(wish: FeedbackWish) = _state.update {
        it.copy(selected = if (wish in it.selected) it.selected - wish else it.selected + wish, failed = false)
    }

    fun onMessage(value: String) = _state.update {
        it.copy(message = value.take(FeedbackRepository.MAX_MESSAGE), failed = false)
    }

    fun submit() {
        val s = _state.value
        if (!s.canSend || s.tooSoon) return
        _state.update { it.copy(sending = true, failed = false) }
        viewModelScope.launch {
            val ok = repo.submit(s.selected, s.message)
            var gained = 0
            if (ok && repo.claimThanksOnce()) {
                val award = xpManager.awardGameXP(XPEvent.FeedbackThanks)
                gained = award.xpGained
                if (gained > 0) _xp.emit(award)
            }
            _state.update { it.copy(sending = false, sent = ok, failed = !ok, xpAwarded = gained) }
        }
    }
}
