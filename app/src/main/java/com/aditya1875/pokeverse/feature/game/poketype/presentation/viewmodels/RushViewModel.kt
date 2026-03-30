package com.aditya1875.pokeverse.feature.game.poketype.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.feature.game.core.data.local.dao.GameScoreDao
import com.aditya1875.pokeverse.feature.game.core.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.game.poketype.data.generator.TypeRushQuestionGenerator
import com.aditya1875.pokeverse.feature.game.poketype.domain.engine.TypeRushEngine
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushDifficulty
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushQuestion
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushRoundResult
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TypeRushViewModel(
    private val xpManager: XPManager,
    private val repository: UserProfileRepository,
    billingManager: IBillingManager,
    gameScoreDao: GameScoreDao,
    private val generator: TypeRushQuestionGenerator,
    private val engine: TypeRushEngine
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
                val generated = generator.generate(difficulty)
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

        val result = engine.evaluateAnswer(
            question = playingState.question,
            selected = playingState.selectedTypes,
            timeRemaining = playingState.timeRemaining
        )

        val totalPoints = result.pointsEarned + result.timeBonus

        currentScore += totalPoints
        if (result.isFullyCorrect) correctRounds++

        // XP
        if (result.isFullyCorrect || result.isPartiallyCorrect) {
            val xpEvent = XPEvent.QuizAnswer(correct = result.isFullyCorrect)
            val xp = xpManager.awardGameXP(xpEvent)
            if (xp.xpGained > 0) _xpResult.emit(xp)
        }

        roundResults.add(result)

        _state.value = TypeRushState.RoundResult(
            result = result,
            questionIndex = playingState.questionIndex,
            totalQuestions = playingState.totalQuestions,
            score = currentScore
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

    fun resetGame() {
        timerJob?.cancel()
        _state.value = TypeRushState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}