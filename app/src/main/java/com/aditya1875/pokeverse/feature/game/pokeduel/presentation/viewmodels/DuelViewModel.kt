package com.aditya1875.pokeverse.feature.game.pokeduel.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.game.pokeduel.domain.engine.DuelGameEngine
import com.aditya1875.pokeverse.feature.game.pokeduel.domain.model.DuelGameState
import com.aditya1875.pokeverse.feature.game.pokeduel.domain.model.DuelOutcome
import com.aditya1875.pokeverse.feature.game.pokeduel.domain.model.DuelPokemon
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase.GetPokemonByNameUseCase
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DuelViewModel(
    private val getPokemonByName: GetPokemonByNameUseCase,
    private val engine: DuelGameEngine,
    private val xpManager: XPManager,
    private val userRepository: UserProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow<DuelGameState>(DuelGameState.Idle)
    val state: StateFlow<DuelGameState> = _state

    private val _xpResult = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpResult: SharedFlow<XPResult> = _xpResult

    private val maxPokemonId = 1010

    fun startGame() {
        viewModelScope.launch {
            _state.value = DuelGameState.Loading
            loadNextRound(round = 1, score = 0, streak = 0, lives = 3)
        }
    }

    fun onChoice(choice: DuelOutcome) {
        val current = _state.value as? DuelGameState.Dueling ?: return
        if (current.result != null) return // already answered

        val result = engine.evaluate(current.left, current.right)
        val isCorrect = choice == result.outcome ||
                (result.outcome == DuelOutcome.DRAW && choice == DuelOutcome.DRAW)

        val newStreak = if (isCorrect) current.streak + 1 else 0
        val gained = engine.calculateScore(100, newStreak, isCorrect)
        val newScore = current.score + gained
        val newLives = if (isCorrect) current.lives else current.lives - 1

        _state.value = current.copy(
            result = result,
            userChoice = choice,
            isCorrect = isCorrect,
            score = newScore,
            streak = newStreak,
            lives = newLives
        )

        viewModelScope.launch {
            // Award XP
            if (isCorrect) {
                val xp = xpManager.awardGameXP(XPEvent.GuessCorrect(streak = newStreak))
                if (xp.xpGained > 0) _xpResult.emit(xp)
            }

            delay(2000L) // Show result for 2 seconds

            if (newLives <= 0) {
                val xp = xpManager.awardGameXP(XPEvent.GuessComplete)
                if (xp.xpGained > 0) _xpResult.emit(xp)
                val currentBest = userRepository.profileFlow.first().bestDuelScore
                userRepository.updateBestScore("duel", newScore)
                userRepository.incrementGamesPlayed()
                _state.value = DuelGameState.GameOver(
                    score = newScore,
                    round = current.round,
                    isNewBest = newScore > currentBest
                )
            } else {
                loadNextRound(
                    round = current.round + 1,
                    score = newScore,
                    streak = newStreak,
                    lives = newLives
                )
            }
        }
    }

    private suspend fun loadNextRound(round: Int, score: Int, streak: Int, lives: Int) {
        _state.value = DuelGameState.Loading
        try {
            val (left, right) = fetchTwoDifferentPokemon()
            _state.value = DuelGameState.Dueling(
                left = left,
                right = right,
                round = round,
                score = score,
                streak = streak,
                lives = lives
            )
        } catch (e: Exception) {
            _state.value = DuelGameState.Idle
        }
    }

    private suspend fun fetchTwoDifferentPokemon(): Pair<DuelPokemon, DuelPokemon> {
        val ids = (1..maxPokemonId).shuffled().take(2)
        val pokemon = ids.map { id ->
            val p = getPokemonByName(id.toString())
            DuelPokemon(
                id = p.id,
                name = p.name,
                spriteUrl = p.sprites.other?.officialArtwork?.frontDefault
                    ?: p.sprites.front_default ?: "",
                types = p.types.map { it.type.name }
            )
        }
        return pokemon[0] to pokemon[1]
    }

    fun resetGame() {
        _state.value = DuelGameState.Idle
    }
}