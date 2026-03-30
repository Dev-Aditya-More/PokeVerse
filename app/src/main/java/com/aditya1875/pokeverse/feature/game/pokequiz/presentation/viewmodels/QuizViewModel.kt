package com.aditya1875.pokeverse.feature.game.pokequiz.presentation.viewmodels

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
import com.aditya1875.pokeverse.feature.game.pokequiz.data.QuizQuestionBank
import com.aditya1875.pokeverse.feature.game.pokequiz.domain.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuizViewModel(
    gameScoreDao: GameScoreDao,
    billingManager: IBillingManager,
    private val xpManager: XPManager,
    private val repository: UserProfileRepository
) : ViewModel() {

    val subscriptionState: StateFlow<SubscriptionState> = billingManager.subscriptionState

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Idle)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _xpResult = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpResult: SharedFlow<XPResult> = _xpResult.asSharedFlow()

    private var timerJob: Job? = null

    val topScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getTopScores()
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    val recentScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getRecentScores()
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    private val usedQuestionIds = mutableSetOf<Int>()

    private var firstGameOfDayAwarded = false

    fun startQuiz(difficulty: QuizDifficulty) {
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            delay(300)

            // First-game-of-day bonus (once per ViewModel lifetime)
            if (!firstGameOfDayAwarded) {
                firstGameOfDayAwarded = true
                val bonus = xpManager.awardGameXP(XPEvent.FirstGameOfDay)
                if (bonus.xpGained > 0) _xpResult.emit(bonus)
            }

            val questions = QuizQuestionBank.getUnusedQuestions(
                difficulty = difficulty,
                excludeIds = usedQuestionIds
            )
            usedQuestionIds.addAll(questions.map { it.id })

            val gameState = QuizGameState(
                questions = questions,
                currentQuestionIndex = 0,
                score = 0,
                correctAnswers = 0,
                timeRemaining = difficulty.timePerQuestion,
                totalTimePerQuestion = difficulty.timePerQuestion,
                difficulty = difficulty
            )

            _uiState.value = QuizUiState.Playing(gameState)
            startTimer()
        }
    }

    fun onBackToMenu() {
        timerJob?.cancel()
        usedQuestionIds.clear() // Reset for new session
        _uiState.value = QuizUiState.Idle
    }

    fun resetQuiz() {
        timerJob?.cancel()
        usedQuestionIds.clear()
        _uiState.value = QuizUiState.Idle
    }

    fun selectAnswer(answerIndex: Int) {
        val currentState = _uiState.value
        if (currentState !is QuizUiState.Playing) return

        timerJob?.cancel()

        val gameState = currentState.gameState
        val currentQuestion = gameState.questions[gameState.currentQuestionIndex]
        val isCorrect = answerIndex == currentQuestion.correctAnswerIndex

        // ── Award XP for this answer immediately ──────────────────────────────
        viewModelScope.launch {
            val result = xpManager.awardGameXP(XPEvent.QuizAnswer(correct = isCorrect))
            if (result.xpGained > 0) _xpResult.emit(result)
        }

        val questionScore = if (isCorrect) {
            calculateQuestionScore(
                isCorrect = true,
                timeRemaining = gameState.timeRemaining,
                totalTime = gameState.totalTimePerQuestion
            )
        } else 0

        val newAnswers = gameState.answers.toMutableList().apply {
            this[gameState.currentQuestionIndex] = answerIndex
        }

        val updatedGameState = gameState.copy(
            score = gameState.score + questionScore,
            correctAnswers = if (isCorrect) gameState.correctAnswers + 1 else gameState.correctAnswers,
            answers = newAnswers
        )

        _uiState.value = QuizUiState.ShowingAnswer(
            gameState = updatedGameState,
            selectedAnswerIndex = answerIndex,
            isCorrect = isCorrect,
            explanation = currentQuestion.explanation
        )
    }

    fun nextQuestion() {
        val currentState = _uiState.value
        if (currentState !is QuizUiState.ShowingAnswer) return

        val gameState = currentState.gameState
        if (gameState.currentQuestionIndex >= gameState.questions.size - 1) {
            finishQuiz(gameState)
            return
        }

        val nextGameState = gameState.copy(
            currentQuestionIndex = gameState.currentQuestionIndex + 1,
            timeRemaining = gameState.totalTimePerQuestion
        )
        _uiState.value = QuizUiState.Playing(nextGameState)
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)

                val currentState = _uiState.value
                if (currentState !is QuizUiState.Playing) break

                val gameState = currentState.gameState
                val newTime = gameState.timeRemaining - 1

                if (newTime <= 0) {
                    selectAnswer(-1)
                    break
                }

                _uiState.value = QuizUiState.Playing(
                    gameState.copy(timeRemaining = newTime)
                )
            }
        }
    }

    private fun finishQuiz(gameState: QuizGameState) {
        val stars = calculateStars(gameState.score, gameState.questions.size)

        viewModelScope.launch {
            val result = xpManager.awardGameXP(
                XPEvent.QuizComplete(
                    score = gameState.correctAnswers,
                    total = gameState.questions.size
                )
            )
            if (result.xpGained > 0) _xpResult.emit(result)

            repository.updateBestScore("quiz", gameState.score)
            repository.incrementGamesPlayed()
        }

        _uiState.value = QuizUiState.Finished(
            score = gameState.score,
            correctAnswers = gameState.correctAnswers,
            totalQuestions = gameState.questions.size,
            difficulty = gameState.difficulty,
            stars = stars
        )
    }

    private fun calculateQuestionScore(isCorrect: Boolean, timeRemaining: Int, totalTime: Int): Int {
        if (!isCorrect) return 0
        val baseScore = 50
        val timeBonus = (timeRemaining.toFloat() / totalTime * 50).toInt()
        return baseScore + timeBonus
    }

    private fun calculateStars(score: Int, totalQuestions: Int): Int {
        val maxScore = totalQuestions * 100
        val percentage = score.toFloat() / maxScore
        return when {
            percentage >= 0.9f -> 3
            percentage >= 0.7f -> 2
            percentage >= 0.5f -> 1
            else -> 0
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}