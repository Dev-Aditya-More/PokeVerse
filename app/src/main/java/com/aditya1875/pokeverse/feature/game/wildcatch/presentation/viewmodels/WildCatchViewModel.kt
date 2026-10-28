package com.aditya1875.pokeverse.feature.game.wildcatch.presentation.viewmodels

import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.game.core.data.local.dao.GameScoreDao
import com.aditya1875.pokeverse.feature.game.core.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.ThrowAccuracy
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.WildCatchDifficulty
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
import com.aditya1875.pokeverse.feature.pokemon.dex.DexProgress
import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexRepository
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class WildCatchViewModel(
    private val repo: PokemonDetailRepo,
    private val xpManager: XPManager,
    private val gameScoreDao: GameScoreDao,
    private val userRepository: UserProfileRepository,
    private val pokedex: PokedexRepository
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
    private var sessionBestScore = 0

    // Caught ids as of the last dex update: tells a first catch from a repeat when the ball settles.
    private var knownCaught: Set<Int> = emptySet()

    init {
        viewModelScope.launch { pokedex.progress.collect { knownCaught = it.caught } }
    }

    fun startGame() {
        currentLives = MAX_LIVES
        pokemonCount = 0
        catches = 0
        currentScore = 0
        currentStreak = 0
        nextPokemon = null
        _gameState.value = WildCatchGameState.Loading
        viewModelScope.launch {
            sessionBestScore = userRepository.profileFlow.first().bestWildCatchScore
            val pokemon = loadRandomPokemon()
            if (pokemon != null) {
                pokemonCount++
                _gameState.value = encounter(pokemon)
                prefetchNext()
            } else {
                _gameState.value = WildCatchGameState.Idle
            }
        }
    }

    // ringFraction: 1f = ring at full size (worst), 0f = ring smallest (perfect).
    // Both the timing windows and the odds get tougher as more Pokémon appear —
    // see WildCatchDifficulty for the ramp.
    fun throwBall(ringFraction: Float) {
        val state = _gameState.value as? WildCatchGameState.Throwing ?: return

        val thresholds = WildCatchDifficulty.ringThresholds(state.pokemonCount)
        val accuracy = when {
            ringFraction <= thresholds.perfect -> ThrowAccuracy.PERFECT
            ringFraction <= thresholds.great -> ThrowAccuracy.GREAT
            ringFraction <= thresholds.nice -> ThrowAccuracy.NICE
            else -> ThrowAccuracy.MISS
        }

        val catchChance = accuracy.baseCatchChance *
            WildCatchDifficulty.catchChanceMultiplier(state.pokemonCount) *
            state.pokemon.tier.catchFactor
        val caught = Random.nextFloat() < catchChance
        val newCatch = caught && state.pokemon.id !in knownCaught
        var lifeRecovered = false

        if (caught) {
            catches++
            currentStreak++
            knownCaught = knownCaught + state.pokemon.id
            viewModelScope.launch { pokedex.markCaught(state.pokemon.id) }
            currentScore += 100 + accuracy.scoreBonus + state.pokemon.tier.scoreBonus
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

        // shakeCount tells the visual story: perfect near-miss = 3 nail-biting shakes,
        // clear miss = 1 shake so the player immediately understands why they failed
        val shakeCount = if (caught) 3 else when (accuracy) {
            ThrowAccuracy.PERFECT -> 3
            ThrowAccuracy.GREAT -> 2
            ThrowAccuracy.NICE -> 1
            ThrowAccuracy.MISS -> 1
        }

        _gameState.value = WildCatchGameState.ShakeResult(
            pokemon = state.pokemon,
            caught = caught,
            accuracy = accuracy,
            lifeRecovered = lifeRecovered,
            pokemonCount = state.pokemonCount,
            catches = catches,
            score = currentScore,
            lives = currentLives,
            shakeCount = shakeCount,
            newCatch = newCatch,
            bestScore = sessionBestScore
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
            _gameState.value = encounter(cached)
            prefetchNext()
        } else {
            _gameState.value = WildCatchGameState.Loading
            viewModelScope.launch {
                val pokemon = loadRandomPokemon()
                if (pokemon != null) {
                    pokemonCount++
                    _gameState.value = encounter(pokemon)
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

    /** A wild Pokémon appears: it is now seen, whether or not it ends up caught. */
    private fun encounter(pokemon: WildCatchPokemon): WildCatchGameState.Throwing {
        viewModelScope.launch { pokedex.markSeen(pokemon.id) }
        return throwingState(pokemon)
    }

    private fun throwingState(pokemon: WildCatchPokemon) = WildCatchGameState.Throwing(
        pokemon = pokemon,
        cycleDurationMs = (speedFor(pokemonCount) * pokemon.tier.ringSpeedFactor).toLong(),
        pokemonCount = pokemonCount,
        catches = catches,
        score = currentScore,
        streak = currentStreak,
        lives = currentLives,
        bestScore = sessionBestScore
    )

    private fun prefetchNext() {
        viewModelScope.launch { nextPokemon = loadRandomPokemon() }
    }

    private suspend fun loadRandomPokemon(): WildCatchPokemon? {
        repeat(5) {
            try {
                val id = (1..DexProgress.CATCHABLE).random()
                val p = repo.getPokemonByName(id.toString())
                val sprite = p.sprites.other?.officialArtwork?.frontDefault
                    ?: p.sprites.front_default ?: return@repeat
                val tier = CatchTier.of(id, p.stats.sumOf { it.base_stat })
                // The first few encounters stay friendly: no epic or legendary yet.
                if (pokemonCount < 4 && tier >= CatchTier.EPIC) return@repeat
                return WildCatchPokemon(
                    id = id,
                    name = p.name,
                    spriteUrl = sprite,
                    types = p.types.map { it.type.name },
                    tier = tier
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
            score = currentScore,
            isNewBest = currentScore > sessionBestScore
        )
    }
}
