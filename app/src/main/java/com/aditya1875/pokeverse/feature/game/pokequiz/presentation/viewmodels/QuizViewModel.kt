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
import com.aditya1875.pokeverse.feature.game.pokequiz.data.DynamicQuizRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuizViewModel(
    private val gameScoreDao: GameScoreDao,
    billingManager: IBillingManager,
    private val xpManager: XPManager,
    private val repository: UserProfileRepository,
    private val dynamicQuizRepo: DynamicQuizRepository,
) : ViewModel() {

    val subscriptionState: StateFlow<SubscriptionState> = billingManager.subscriptionState

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Idle)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _xpResult = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpResult: SharedFlow<XPResult> = _xpResult.asSharedFlow()

    private var timerJob: Job? = null

    val topScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getTopScoresForGame("quiz")
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    val recentScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getRecentScores()
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    private val usedQuestionIds = mutableSetOf<Int>()

    private var firstGameOfDayAwarded = false

    fun startQuiz(difficulty: QuizDifficulty) {
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading

            if (!firstGameOfDayAwarded) {
                firstGameOfDayAwarded = true
                val bonus = xpManager.awardGameXP(XPEvent.FirstGameOfDay)
                if (bonus.xpGained > 0) _xpResult.emit(bonus)
            }

            val bestScore = repository.profileFlow.first().bestQuizScore

            val questions = run {
                val dynamic = dynamicQuizRepo.generateQuestions(difficulty)
                if (dynamic.size >= difficulty.questionCount) {
                    dynamic
                } else {
                    val local = QuizQuestionBank.getUnusedQuestions(difficulty, usedQuestionIds)
                    usedQuestionIds.addAll(local.map { it.id })
                    local
                }
            }

            usedQuestionIds.addAll(questions.map { it.id })

            val gameState = QuizGameState(
                questions = questions,
                currentQuestionIndex = 0,
                score = 0,
                correctAnswers = 0,
                timeRemaining = difficulty.timePerQuestion,
                totalTimePerQuestion = difficulty.timePerQuestion,
                difficulty = difficulty,
                combo = 0,
                bestScore = bestScore
            )

            _uiState.value = QuizUiState.Playing(gameState)
            startTimer()
        }
    }

    // ── Endless mode: keep the question pool topped up ────────────────────────
    // Called whenever the player nears the end of the loaded pool.
    private fun extendQuestionPoolIfNeeded(gameState: QuizGameState) {
        val remaining = gameState.questions.size - gameState.currentQuestionIndex - 1
        if (remaining > 3) return

        viewModelScope.launch {
            val more = run {
                val dynamic = try {
                    dynamicQuizRepo.generateQuestions(gameState.difficulty)
                        .filter { it.id !in usedQuestionIds }
                } catch (e: Exception) {
                    emptyList()
                }
                dynamic.ifEmpty {
                    QuizQuestionBank.getUnusedQuestions(gameState.difficulty, usedQuestionIds)
                }
            }
            if (more.isEmpty()) return@launch
            usedQuestionIds.addAll(more.map { it.id })

            // Append to whatever state we're in now — the pool only ever grows
            when (val s = _uiState.value) {
                is QuizUiState.Playing ->
                    _uiState.value = QuizUiState.Playing(
                        s.gameState.copy(questions = s.gameState.questions + more)
                    )
                is QuizUiState.ShowingAnswer ->
                    _uiState.value = s.copy(
                        gameState = s.gameState.copy(questions = s.gameState.questions + more)
                    )
                else -> Unit
            }
        }
    }

    // ── Rewarded-ad perks ──────────────────────────────────────────────────────
    /** Freeze the countdown while the ad dialog / rewarded ad is on screen */
    fun pauseTimer() {
        timerJob?.cancel()
    }

    /** Resume the countdown from where it was paused */
    fun resumeTimer() {
        if (_uiState.value is QuizUiState.Playing) startTimer()
    }

    /** Skip the current question: no life lost, combo kept, doesn't count as answered */
    fun skipQuestion() {
        val currentState = _uiState.value
        if (currentState !is QuizUiState.Playing) return
        timerJob?.cancel()

        val gameState = currentState.gameState
        if (gameState.currentQuestionIndex >= gameState.questions.size - 1) {
            // Pool exhausted and nothing to skip to — treat as finish
            finishQuiz(gameState)
            return
        }
        val next = gameState.copy(
            currentQuestionIndex = gameState.currentQuestionIndex + 1,
            timeRemaining = gameState.totalTimePerQuestion,
            eliminatedOptions = emptyList()
        )
        _uiState.value = QuizUiState.Playing(next)
        extendQuestionPoolIfNeeded(next)
        startTimer()
    }

    /** 50/50 hint: eliminates two wrong options on the current question */
    fun useHint() {
        val currentState = _uiState.value
        if (currentState !is QuizUiState.Playing) return
        val gameState = currentState.gameState
        if (gameState.eliminatedOptions.isNotEmpty()) return

        val question = gameState.questions[gameState.currentQuestionIndex]
        val wrong = question.options.indices
            .filter { it != question.correctAnswerIndex }
            .shuffled()
            .take(2)
        _uiState.value = QuizUiState.Playing(
            gameState.copy(eliminatedOptions = wrong)
        )
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

        val newCombo = if (isCorrect) gameState.combo + 1 else 0

        // Endless mode: wrong answers and timeouts cost a heart
        val newLives = if (isCorrect) gameState.lives else gameState.lives - 1

        val updatedGameState = gameState.copy(
            score = gameState.score + questionScore,
            correctAnswers = if (isCorrect) gameState.correctAnswers + 1 else gameState.correctAnswers,
            questionsAnswered = gameState.questionsAnswered + 1,
            lives = newLives,
            combo = newCombo
        )

        _uiState.value = QuizUiState.ShowingAnswer(
            gameState = updatedGameState,
            selectedAnswerIndex = answerIndex,
            isCorrect = isCorrect,
            explanation = currentQuestion.explanation
        )
        extendQuestionPoolIfNeeded(updatedGameState)
    }

    fun nextQuestion() {
        val currentState = _uiState.value
        if (currentState !is QuizUiState.ShowingAnswer) return

        val gameState = currentState.gameState
        // Run ends when the hearts run out — or the question pool is truly dry
        if (gameState.lives <= 0 || gameState.currentQuestionIndex >= gameState.questions.size - 1) {
            finishQuiz(gameState)
            return
        }

        val nextGameState = gameState.copy(
            currentQuestionIndex = gameState.currentQuestionIndex + 1,
            timeRemaining = gameState.totalTimePerQuestion,
            eliminatedOptions = emptyList()
            // combo and bestScore carry forward automatically via copy
        )
        _uiState.value = QuizUiState.Playing(nextGameState)
        extendQuestionPoolIfNeeded(nextGameState)
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
        val answered = gameState.questionsAnswered.coerceAtLeast(1)
        val stars = calculateStars(gameState.score, answered)
        val isNewBest = gameState.score > gameState.bestScore

        viewModelScope.launch {
            val result = xpManager.awardGameXP(
                XPEvent.QuizComplete(
                    score = gameState.correctAnswers,
                    total = gameState.questionsAnswered
                )
            )
            if (result.xpGained > 0) _xpResult.emit(result)

            repository.updateBestScore("quiz", gameState.score)
            repository.incrementGamesPlayed()

            gameScoreDao.insertScore(
                GameScoreEntity(
                    gameType = "quiz",
                    difficulty = gameState.difficulty.name,
                    score = gameState.score,
                    moves = 0,
                    timeSeconds = 0,
                    stars = stars
                )
            )
        }

        _uiState.value = QuizUiState.Finished(
            score = gameState.score,
            correctAnswers = gameState.correctAnswers,
            totalQuestions = gameState.questionsAnswered,
            difficulty = gameState.difficulty,
            stars = stars,
            isNewBest = isNewBest
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