package com.aditya1875.pokeverse.feature.game.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Generic endless-mode state machine — lives, streak, best streak, and the
 * current round — shared by every "endless" mini-game instead of each one
 * reimplementing the same lives/streak/scoring bookkeeping.
 *
 * Deliberately has zero Compose/Android imports so it stays a plain,
 * unit-testable Kotlin class. UI layers collect [state] and call [submitAnswer]
 * / [onTimeout] / [skip].
 *
 * @param Round the per-round payload a specific game generates (e.g. `SurvivorRound`)
 * @param Answer whatever shape that game's UI submits back (e.g. a type name String)
 */
class EndlessRoundLoop<Round, Answer>(
    private val scope: CoroutineScope,
    private val maxLives: Int = 3,
    private val generateRound: suspend (streak: Int) -> Round?,
    private val isCorrect: (Round, Answer) -> Boolean
) {
    private val _state = MutableStateFlow(EndlessLoopState<Round>(lives = maxLives))
    val state: StateFlow<EndlessLoopState<Round>> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun start() = advance()

    fun submitAnswer(answer: Answer) {
        val round = _state.value.currentRound ?: return
        if (_state.value.status != LoopStatus.Playing) return

        if (isCorrect(round, answer)) {
            val streak = _state.value.streak + 1
            _state.update {
                it.copy(streak = streak, bestStreak = maxOf(it.bestStreak, streak))
            }
            advance()
        } else {
            loseLife()
        }
    }

    fun onTimeout() = loseLife()

    /** Regenerates the current round without touching lives/streak — the existing skip perk. */
    fun skip() = advance()

    /** Recovers from [LoopStatus.GameOver] with [lives] hearts and resumes play — the revive perk. */
    fun revive(lives: Int = 1) {
        if (_state.value.status != LoopStatus.GameOver) return
        _state.update { it.copy(lives = lives, status = LoopStatus.Loading) }
        advance()
    }

    private fun loseLife() {
        val livesLeft = _state.value.lives - 1
        if (livesLeft <= 0) {
            _state.update { it.copy(lives = 0, status = LoopStatus.GameOver, currentRound = null) }
        } else {
            _state.update { it.copy(lives = livesLeft, streak = 0) }
            advance()
        }
    }

    private fun advance() {
        loadJob?.cancel()
        _state.update { it.copy(status = LoopStatus.Loading) }
        loadJob = scope.launch {
            val round = generateRound(_state.value.streak)
            _state.update {
                if (round != null) it.copy(status = LoopStatus.Playing, currentRound = round)
                else it.copy(status = LoopStatus.GameOver, currentRound = null)
            }
        }
    }
}

data class EndlessLoopState<Round>(
    val status: LoopStatus = LoopStatus.Loading,
    val currentRound: Round? = null,
    val lives: Int = 3,
    val streak: Int = 0,
    val bestStreak: Int = 0
)

enum class LoopStatus { Loading, Playing, GameOver }
