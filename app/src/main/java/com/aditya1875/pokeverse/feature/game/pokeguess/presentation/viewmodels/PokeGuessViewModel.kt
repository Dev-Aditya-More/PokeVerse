package com.aditya1875.pokeverse.feature.game.pokeguess.presentation.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.feature.game.core.data.local.dao.GameScoreDao
import com.aditya1875.pokeverse.feature.game.core.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.game.pokeguess.domain.model.GuessDifficulty
import com.aditya1875.pokeverse.feature.game.pokeguess.domain.model.PokeGuessQuestion
import com.aditya1875.pokeverse.feature.game.pokeguess.domain.state.GuessGameState
import com.aditya1875.pokeverse.feature.game.pokeguess.domain.usecases.GeneratePokeGuessQuestionsUseCase
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
import kotlin.collections.forEach

class PokeGuessViewModel(
    gameScoreDao: GameScoreDao,
    billingManager: IBillingManager,
    private val xpManager: XPManager,
    private val userRepository: UserProfileRepository,
    private val generateQuestionsUseCase: GeneratePokeGuessQuestionsUseCase,
    private val imageLoader: ImageLoader,
    private val context: Context
) : ViewModel() {

    val subscriptionState: StateFlow<SubscriptionState> = billingManager.subscriptionState

    private val _gameState = MutableStateFlow<GuessGameState>(GuessGameState.Idle)
    val gameState: StateFlow<GuessGameState> = _gameState.asStateFlow()

    private val _xpResult = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpResult: SharedFlow<XPResult> = _xpResult.asSharedFlow()
    private var timerJob: Job? = null
    private var currentScore = 0
    private var correctAnswers = 0
    private var currentStreak = 0
    private val allQuestions = mutableListOf<PokeGuessQuestion>()
    private var firstGameOfDayAwarded = false

    val topScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getTopScores()
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    val recentScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getRecentScores()
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    fun prefetchSprites(questions: List<PokeGuessQuestion>) {
        questions.forEach { question ->
            val request = ImageRequest.Builder(context)
                .data(question.spriteUrl)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            imageLoader.enqueue(request)
        }
    }

    fun startGame(difficulty: GuessDifficulty) {
        viewModelScope.launch {
            _gameState.value = GuessGameState.Loading

            // First-game bonus
            if (!firstGameOfDayAwarded) {
                val bonus = xpManager.awardGameXP(XPEvent.FirstGameOfDay)
                if (bonus.xpGained > 0) _xpResult.emit(bonus)
                firstGameOfDayAwarded = true
            }

            try {
                val questions = generateQuestionsUseCase(difficulty)
                allQuestions.clear()
                allQuestions.addAll(questions)
                currentScore = 0
                correctAnswers = 0
                currentStreak = 0
                showQuestion(0, difficulty)
            } catch (e: Exception) {
                Log.e("PokeGuess", "Failed to generate questions", e)
                _gameState.value = GuessGameState.Idle
            }
        }
    }

    private fun showQuestion(index: Int, difficulty: GuessDifficulty) {

        if (index >= allQuestions.size) {
            val current = _gameState.value
            if (current is GuessGameState.ShowingSilhouette) {
                finishGame(difficulty, current)
            } else {
                finishGame(
                    difficulty,
                    GuessGameState.ShowingSilhouette(
                        question = allQuestions.last(),
                        currentQuestionIndex = index,
                        totalQuestions = allQuestions.size,
                        score = currentScore,
                        timeRemaining = 0
                    )
                )
            }
            return
        }

        val question = allQuestions[index]

        _gameState.value = GuessGameState.ShowingSilhouette(
            question = question,
            currentQuestionIndex = index,
            totalQuestions = allQuestions.size,
            score = currentScore,
            timeRemaining = difficulty.timePerQuestion
        )

        startTimer(difficulty.timePerQuestion, index)
    }

    private fun startTimer(totalTime: Int, questionIndex: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var timeLeft = totalTime

            while (timeLeft > 0) {
                delay(1000)
                timeLeft--

                val currentState = _gameState.value
                if (currentState is GuessGameState.ShowingSilhouette) {
                    _gameState.value = currentState.copy(timeRemaining = timeLeft)
                }
            }

            onTimeUp(questionIndex)
        }
    }

    private fun onTimeUp(questionIndex: Int) {
        timerJob?.cancel()

        val question = allQuestions[questionIndex]

        _gameState.value = GuessGameState.Revealing(
            question = question,
            selectedAnswer = "",
            isCorrect = false,
            isTimeUp = true,
            currentQuestionIndex = questionIndex,
            totalQuestions = allQuestions.size,
            score = currentScore
        )
    }

    fun submitAnswer(selectedAnswer: String, questionIndex: Int, difficulty: GuessDifficulty) {
        timerJob?.cancel()

        val question = allQuestions[questionIndex]
        val isCorrect = selectedAnswer == question.pokemonName

        if (isCorrect) {
            correctAnswers++
            currentStreak++

            val currentState = _gameState.value
            val timeBonus = if (currentState is GuessGameState.ShowingSilhouette) {
                (currentState.timeRemaining * 10).coerceAtLeast(0)
            } else 0
            currentScore += 100 + timeBonus

            // ── Award XP for correct guess ────────────────────────────────────
            viewModelScope.launch {
                val result = xpManager.awardGameXP(XPEvent.GuessCorrect(streak = currentStreak))
                if (result.xpGained > 0) _xpResult.emit(result)
            }
        } else {
            currentStreak = 0   // reset streak on wrong answer
        }

        _gameState.value = GuessGameState.Revealing(
            question = question,
            selectedAnswer = selectedAnswer,
            isCorrect = isCorrect,
            isTimeUp = false,
            currentQuestionIndex = questionIndex,
            totalQuestions = allQuestions.size,
            score = currentScore
        )
    }

    fun nextQuestion(difficulty: GuessDifficulty) {
        val currentState = _gameState.value
        if (currentState is GuessGameState.Revealing) {
            val nextIndex = currentState.currentQuestionIndex + 1
            // Eagerly prefetch the one after next too
            allQuestions.getOrNull(nextIndex + 1)?.let { prefetchSprites(listOf(it)) }
            showQuestion(nextIndex, difficulty)
        }
    }

    private fun finishGame(difficulty: GuessDifficulty, state: GuessGameState.ShowingSilhouette) {
        viewModelScope.launch {
            val result = xpManager.awardGameXP(XPEvent.GuessComplete)
            if (result.xpGained > 0) _xpResult.emit(result)

            userRepository.updateBestScore("guess", currentScore)
            userRepository.incrementGamesPlayed()
        }

        _gameState.value = GuessGameState.Finished(
            score = currentScore,
            correctAnswers = correctAnswers,
            totalQuestions = allQuestions.size,
            difficulty = difficulty
        )
    }

    fun resetGame() {
        timerJob?.cancel()
        _gameState.value = GuessGameState.Idle
        currentScore = 0
        correctAnswers = 0
        allQuestions.clear()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}