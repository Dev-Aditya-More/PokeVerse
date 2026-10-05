package com.aditya1875.pokeverse.feature.game.chase.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.game.chase.data.ChaseSpriteLoader
import com.aditya1875.pokeverse.feature.game.chase.data.ChaseSprites
import com.aditya1875.pokeverse.feature.game.chase.domain.engine.ChaseEngine
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseConfig
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseEvent
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseStatus
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseStep
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseWorld
import com.aditya1875.pokeverse.feature.game.chase.domain.state.ChaseGameState
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.core.data.local.dao.GameScoreDao
import com.aditya1875.pokeverse.feature.game.core.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Owns the run's phase and forwards input to [ChaseEngine]. The frame loop
 * itself lives in the UI (it needs Compose's frame clock) and calls [onFrame].
 */
class ChaseViewModel(
    private val spriteLoader: ChaseSpriteLoader,
    private val xpManager: XPManager,
    private val gameScoreDao: GameScoreDao,
    private val userRepository: UserProfileRepository,
    billingManager: IBillingManager
) : ViewModel() {

    private val engine = ChaseEngine()
    val config: ChaseConfig get() = engine.config

    val subscriptionState: StateFlow<SubscriptionState> = billingManager.subscriptionState

    private val _gameState = MutableStateFlow<ChaseGameState>(ChaseGameState.Idle)
    val gameState: StateFlow<ChaseGameState> = _gameState.asStateFlow()

    /** Updated every frame — kept apart from [gameState] so only the canvas and HUD redraw. */
    private val _world = MutableStateFlow(engine.newWorld())
    val world: StateFlow<ChaseWorld> = _world.asStateFlow()

    private val _sprites = MutableStateFlow<ChaseSprites?>(null)
    val sprites: StateFlow<ChaseSprites?> = _sprites.asStateFlow()

    private val _events = MutableSharedFlow<ChaseEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ChaseEvent> = _events.asSharedFlow()

    private val _xpResult = MutableSharedFlow<XPResult>(extraBufferCapacity = 4)
    val xpResult: SharedFlow<XPResult> = _xpResult.asSharedFlow()

    private var bestScore = 0

    private val isRunning: Boolean
        get() = (_gameState.value as? ChaseGameState.Playing)?.isPaused == false

    /** Loads sprites (once) and the player's best, then waits on the start tap. */
    fun prepare() {
        if (_gameState.value != ChaseGameState.Idle) return
        _gameState.value = ChaseGameState.Loading
        viewModelScope.launch {
            if (_sprites.value == null) _sprites.value = spriteLoader.load()
            bestScore = userRepository.profileFlow.first().bestChaseScore
            _world.value = engine.newWorld()
            _gameState.value = ChaseGameState.Ready(bestScore)
        }
    }

    fun start() {
        if (_gameState.value !is ChaseGameState.Ready) return
        _gameState.value = ChaseGameState.Playing()
        _sprites.value?.setAnimating(true)
    }

    fun onFrame(frameMs: Long) {
        if (isRunning) apply(engine.step(_world.value, frameMs))
    }

    fun moveLane(direction: Int) {
        if (isRunning) _world.value = engine.moveLane(_world.value, direction)
    }

    fun thunderbolt() {
        if (isRunning) apply(engine.thunderbolt(_world.value))
    }

    fun pause() {
        if (!isRunning) return
        _gameState.value = ChaseGameState.Playing(isPaused = true)
        _sprites.value?.setAnimating(false)
    }

    fun resume() {
        val playing = _gameState.value as? ChaseGameState.Playing ?: return
        if (!playing.isPaused) return
        _gameState.value = ChaseGameState.Playing(isPaused = false)
        _sprites.value?.setAnimating(true)
    }

    fun revive() {
        if (_gameState.value !is ChaseGameState.Caught || !engine.canRevive(_world.value)) return
        _world.value = engine.revive(_world.value)
        _gameState.value = ChaseGameState.Playing()
        _sprites.value?.setAnimating(true)
    }

    /** Ends the run from the "caught" screen and records the result. */
    fun finish() {
        if (_gameState.value !is ChaseGameState.Caught) return
        val run = _world.value
        val isNewBest = run.score > bestScore
        if (isNewBest) bestScore = run.score
        _gameState.value = ChaseGameState.Finished(
            score = run.score,
            meters = run.meters.toInt(),
            berries = run.berries,
            isNewBest = isNewBest
        )
        recordRun(run)
    }

    fun playAgain() {
        _world.value = engine.newWorld()
        _gameState.value = ChaseGameState.Ready(bestScore)
    }

    private fun apply(step: ChaseStep) {
        _world.value = step.world
        step.events.forEach { _events.tryEmit(it) }
        if (step.world.status == ChaseStatus.Caught) {
            _gameState.value = ChaseGameState.Caught(canRevive = engine.canRevive(step.world))
            _sprites.value?.setAnimating(false)
        }
    }

    private fun recordRun(run: ChaseWorld) {
        val finished = _gameState.value as? ChaseGameState.Finished ?: return
        viewModelScope.launch {
            val xp = xpManager.awardGameXP(XPEvent.ChaseComplete(meters = finished.meters))
            if (xp.xpGained > 0) _xpResult.emit(xp)

            userRepository.updateBestScore("chase", run.score)
            userRepository.incrementGamesPlayed()
            gameScoreDao.insertScore(
                GameScoreEntity(
                    gameType = "chase",
                    difficulty = "ENDLESS",
                    score = run.score,
                    moves = run.berries,
                    timeSeconds = (run.elapsedMs / 1000).toInt(),
                    stars = finished.stars
                )
            )
        }
    }

    override fun onCleared() {
        _sprites.value?.setAnimating(false)
    }
}
