package com.aditya1875.pokeverse.feature.game.survivor.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.core.data.local.dao.GameScoreDao
import com.aditya1875.pokeverse.feature.game.core.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.feature.game.core.domain.EndlessLoopState
import com.aditya1875.pokeverse.feature.game.core.domain.EndlessRoundLoop
import com.aditya1875.pokeverse.feature.game.core.domain.LoopStatus
import com.aditya1875.pokeverse.feature.game.survivor.domain.engine.SurvivorRoundGenerator
import com.aditya1875.pokeverse.feature.game.survivor.domain.model.DifficultyTier
import com.aditya1875.pokeverse.feature.game.survivor.domain.model.MatchupPokemon
import com.aditya1875.pokeverse.feature.game.survivor.domain.model.SurvivorRound
import com.aditya1875.pokeverse.feature.game.survivor.domain.state.SurvivorGameState
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SurvivorViewModel(
    private val roundGenerator: SurvivorRoundGenerator,
    private val xpManager: XPManager,
    private val gameScoreDao: GameScoreDao,
    private val userRepository: UserProfileRepository,
    private val context: Context,
    private val imageLoader: ImageLoader,
    billingManager: IBillingManager
) : ViewModel() {

    companion object {
        private const val REVEAL_DELAY_MS = 800L
    }

    val subscriptionState: StateFlow<SubscriptionState> = billingManager.subscriptionState

    private val _gameState = MutableStateFlow<SurvivorGameState>(SurvivorGameState.Idle)
    val gameState: StateFlow<SurvivorGameState> = _gameState.asStateFlow()

    private val _xpResult = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpResult: SharedFlow<XPResult> = _xpResult.asSharedFlow()

    private val roundLoop = EndlessRoundLoop<SurvivorRound, String>(
        scope = viewModelScope,
        generateRound = { streak -> buildNextRound(streak) },
        isCorrect = { round, answer -> answer in round.correctTypes }
    )

    private var timerJob: Job? = null
    private var prefetchJob: Job? = null
    private var prefetchedDefender: MatchupPokemon? = null
    private var score = 0
    private var roundsPlayed = 0
    private var sessionBestScore = 0

    fun startGame() {
        score = 0
        roundsPlayed = 0
        _gameState.value = SurvivorGameState.Loading
        viewModelScope.launch {
            sessionBestScore = userRepository.profileFlow.first().bestSurvivorScore
            roundLoop.start()
            applyLoopState(roundLoop.state.first { it.status != LoopStatus.Loading })
        }
    }

    fun submitAnswer(type: String) {
        val playing = _gameState.value as? SurvivorGameState.Playing ?: return
        timerJob?.cancel()
        resolveAnswer(playing, selectedType = type)
    }

    fun onTimeout() {
        val playing = _gameState.value as? SurvivorGameState.Playing ?: return
        timerJob?.cancel()
        resolveAnswer(playing, selectedType = null)
    }

    fun reviveGame() {
        roundLoop.revive()
        viewModelScope.launch {
            applyLoopState(roundLoop.state.first { it.status != LoopStatus.Loading })
        }
    }

    fun resetGame() {
        timerJob?.cancel()
        _gameState.value = SurvivorGameState.Idle
    }

    private fun resolveAnswer(playing: SurvivorGameState.Playing, selectedType: String?) {
        val round = playing.round
        val wasCorrect = selectedType != null && selectedType in round.correctTypes
        val newStreak = if (wasCorrect) playing.streak + 1 else playing.streak
        if (wasCorrect) {
            score += 10 + (newStreak * 2).coerceAtMost(40)
        }

        // Show the reveal before mutating the loop — freezes input and lets the
        // player see what happened before the next round (or game over) appears.
        _gameState.value = SurvivorGameState.RoundResult(
            round = round,
            selectedType = selectedType,
            wasCorrect = wasCorrect,
            roundsPlayed = playing.roundsPlayed,
            lives = playing.lives,
            streak = playing.streak,
            score = score
        )

        viewModelScope.launch {
            if (wasCorrect) {
                val xp = xpManager.awardGameXP(XPEvent.SurvivorCorrect(newStreak))
                if (xp.xpGained > 0) _xpResult.emit(xp)
            }
            delay(REVEAL_DELAY_MS)

            if (selectedType != null) roundLoop.submitAnswer(selectedType) else roundLoop.onTimeout()
            applyLoopState(roundLoop.state.first { it.status != LoopStatus.Loading })
        }
    }

    private fun applyLoopState(loopState: EndlessLoopState<SurvivorRound>) {
        timerJob?.cancel()
        when (loopState.status) {
            LoopStatus.Playing -> {
                val round = loopState.currentRound ?: return
                roundsPlayed++
                _gameState.value = SurvivorGameState.Playing(
                    round = round,
                    roundsPlayed = roundsPlayed,
                    lives = loopState.lives,
                    streak = loopState.streak,
                    score = score,
                    timeRemainingMs = round.timeBudgetMs,
                    bestScore = sessionBestScore
                )
                startTimer(round.timeBudgetMs)
                // Fetch the round-after-next's Pokémon (and warm its sprite into
                // Coil's cache) while the player is still looking at this one —
                // without this, every round shows a brief loading flash while its
                // sprite fetches over the network.
                prefetchNextRound()
            }
            LoopStatus.GameOver -> finishGame(loopState.bestStreak)
            LoopStatus.Loading -> _gameState.value = SurvivorGameState.Loading
        }
    }

    // The slow network fetch happens here, ahead of time. buildNextRound below
    // just needs the (already pure/instant) buildRound step once it's actually
    // this defender's turn.
    private fun prefetchNextRound() {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            val defender = roundGenerator.fetchDefender() ?: return@launch
            prefetchedDefender = defender
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(defender.spriteUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            )
        }
    }

    private suspend fun buildNextRound(streak: Int): SurvivorRound? {
        val tier = DifficultyTier.forStreak(streak)
        val cached = prefetchedDefender
        prefetchedDefender = null
        val defender = cached ?: roundGenerator.fetchDefender()
        // Falls back to a fresh fetch+build if the cached defender turns out to
        // have no super-effective answer under this tier's modifiers, or if
        // prefetching hadn't finished in time.
        return defender?.let { roundGenerator.buildRound(it, tier) } ?: roundGenerator.generate(streak)
    }

    private fun startTimer(totalMs: Int) {
        timerJob = viewModelScope.launch {
            var remaining = totalMs
            while (remaining > 0) {
                delay(100)
                remaining -= 100
                val playing = _gameState.value as? SurvivorGameState.Playing ?: return@launch
                _gameState.value = playing.copy(timeRemainingMs = remaining.coerceAtLeast(0))
            }
            onTimeout()
        }
    }

    private fun finishGame(bestStreak: Int) {
        val isNewBest = score > sessionBestScore
        viewModelScope.launch {
            val xp = xpManager.awardGameXP(XPEvent.SurvivorComplete)
            if (xp.xpGained > 0) _xpResult.emit(xp)

            userRepository.updateBestScore("survivor", score)
            userRepository.incrementGamesPlayed()

            gameScoreDao.insertScore(
                GameScoreEntity(
                    gameType = "survivor",
                    difficulty = "SURVIVAL",
                    score = score,
                    moves = roundsPlayed,
                    timeSeconds = 0,
                    stars = when {
                        bestStreak >= 20 -> 3
                        bestStreak >= 10 -> 2
                        else -> 1
                    }
                )
            )
        }
        _gameState.value = SurvivorGameState.Finished(
            score = score,
            bestStreak = bestStreak,
            roundsPlayed = roundsPlayed,
            isNewBest = isNewBest
        )
    }

    override fun onCleared() {
        timerJob?.cancel()
        prefetchJob?.cancel()
    }
}
