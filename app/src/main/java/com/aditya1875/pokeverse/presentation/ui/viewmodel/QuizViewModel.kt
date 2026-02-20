package com.aditya1875.pokeverse.presentation.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.data.billing.IBillingManager
import com.aditya1875.pokeverse.data.billing.PremiumPlan
import com.aditya1875.pokeverse.data.billing.SubscriptionState
import com.aditya1875.pokeverse.data.local.dao.GameScoreDao
import com.aditya1875.pokeverse.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.data.remote.model.gameModels.GameScore
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components.QuizDifficulty
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components.QuizGameState
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components.QuizQuestionBank
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components.QuizUiState
import com.aditya1875.pokeverse.utils.Difficulty
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuizViewModel(
    private val gameScoreDao: GameScoreDao,
    private val billingManager: IBillingManager
) : ViewModel() {

    val subscriptionState: StateFlow<SubscriptionState> = billingManager.subscriptionState

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Idle)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    val topScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getTopScores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getRecentScores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun canPlayDifficulty(difficulty: Difficulty): Boolean {
        return when (difficulty) {
            Difficulty.EASY -> true
            Difficulty.MEDIUM -> true
            Difficulty.HARD -> subscriptionState.value is SubscriptionState.Premium
        }
    }

    fun startQuiz(difficulty: QuizDifficulty) {
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            delay(300)

            val questions = QuizQuestionBank.getQuestionsByDifficulty(difficulty)

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
        _uiState.value = QuizUiState.Idle
    }

    fun selectAnswer(answerIndex: Int) {
        val currentState = _uiState.value
        if (currentState !is QuizUiState.Playing) return

        timerJob?.cancel()

        val gameState = currentState.gameState
        val currentQuestion = gameState.questions[gameState.currentQuestionIndex]
        val isCorrect = answerIndex == currentQuestion.correctAnswerIndex

        val questionScore = if (isCorrect) {
            calculateQuestionScore(
                isCorrect = true,
                timeRemaining = gameState.timeRemaining,
                totalTime = gameState.totalTimePerQuestion
            )
        } else 0

        val updatedGameState = gameState.copy(
            score = gameState.score + questionScore,
            correctAnswers = if (isCorrect) gameState.correctAnswers + 1 else gameState.correctAnswers,
            answers = gameState.answers.apply { this[gameState.currentQuestionIndex] = answerIndex }
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
        val stars = calculateStars(
            score = gameState.score,
            totalQuestions = gameState.questions.size
        )

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

    fun resetQuiz() {
        timerJob?.cancel()
        _uiState.value = QuizUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}