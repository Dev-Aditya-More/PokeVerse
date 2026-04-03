package com.aditya1875.pokeverse.feature.game.pokematch.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.core.data.local.dao.GameScoreDao
import com.aditya1875.pokeverse.feature.game.core.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.feature.game.pokematch.domain.model.CardState
import com.aditya1875.pokeverse.feature.game.pokematch.domain.model.Difficulty
import com.aditya1875.pokeverse.feature.game.pokematch.domain.model.GameState
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MatchViewModel(
    private val repository: PokemonDetailRepo,
    private val gameScoreDao: GameScoreDao,
    billingManager: IBillingManager,
    private val xpManager: XPManager,
    private val userRepository: UserProfileRepository,
    private val imageLoader: ImageLoader,
    private val appContext: Context
) : ViewModel() {

    private val _gameState = MutableStateFlow<GameState>(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState

    private val _selectedDifficulty = MutableStateFlow(Difficulty.EASY)
    val selectedDifficulty: StateFlow<Difficulty> = _selectedDifficulty

    private val _xpResult = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpResult: SharedFlow<XPResult> = _xpResult.asSharedFlow()

    val subscriptionState: StateFlow<SubscriptionState> = billingManager.subscriptionState
    val topScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getTopScores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getRecentScores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val matchCheckMutex = Mutex()

    private var gameCompleted = false

    private var timerJob: Job? = null
    private var currentDifficulty = Difficulty.EASY

    private var firstGameOfDayAwarded = false

    suspend fun preloadSprites(urls: List<String>) {
        urls.forEach { url ->
            imageLoader.execute(
                ImageRequest.Builder(appContext)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            )
        }
    }

    fun canPlayDifficulty(difficulty: Difficulty): Boolean {
        return when (difficulty) {
            Difficulty.HARD -> subscriptionState.value is SubscriptionState.Premium
            else -> true
        }
    }

    fun selectDifficulty(difficulty: Difficulty) {
        _selectedDifficulty.value = difficulty
    }

    fun startGame(difficulty: Difficulty) {
        currentDifficulty = difficulty
        gameCompleted = false
        _gameState.value = GameState.Loading

        viewModelScope.launch {
            try {
                val pokemon = fetchRandomPokemon(difficulty.pairs)
                val cards = createCardDeck(pokemon, difficulty)

                val urls = cards.map { it.spriteUrl }.distinct()

                preloadSprites(urls)

                _gameState.value = GameState.Playing(
                    cards = cards,
                    timeRemaining = difficulty.timeSeconds,
                    difficulty = difficulty
                )

                startTimer()

            } catch (e: Exception) {
                Log.e("MatchVM", "Failed to start game", e)
                _gameState.value = GameState.Idle
            }
        }
    }

    private suspend fun fetchRandomPokemon(count: Int): List<Pair<String, String>> {
        val maxPokemonId = 1010
        val randomIds = (1..maxPokemonId).shuffled().take(count)

        return randomIds.mapNotNull { id ->
            try {
                val pokemon = repository.getPokemonByName(id.toString())
                val spriteUrl = pokemon.sprites.other?.officialArtwork?.frontDefault
                    ?: pokemon.sprites.front_default
                    ?: return@mapNotNull null
                Pair(pokemon.name, spriteUrl)
            } catch (e: Exception) {
                null
            }
        }.take(count)
    }

    private fun createCardDeck(
        pokemon: List<Pair<String, String>>,
        difficulty: Difficulty
    ): List<CardState> {
        val cards = mutableListOf<CardState>()

        pokemon.forEachIndexed { pairId, (name, spriteUrl) ->
            cards.add(
                CardState(
                    index = pairId * 2,
                    pokemonName = name,
                    spriteUrl = spriteUrl,
                    pairId = pairId
                )
            )
            cards.add(
                CardState(
                    index = pairId * 2 + 1,
                    pokemonName = name,
                    spriteUrl = spriteUrl,
                    pairId = pairId
                )
            )
        }

        return cards.shuffled().mapIndexed { idx, card ->
            card.copy(index = idx)
        }
    }

    fun onCardFlipped(cardIndex: Int) {
        val currentState = _gameState.value as? GameState.Playing ?: return
        val card = currentState.cards[cardIndex]

        if (card.isFlipped || card.isMatched) return
        if (currentState.flippedIndices.size >= 2) return

        val newFlipped = currentState.flippedIndices + cardIndex
        val newCards = currentState.cards.toMutableList()
        newCards[cardIndex] = card.copy(isFlipped = true)

        _gameState.value = currentState.copy(
            cards = newCards,
            flippedIndices = newFlipped
        )

        if (newFlipped.size == 2) {
            checkForMatch(newFlipped[0], newFlipped[1])
        }
    }

    private fun checkForMatch(firstIndex: Int, secondIndex: Int) {
        viewModelScope.launch {
            matchCheckMutex.withLock {
                val currentState = _gameState.value as? GameState.Playing ?: return@launch

                if (gameCompleted) return@launch

                val firstCard = currentState.cards[firstIndex]
                val secondCard = currentState.cards[secondIndex]
                val newMoves = currentState.moves + 1

                if (firstCard.pairId == secondCard.pairId) {
                    // Cards match
                    val newCards = currentState.cards.toMutableList()
                    newCards[firstIndex] = firstCard.copy(isMatched = true)
                    newCards[secondIndex] = secondCard.copy(isMatched = true)

                    val newMatchedPairs = currentState.matchedPairs + firstCard.pairId
                    val newScore = calculateScore(
                        moves = newMoves,
                        matchesFound = newMatchedPairs.size,
                        totalPairs = currentState.difficulty.pairs,
                        timeRemaining = currentState.timeRemaining,
                        difficulty = currentState.difficulty
                    )

                    val updatedState = currentState.copy(
                        cards = newCards,
                        flippedIndices = emptyList(),
                        matchedPairs = newMatchedPairs,
                        moves = newMoves,
                        score = newScore
                    )

                    val matchedPairsCount = newMatchedPairs.size
                    val matchedCardsCount = newCards.count { it.isMatched }
                    val totalPairs = currentState.difficulty.pairs
                    val expectedCards = totalPairs * 2

                    val isVictory = matchedPairsCount >= totalPairs &&
                            matchedCardsCount >= expectedCards

                    Log.d(
                        "PokéMatch",
                        "Match! Pairs: $matchedPairsCount/$totalPairs, Cards: $matchedCardsCount/$expectedCards, Victory: $isVictory"
                    )

                    if (isVictory) {
                        gameCompleted = true

                        timerJob?.cancel()

                        Log.d("PokéMatch", "🎉 Victory! Timer cancelled, showing victory screen")

                        _gameState.value = updatedState

                        delay(400)

                        handleVictory(updatedState)
                    } else {
                        // Not victory yet, just update state
                        _gameState.value = updatedState
                    }
                } else {
                    delay(800)

                    val state = _gameState.value as? GameState.Playing ?: return@launch

                    if (gameCompleted) return@launch

                    val updatedCards = state.cards.toMutableList()
                    updatedCards[firstIndex] = state.cards[firstIndex].copy(isFlipped = false)
                    updatedCards[secondIndex] = state.cards[secondIndex].copy(isFlipped = false)

                    _gameState.value = state.copy(
                        cards = updatedCards,
                        flippedIndices = emptyList(),
                        moves = newMoves
                    )
                }
            }
        }
    }

    private fun calculateScore(
        moves: Int,
        matchesFound: Int,
        totalPairs: Int,
        timeRemaining: Int,
        difficulty: Difficulty
    ): Int {
        val baseScore = matchesFound * 100
        val timeBonus = timeRemaining * 2
        val difficultyMultiplier = when (difficulty) {
            Difficulty.EASY -> 1
            Difficulty.MEDIUM -> 2
            Difficulty.HARD -> 3
        }
        val movePenalty = (moves - matchesFound).coerceAtLeast(0) * 5
        return ((baseScore + timeBonus - movePenalty) * difficultyMultiplier).coerceAtLeast(0)
    }

    private fun calculateStars(
        moves: Int,
        totalPairs: Int,
        timeRemaining: Int,
        totalTime: Int
    ): Int {
        val timePercent = timeRemaining.toFloat() / totalTime
        val moveRatio = totalPairs.toFloat() / moves.coerceAtLeast(1)

        return when {
            timePercent > 0.5f && moveRatio > 0.7f -> 3
            timePercent > 0.2f && moveRatio > 0.4f -> 2
            else -> 1
        }
    }

    private suspend fun handleVictory(state: GameState.Playing) {
        timerJob?.cancel()
        val timeTaken = state.difficulty.timeSeconds - state.timeRemaining
        val stars = calculateStars(
            moves = state.moves,
            totalPairs = state.difficulty.pairs,
            timeRemaining = state.timeRemaining,
            totalTime = state.difficulty.timeSeconds
        )

        // ── Award XP for completing the match ─────────────────────────────────
        val par = state.difficulty.pairs * 2
        val xpResult = xpManager.awardGameXP(
            XPEvent.MatchComplete(moves = state.moves, par = par)
        )
        if (xpResult.xpGained > 0) _xpResult.emit(xpResult)

        userRepository.updateBestScore("match", state.score)
        userRepository.incrementGamesPlayed()

        val existingBest = gameScoreDao.getBestScore(state.difficulty.name)
        val isNewBest = existingBest == null || state.score > existingBest.score

        gameScoreDao.insertScore(
            GameScoreEntity(
                difficulty = state.difficulty.name,
                score = state.score,
                moves = state.moves,
                timeSeconds = timeTaken,
                stars = stars
            )
        )

        _gameState.value = GameState.Victory(
            score = state.score,
            moves = state.moves,
            timeTaken = timeTaken,
            stars = stars,
            difficulty = state.difficulty,
            isNewBest = isNewBest
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val currentState = _gameState.value as? GameState.Playing ?: break
                val newTime = currentState.timeRemaining - 1

                if (newTime <= 0) {
                    _gameState.value = GameState.TimeUp(
                        matchesFound = currentState.matchedPairs.size,
                        totalPairs = currentState.difficulty.pairs,
                        difficulty = currentState.difficulty
                    )
                    break
                }

                _gameState.value = currentState.copy(timeRemaining = newTime)
            }
        }
    }

    fun pauseGame() {
        timerJob?.cancel()
        val current = _gameState.value as? GameState.Playing ?: return
        _gameState.value = GameState.Paused(current)
    }

    fun resumeGame() {
        val paused = _gameState.value as? GameState.Paused ?: return
        _gameState.value = paused.playing
        startTimer()
    }

    fun restartGame() {
        timerJob?.cancel()
        startGame(currentDifficulty)
    }

    fun returnToMenu() {
        timerJob?.cancel()
        _gameState.value = GameState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}