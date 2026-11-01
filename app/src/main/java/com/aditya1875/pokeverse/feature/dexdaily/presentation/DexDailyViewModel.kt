package com.aditya1875.pokeverse.feature.dexdaily.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.dexdaily.data.DexDailyContent
import com.aditya1875.pokeverse.feature.dexdaily.data.DexDailyRepository
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyCalendar
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyRecord
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyRules
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface DexDailyLoad {
    data object Loading : DexDailyLoad
    data class Ready(val content: DexDailyContent) : DexDailyLoad
    data object Failed : DexDailyLoad
}

/** One-shot feedback for sounds/haptics/animation. */
sealed interface DexDailyFeedback {
    data object Wrong : DexDailyFeedback
    data class Finished(val solved: Boolean) : DexDailyFeedback
}

/**
 * Drives the Home card, the play sheet and the archive. Progress lives in [DexDailyRepository]
 * (DataStore), so the card, the sheet and the archive screen always agree, even across
 * separate ViewModel instances.
 */
class DexDailyViewModel(
    private val repository: DexDailyRepository,
    private val xpManager: XPManager,
) : ViewModel() {

    private val _today = MutableStateFlow(DexDailyCalendar.today())
    val today: StateFlow<String> = _today.asStateFlow()

    val records: StateFlow<Map<String, DexDailyRecord>> =
        repository.records.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val streak: StateFlow<Int> = combine(records, _today) { r, t -> DexDailyRules.streak(r, t) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _loads = MutableStateFlow<Map<String, DexDailyLoad>>(emptyMap())
    val loads: StateFlow<Map<String, DexDailyLoad>> = _loads.asStateFlow()

    /** Date whose puzzle is open in the sheet, or null. */
    private val _openDate = MutableStateFlow<String?>(null)
    val openDate: StateFlow<String?> = _openDate.asStateFlow()

    private val _xp = MutableSharedFlow<XPResult>(extraBufferCapacity = 2)
    val xp: SharedFlow<XPResult> = _xp.asSharedFlow()

    private val _feedback = MutableSharedFlow<DexDailyFeedback>(extraBufferCapacity = 4)
    val feedback: SharedFlow<DexDailyFeedback> = _feedback.asSharedFlow()

    private val guessLock = Mutex()

    init {
        load(_today.value)
    }

    /** Call on resume: after midnight the card should move on to the new puzzle. */
    fun refreshDay() {
        val now = DexDailyCalendar.today()
        if (now != _today.value) {
            _today.value = now
            load(now)
        }
    }

    fun load(date: String) {
        val current = _loads.value[date]
        if (current is DexDailyLoad.Ready || current is DexDailyLoad.Loading) return
        _loads.update { it + (date to DexDailyLoad.Loading) }
        viewModelScope.launch {
            val result = runCatching { repository.content(date) }
            _loads.update { it + (date to (result.getOrNull()?.let { c -> DexDailyLoad.Ready(c) } ?: DexDailyLoad.Failed)) }
        }
    }

    fun retry(date: String) {
        _loads.update { it - date }
        load(date)
    }

    fun open(date: String) {
        load(date)
        _openDate.value = date
    }

    fun close() {
        _openDate.value = null
    }

    /** Pro perk: reveal type, generation and first letter before the single guess. */
    fun useHints(date: String, isPremium: Boolean) {
        if (!isPremium) return
        viewModelScope.launch {
            guessLock.withLock {
                val isArchive = date != _today.value
                repository.update(date) { stored ->
                    val base = if (stored == DexDailyRecord() && isArchive) stored.copy(archive = true) else stored
                    DexDailyRules.useHints(base)
                }
            }
        }
    }

    fun guess(date: String, optionId: Int) {
        val content = (_loads.value[date] as? DexDailyLoad.Ready)?.content ?: return
        viewModelScope.launch {
            guessLock.withLock {
                // "before" is read inside the atomic update: the records flow can lag a fast double tap.
                var before = DexDailyRecord()
                // A puzzle first opened after its own day is an archive play (no XP, no streak).
                val isArchive = date != _today.value
                val after = repository.update(date) { stored ->
                    before = stored
                    val base = if (stored == DexDailyRecord() && isArchive) stored.copy(archive = true) else stored
                    DexDailyRules.guess(base, content.puzzle, optionId)
                }
                if (after == before) return@withLock
                if (!after.finished) {
                    _feedback.emit(DexDailyFeedback.Wrong)
                    return@withLock
                }
                _feedback.emit(DexDailyFeedback.Finished(after.solved))
                val xp = DexDailyRules.xpFor(after)
                if (!before.finished && xp > 0) {
                    val result = xpManager.awardGameXP(XPEvent.DexDaily(xp))
                    if (result.xpGained > 0) _xp.emit(result)
                }
            }
        }
    }
}
