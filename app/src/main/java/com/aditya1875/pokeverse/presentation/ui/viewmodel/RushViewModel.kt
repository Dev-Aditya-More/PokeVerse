package com.aditya1875.pokeverse.presentation.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.data.billing.IBillingManager
import com.aditya1875.pokeverse.data.billing.SubscriptionState
import com.aditya1875.pokeverse.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.data.local.dao.GameScoreDao
import com.aditya1875.pokeverse.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.domain.repository.PokemonRepo
import com.aditya1875.pokeverse.domain.xp.XPEvent
import com.aditya1875.pokeverse.domain.xp.XPManager
import com.aditya1875.pokeverse.domain.xp.XPResult
import com.aditya1875.pokeverse.presentation.screens.game.poketype.components.ALL_TYPES
import com.aditya1875.pokeverse.presentation.screens.game.poketype.components.TypeRushDifficulty
import com.aditya1875.pokeverse.presentation.screens.game.poketype.components.TypeRushQuestion
import com.aditya1875.pokeverse.presentation.screens.game.poketype.components.TypeRushRoundResult
import com.aditya1875.pokeverse.presentation.screens.game.poketype.components.TypeRushState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TypeRushViewModel(
    private val pokemonRepo: PokemonRepo,
    private val xpManager: XPManager,
    private val repository: UserProfileRepository,
    billingManager: IBillingManager,
    private val gameScoreDao: GameScoreDao
) : ViewModel() {

    val subscriptionState: StateFlow<SubscriptionState> = billingManager.subscriptionState

    private val _state = MutableStateFlow<TypeRushState>(TypeRushState.Idle)
    val state: StateFlow<TypeRushState> = _state

    private val _xpResult = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpResult: SharedFlow<XPResult> = _xpResult

    private var timerJob: Job? = null
    private val questions = mutableListOf<TypeRushQuestion>()
    private val roundResults = mutableListOf<TypeRushRoundResult>()
    private var currentScore = 0
    private var correctRounds = 0
    private var currentDifficulty = TypeRushDifficulty.EASY
    private var firstGameAwarded = false

    fun canPlayHard() = subscriptionState.value is SubscriptionState.Premium

    val topScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getTopScores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─────────────────────────────────────────────────────────────────────────
    fun startGame(difficulty: TypeRushDifficulty) {
        currentDifficulty = difficulty
        currentScore = 0
        correctRounds = 0
        questions.clear()
        roundResults.clear()

        viewModelScope.launch {
            _state.value = TypeRushState.Loading

            if (!firstGameAwarded) {
                firstGameAwarded = true
                val bonus = xpManager.awardGameXP(XPEvent.FirstGameOfDay)
                if (bonus.xpGained > 0) _xpResult.emit(bonus)
            }

            try {
                val generated = generateQuestions(difficulty)
                questions.addAll(generated)
                showQuestion(0)
            } catch (e: Exception) {
                Log.e("TypeRush", "Failed to generate questions", e)
                _state.value = TypeRushState.Idle
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User taps a type bubble
    // ─────────────────────────────────────────────────────────────────────────
    fun onTypeTapped(type: String) {
        val current = _state.value as? TypeRushState.Playing ?: return
        if (current.isLocked) return

        val newSelected = if (type in current.selectedTypes) {
            current.selectedTypes - type          // deselect
        } else {
            current.selectedTypes + type          // select
        }

        // If a wrong type was tapped → lock immediately (wrong answer)
        if (type !in current.question.correctTypes && type in newSelected) {
            timerJob?.cancel()
            _state.value = current.copy(selectedTypes = newSelected, isLocked = true)
            viewModelScope.launch {
                delay(400)
                resolveRound(current.copy(selectedTypes = newSelected))
            }
            return
        }

        _state.value = current.copy(selectedTypes = newSelected)

        // All correct types selected → auto-advance
        val allCorrectSelected = current.question.correctTypes.all { it in newSelected }
        if (allCorrectSelected) {
            timerJob?.cancel()
            _state.value = current.copy(selectedTypes = newSelected, isLocked = true)
            viewModelScope.launch {
                delay(600)    // brief pause so user sees the green highlight
                resolveRound(current.copy(selectedTypes = newSelected))
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    fun nextRound() {
        val current = _state.value as? TypeRushState.RoundResult ?: return
        val nextIndex = current.questionIndex + 1
        if (nextIndex >= questions.size) {
            finishGame()
        } else {
            showQuestion(nextIndex)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private fun showQuestion(index: Int) {
        val q = questions[index]
        _state.value = TypeRushState.Playing(
            question = q,
            questionIndex = index,
            totalQuestions = questions.size,
            score = currentScore,
            timeRemaining = currentDifficulty.timePerRound,
        )
        startTimer(index)
    }

    private fun startTimer(questionIndex: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var timeLeft = currentDifficulty.timePerRound
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
                val s = _state.value as? TypeRushState.Playing ?: return@launch
                if (s.isLocked) return@launch
                _state.value = s.copy(timeRemaining = timeLeft)
            }
            // Time up
            val s = _state.value as? TypeRushState.Playing ?: return@launch
            _state.value = s.copy(isLocked = true)
            delay(300)
            resolveRound(s)
        }
    }

    private suspend fun resolveRound(playingState: TypeRushState.Playing) {
        val q = playingState.question
        val selected = playingState.selectedTypes
        val correct = q.correctTypes.toSet()

        val allCorrectSelected = correct.all { it in selected }
        val noWrongSelected = selected.none { it !in correct }
        val isFullyCorrect = allCorrectSelected && noWrongSelected
        val isPartiallyCorrect = correct.any { it in selected }

        val basePoints = when {
            isFullyCorrect -> 100
            isPartiallyCorrect -> 40
            else -> 0
        }
        val timeBonus = if (isFullyCorrect) playingState.timeRemaining * 5 else 0
        val points = basePoints + timeBonus

        currentScore += points
        if (isFullyCorrect) correctRounds++

        // Award XP
        if (isFullyCorrect || isPartiallyCorrect) {
            val xpEvent = XPEvent.QuizAnswer(correct = isFullyCorrect)
            val result = xpManager.awardGameXP(xpEvent)
            if (result.xpGained > 0) _xpResult.emit(result)
        }

        val roundResult = TypeRushRoundResult(
            question = q,
            selectedTypes = selected,
            isFullyCorrect = isFullyCorrect,
            isPartiallyCorrect = isPartiallyCorrect,
            pointsEarned = basePoints,
            timeBonus = timeBonus,
        )
        roundResults.add(roundResult)

        _state.value = TypeRushState.RoundResult(
            result = roundResult,
            questionIndex = playingState.questionIndex,
            totalQuestions = playingState.totalQuestions,
            score = currentScore,
        )
    }

    private fun finishGame() {
        viewModelScope.launch {
            // Completion XP
            val result = xpManager.awardGameXP(
                XPEvent.QuizComplete(score = correctRounds, total = questions.size)
            )
            if (result.xpGained > 0) _xpResult.emit(result)

            repository.updateBestScore("typerush", currentScore)
            repository.incrementGamesPlayed()
        }

        _state.value = TypeRushState.Finished(
            score = currentScore,
            correctRounds = correctRounds,
            totalRounds = questions.size,
            difficulty = currentDifficulty,
            results = roundResults.toList(),
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Question generation
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun generateQuestions(difficulty: TypeRushDifficulty): List<TypeRushQuestion> {
        val questions = mutableListOf<TypeRushQuestion>()
        val usedIds = mutableSetOf<Int>()
        val maxId = when (difficulty) {
            TypeRushDifficulty.EASY   -> 151
            TypeRushDifficulty.MEDIUM -> 493
            TypeRushDifficulty.HARD   -> 1010
        }

        repeat(difficulty.rounds) {
            var attempts = 0
            while (attempts < 50) {
                val id = (1..maxId).random()
                if (id in usedIds) { attempts++; continue }
                usedIds.add(id)
                try {
                    val pokemon = pokemonRepo.getPokemonByName(id.toString())
                    val sprite = if (difficulty.showName) {
                        pokemon.sprites.other?.officialArtwork?.frontDefault
                            ?: pokemon.sprites.front_default
                    } else {
                        // silhouette — still use same sprite, screen will darken it
                        pokemon.sprites.other?.officialArtwork?.frontDefault
                            ?: pokemon.sprites.front_default
                    } ?: run { attempts++; continue }

                    val correctTypes = pokemon.types.map { it.type.name }

                    val wrongTypes = ALL_TYPES
                        .filter { it !in correctTypes }
                        .shuffled()
                        .take(difficulty.optionCount - correctTypes.size)

                    val options = (correctTypes + wrongTypes).shuffled()

                    questions.add(
                        TypeRushQuestion(
                            pokemonId = id,
                            pokemonName = pokemon.name,
                            spriteUrl = sprite,
                            correctTypes = correctTypes,
                            options = options,
                        )
                    )
                    break
                } catch (e: Exception) {
                    Log.w("TypeRush", "Skip $id: ${e.message}")
                }
                attempts++
            }
        }
        return questions
    }

    fun resetGame() {
        timerJob?.cancel()
        _state.value = TypeRushState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}