package com.aditya1875.pokeverse.feature.game.poketype.presentation.viewmodels

import android.content.Context
import android.util.Log
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
import com.aditya1875.pokeverse.feature.game.pokeguess.domain.model.PokeGuessQuestion
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.game.poketype.data.generator.TypeRushQuestionGenerator
import com.aditya1875.pokeverse.feature.game.poketype.domain.engine.TypeRushEngine
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.RUSH_MAX_LIVES
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
import kotlin.collections.forEach

class TypeRushViewModel(
    private val xpManager: XPManager,
    private val repository: UserProfileRepository,
    billingManager: IBillingManager,
    private val gameScoreDao: GameScoreDao,
    private val generator: TypeRushQuestionGenerator,
    private val engine: TypeRushEngine,
    private val context: Context,
    private val imageLoader: ImageLoader
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
    private var lives = RUSH_MAX_LIVES
    private var roundsPlayed = 0
    private val usedPokemonIds = mutableSetOf<Int>()
    private var isExtendingPool = false
    private var currentDifficulty = TypeRushDifficulty.EASY
    private var firstGameAwarded = false

    fun canPlayHard() = subscriptionState.value is SubscriptionState.Premium

    val topScores: StateFlow<List<GameScoreEntity>> = gameScoreDao.getTopScoresForGame("typerush")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun prefetchSprites(questions: List<TypeRushQuestion>) {
        questions.forEach { question ->
            val request = ImageRequest.Builder(context)
                .data(question.spriteUrl)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            imageLoader.enqueue(request)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    fun startGame(difficulty: TypeRushDifficulty) {
        currentDifficulty = difficulty
        currentScore = 0
        correctRounds = 0
        lives = RUSH_MAX_LIVES
        roundsPlayed = 0
        questions.clear()
        roundResults.clear()
        usedPokemonIds.clear()

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
                usedPokemonIds.addAll(generated.map { it.pokemonId })
                // Warm the image cache for the whole batch now — by the time the
                // player advances past round 1, the rest are already downloading/cached
                // instead of only starting once each round composes.
                prefetchSprites(generated.drop(1))
                showQuestion(0)
            } catch (e: Exception) {
                Log.e("TypeRush", "Failed to generate questions", e)
                _state.value = TypeRushState.Idle
            }
        }
    }

    // ── Endless mode: keep the round pool topped up ───────────────────────────
    private fun extendPoolIfNeeded(currentIndex: Int) {
        if (questions.size - currentIndex - 1 > 3 || isExtendingPool) return
        isExtendingPool = true
        viewModelScope.launch {
            try {
                val more = generator.generate(currentDifficulty)
                    .filter { it.pokemonId !in usedPokemonIds }
                if (more.isNotEmpty()) {
                    questions.addAll(more)
                    usedPokemonIds.addAll(more.map { it.pokemonId })
                    prefetchSprites(more.take(3))
                }
            } catch (e: Exception) {
                Log.e("TypeRush", "Failed to extend round pool", e)
            } finally {
                isExtendingPool = false
            }
        }
    }

    /** Freeze the countdown while the ad dialog / rewarded ad is on screen */
    fun pauseTimer() {
        timerJob?.cancel()
    }

    /** Resume the countdown from where it was paused */
    fun resumeTimer() {
        val current = _state.value as? TypeRushState.Playing ?: return
        if (!current.isLocked && current.timeRemaining > 0) {
            startTimer(current.questionIndex, startFrom = current.timeRemaining)
        }
    }

    /** Rewarded-ad perk: advance without answering — no life lost */
    fun skipRound() {
        val current = _state.value as? TypeRushState.Playing ?: return
        timerJob?.cancel()
        val nextIndex = current.questionIndex + 1
        if (nextIndex >= questions.size) {
            finishGame()
            return
        }
        prefetchSprites(listOfNotNull(questions.getOrNull(nextIndex + 1)))
        showQuestion(nextIndex)
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
        // Run ends when the hearts run out
        if (lives <= 0) {
            finishGame()
            return
        }
        val nextIndex = current.questionIndex + 1
        if (nextIndex >= questions.size) {
            finishGame()
        } else {
            prefetchSprites(listOf(questions[nextIndex]))
            showQuestion(nextIndex)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private fun showQuestion(index: Int) {
        val q = questions[index]
        _state.value = TypeRushState.Playing(
            question = q,
            questionIndex = index,
            roundsPlayed = roundsPlayed,
            lives = lives,
            score = currentScore,
            timeRemaining = currentDifficulty.timePerRound,
        )
        extendPoolIfNeeded(index)
        startTimer(index)
    }

    private fun startTimer(questionIndex: Int, startFrom: Int = currentDifficulty.timePerRound) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var timeLeft = startFrom
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
        if (result.isFullyCorrect) correctRounds++ else lives--
        roundsPlayed++

        // XP — only fully correct answers earn XP (partial correct = 0 XP, same as before)
        if (result.isFullyCorrect) {
            val xp = xpManager.awardGameXP(XPEvent.RushCorrect)
            if (xp.xpGained > 0) _xpResult.emit(xp)
        }

        roundResults.add(result)

        _state.value = TypeRushState.RoundResult(
            result = result,
            questionIndex = playingState.questionIndex,
            roundsPlayed = roundsPlayed,
            lives = lives,
            score = currentScore
        )
    }

    private fun finishGame() {
        timerJob?.cancel()
        viewModelScope.launch {
            // Completion XP
            val result = xpManager.awardGameXP(
                XPEvent.RushComplete(score = correctRounds, total = roundsPlayed)
            )
            if (result.xpGained > 0) _xpResult.emit(result)

            repository.updateBestScore("typerush", currentScore)
            repository.incrementGamesPlayed()

            val totalQ = roundsPlayed.coerceAtLeast(1)
            gameScoreDao.insertScore(
                GameScoreEntity(
                    gameType = "typerush",
                    difficulty = currentDifficulty.name,
                    score = currentScore,
                    moves = 0,
                    timeSeconds = 0,
                    stars = when {
                        correctRounds.toFloat() / totalQ > 0.8f -> 3
                        correctRounds.toFloat() / totalQ > 0.5f -> 2
                        else -> 1
                    }
                )
            )
        }

        _state.value = TypeRushState.Finished(
            score = currentScore,
            correctRounds = correctRounds,
            totalRounds = roundsPlayed,
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