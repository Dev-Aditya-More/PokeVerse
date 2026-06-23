package com.aditya1875.pokeverse.feature.game.wildcatch.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.game.core.data.local.dao.GameScoreDao
import com.aditya1875.pokeverse.feature.game.core.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.ThrowAccuracy
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.WildCatchPokemon
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.state.WildCatchGameState
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class WildCatchViewModel(
    private val repo: PokemonDetailRepo,
    private val xpManager: XPManager,
    private val gameScoreDao: GameScoreDao,
    private val userRepository: UserProfileRepository
) : ViewModel() {

    companion object {
        const val MAX_LIVES = 3
        private const val STREAK_FOR_LIFE = 5 // recover a heart every 5-catch streak
    }

    private val _gameState = MutableStateFlow<WildCatchGameState>(WildCatchGameState.Idle)
    val gameState: StateFlow<WildCatchGameState> = _gameState.asStateFlow()

    private val _xpResult = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpResult: SharedFlow<XPResult> = _xpResult.asSharedFlow()

    private var currentLives = MAX_LIVES
    private var pokemonCount = 0
    private var catches = 0
    private var currentScore = 0
    private var currentStreak = 0
    private var nextPokemon: WildCatchPokemon? = null

    fun startGame() {
        currentLives = MAX_LIVES
        pokemonCount = 0
        catches = 0
        currentScore = 0
        currentStreak = 0
        nextPokemon = null
        _gameState.value = WildCatchGameState.Loading
        viewModelScope.launch {
            val pokemon = loadRandomPokemon()
            if (pokemon != null) {
                pokemonCount++
                _gameState.value = throwingState(pokemon)
                prefetchNext()
            } else {
                _gameState.value = WildCatchGameState.Idle
            }
        }
    }

    // ringFraction: 1f = ring at full size (worst), 0f = ring smallest (perfect)
    fun throwBall(ringFraction: Float) {
        val state = _gameState.value as? WildCatchGameState.Throwing ?: return

        val accuracy = when {
            ringFraction <= 0.20f -> ThrowAccuracy.PERFECT
            ringFraction <= 0.40f -> ThrowAccuracy.GREAT
            ringFraction <= 0.70f -> ThrowAccuracy.NICE
            else -> ThrowAccuracy.MISS
        }

        val caught = Random.nextFloat() < accuracy.catchChance
        var lifeRecovered = false

        if (caught) {
            catches++
            currentStreak++
            currentScore += 100 + accuracy.scoreBonus
            // Recover a life every STREAK_FOR_LIFE consecutive catches
            if (currentStreak % STREAK_FOR_LIFE == 0 && currentLives < MAX_LIVES) {
                currentLives++
                lifeRecovered = true
            }
            viewModelScope.launch {
                val result = xpManager.awardGameXP(XPEvent.WildCatchCaught(streak = currentStreak))
                if (result.xpGained > 0) _xpResult.emit(result)
            }
        } else {
            currentStreak = 0
            currentLives--
        }

        _gameState.value = WildCatchGameState.ShakeResult(
            pokemon = state.pokemon,
            caught = caught,
            accuracy = accuracy,
            lifeRecovered = lifeRecovered,
            pokemonCount = state.pokemonCount,
            catches = catches,
            score = currentScore,
            lives = currentLives
        )
    }

    fun nextRound() {
        if (currentLives <= 0) {
            finishGame()
            return
        }
        val cached = nextPokemon
        nextPokemon = null
        if (cached != null) {
            pokemonCount++
            _gameState.value = throwingState(cached)
            prefetchNext()
        } else {
            _gameState.value = WildCatchGameState.Loading
            viewModelScope.launch {
                val pokemon = loadRandomPokemon()
                if (pokemon != null) {
                    pokemonCount++
                    _gameState.value = throwingState(pokemon)
                    prefetchNext()
                } else {
                    finishGame()
                }
            }
        }
    }

    fun reviveGame() {
        currentLives = 1
        nextRound()
    }

    fun resetGame() {
        _gameState.value = WildCatchGameState.Idle
        currentLives = MAX_LIVES
        pokemonCount = 0
        catches = 0
        currentScore = 0
        currentStreak = 0
        nextPokemon = null
    }

    private fun throwingState(pokemon: WildCatchPokemon) = WildCatchGameState.Throwing(
        pokemon = pokemon,
        cycleDurationMs = speedFor(pokemonCount),
        pokemonCount = pokemonCount,
        catches = catches,
        score = currentScore,
        streak = currentStreak,
        lives = currentLives
    )

    private fun prefetchNext() {
        viewModelScope.launch { nextPokemon = loadRandomPokemon() }
    }

    private suspend fun loadRandomPokemon(): WildCatchPokemon? {
        repeat(5) {
            try {
                val id = (1..809).random()
                val p = repo.getPokemonByName(id.toString())
                val sprite = p.sprites.other?.officialArtwork?.frontDefault
                    ?: p.sprites.front_default ?: return@repeat
                return WildCatchPokemon(
                    id = id,
                    name = p.name,
                    spriteUrl = sprite,
                    types = p.types.map { it.type.name }
                )
            } catch (_: Exception) {}
        }
        return null
    }

    // Speed scales with how many Pokémon have appeared — pressure mounts over time
    private fun speedFor(count: Int): Long = when {
        count <= 3 -> 3500L
        count <= 6 -> 3000L
        count <= 10 -> 2500L
        count <= 15 -> 2200L
        count <= 20 -> 1900L
        count <= 25 -> 1600L
        count <= 30 -> 1400L
        else -> 1200L
    }

    private fun finishGame() {
        viewModelScope.launch {
            val result = xpManager.awardGameXP(XPEvent.WildCatchComplete)
            if (result.xpGained > 0) _xpResult.emit(result)
            userRepository.updateBestScore("wildcatch", currentScore)
            userRepository.incrementGamesPlayed()
            val catchRate = if (pokemonCount > 0) catches.toFloat() / pokemonCount else 0f
            gameScoreDao.insertScore(
                GameScoreEntity(
                    gameType = "wildcatch",
                    difficulty = "SURVIVAL",
                    score = currentScore,
                    moves = pokemonCount,
                    timeSeconds = 0,
                    stars = when {
                        catchRate >= 0.85f -> 3
                        catchRate >= 0.60f -> 2
                        else -> 1
                    }
                )
            )
        }
        _gameState.value = WildCatchGameState.Finished(
            catches = catches,
            pokemonCount = pokemonCount,
            score = currentScore
        )
    }
}
