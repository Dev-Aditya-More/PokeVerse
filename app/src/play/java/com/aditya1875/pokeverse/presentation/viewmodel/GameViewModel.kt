package com.aditya1875.pokeverse.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.data.billing.BillingManager
import com.aditya1875.pokeverse.data.local.dao.GameScoreDao
import com.aditya1875.pokeverse.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.domain.repository.PokemonRepo
import com.aditya1875.pokeverse.utils.CardState
import com.aditya1875.pokeverse.utils.Difficulty
import com.aditya1875.pokeverse.utils.GameState
import com.aditya1875.pokeverse.utils.SubscriptionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class GameViewModel(
    private val repository: PokemonRepo,
    private val gameScoreDao: GameScoreDao,
    private val billingManager: BillingManager   // INJECT
) : ViewModel() {

    val subscriptionState = billingManager.subscriptionState

    fun canPlayDifficulty(difficulty: Difficulty): Boolean {
        return when (subscriptionState.value) {
            is SubscriptionState.Premium -> true
            else -> when (difficulty) {
                Difficulty.EASY   -> true
                Difficulty.MEDIUM -> _gamesPlayedToday.value < FREE_DAILY_MEDIUM_LIMIT
                Difficulty.HARD   -> false
            }
        }
    }

    private val _gameState = MutableStateFlow<GameState>(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState

    private val _selectedDifficulty = MutableStateFlow(Difficulty.EASY)
    val selectedDifficulty: StateFlow<Difficulty> = _selectedDifficulty

    val topScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getTopScores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getRecentScores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _gamesPlayedToday = MutableStateFlow(0)
    val gamesPlayedToday: StateFlow<Int> = _gamesPlayedToday

    private var timerJob: Job? = null
    private var gameStartTime = 0L
    private var currentDifficulty = Difficulty.EASY

    // Free tier limits
    private val FREE_DAILY_MEDIUM_LIMIT = 3

    fun selectDifficulty(difficulty: Difficulty) {
        _selectedDifficulty.value = difficulty
    }

    fun startGame(difficulty: Difficulty) {
        currentDifficulty = difficulty
        _gameState.value = GameState.Loading
        viewModelScope.launch {
            try {
                val pokemon = fetchRandomPokemon(difficulty.pairs)
                val cards = createCardDeck(pokemon, difficulty)
                _gameState.value = GameState.Playing(
                    cards = cards,
                    timeRemaining = difficulty.timeSeconds,
                    difficulty = difficulty
                )
                gameStartTime = System.currentTimeMillis()
                startTimer()
            } catch (e: Exception) {
                Log.e("GameVM", "Failed to start game", e)
                _gameState.value = GameState.Idle
            }
        }
    }

    private suspend fun fetchRandomPokemon(count: Int): List<Pair<String, String>> {
        // Total Pokemon in PokeAPI (up to gen 9)
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
        }.take(count) // Ensure we have exactly the right count
    }

    private fun createCardDeck(
        pokemon: List<Pair<String, String>>,
        difficulty: Difficulty
    ): List<CardState> {
        val cards = mutableListOf<CardState>()

        pokemon.forEachIndexed { pairId, (name, spriteUrl) ->
            // Add two cards for each Pokemon (the pair)
            cards.add(CardState(
                index = pairId * 2,
                pokemonName = name,
                spriteUrl = spriteUrl,
                pairId = pairId
            ))
            cards.add(CardState(
                index = pairId * 2 + 1,
                pokemonName = name,
                spriteUrl = spriteUrl,
                pairId = pairId
            ))
        }

        return cards.shuffled().mapIndexed { idx, card ->
            card.copy(index = idx)
        }
    }

    fun onCardFlipped(cardIndex: Int) {
        val currentState = _gameState.value as? GameState.Playing ?: return
        val card = currentState.cards[cardIndex]

        // Ignore if already flipped or matched
        if (card.isFlipped || card.isMatched) return

        // Ignore if two cards already flipped
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
            val currentState = _gameState.value as? GameState.Playing ?: return@launch
            val firstCard = currentState.cards[firstIndex]
            val secondCard = currentState.cards[secondIndex]
            val newMoves = currentState.moves + 1

            if (firstCard.pairId == secondCard.pairId) {
                // MATCH FOUND
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
                _gameState.value = updatedState

                // Check if game is complete
                if (newMatchedPairs.size == currentState.difficulty.pairs) {
                    handleVictory(updatedState)
                }
            } else {
                // NO MATCH - wait then flip back
                delay(800)
                val state = _gameState.value as? GameState.Playing ?: return@launch
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
        val perfectMoves = totalPairs // best case - find every pair first try
        val moveRatio = perfectMoves.toFloat() / moves.coerceAtLeast(1)

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

        val existingBest = gameScoreDao.getBestScore(state.difficulty.name)
        val isNewBest = existingBest == null || state.score > existingBest.score

        // Save score
        gameScoreDao.insertScore(
            GameScoreEntity(
                difficulty = state.difficulty.name,
                score = state.score,
                moves = state.moves,
                timeSeconds = timeTaken,
                stars = stars
            )
        )

        _gamesPlayedToday.value++

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