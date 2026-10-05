package com.aditya1875.pokeverse.feature.game.chase.presentation.screens

import androidx.activity.compose.BackHandler
import com.aditya1875.pokeverse.feature.game.core.presentation.AdRequestOverlay
import com.aditya1875.pokeverse.feature.game.core.presentation.AdRequestPhase
import com.aditya1875.pokeverse.feature.game.core.presentation.PreloadRewardedAd
import com.aditya1875.pokeverse.feature.game.core.presentation.rememberPausingRewardedAd
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.core.ui.components.NoInternetScreen
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseEvent
import com.aditya1875.pokeverse.feature.game.chase.domain.state.ChaseGameState
import com.aditya1875.pokeverse.feature.game.chase.presentation.components.ChaseAccent
import com.aditya1875.pokeverse.feature.game.chase.presentation.components.ChaseCaughtOverlay
import com.aditya1875.pokeverse.feature.game.chase.presentation.components.ChaseGuideOverlay
import com.aditya1875.pokeverse.feature.game.chase.presentation.components.ChaseHud
import com.aditya1875.pokeverse.feature.game.chase.presentation.components.ChasePausedOverlay
import com.aditya1875.pokeverse.feature.game.chase.presentation.components.ChaseReadyOverlay
import com.aditya1875.pokeverse.feature.game.chase.presentation.components.ChaseTrack
import com.aditya1875.pokeverse.feature.game.chase.presentation.components.laneSteering
import com.aditya1875.pokeverse.feature.game.chase.presentation.viewmodels.ChaseViewModel
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.core.presentation.GameLoadingContent
import com.aditya1875.pokeverse.feature.game.core.presentation.GameResultLayout
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultHeroIcon
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatChips
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import com.aditya1875.pokeverse.utils.ConnectivityObserver
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.aditya1875.pokeverse.utils.SoundManager
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val ChaseBackground = Color(0xFF14181F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaseScreen(
    onBack: () -> Unit,
    viewModel: ChaseViewModel = koinViewModel()
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    // Collected as State but only *read* inside the canvas/HUD, so frames don't recompose this screen.
    val world = viewModel.world.collectAsStateWithLifecycle()
    val sprites by viewModel.sprites.collectAsStateWithLifecycle()
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()
    val connectivityObserver: ConnectivityObserver = koinInject()
    val isOnline by connectivityObserver.isOnline.collectAsState(initial = true)
    val soundManager: SoundManager = koinInject()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingXp by remember { mutableStateOf<XPResult?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    var guideChecked by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    val inRun = gameState is ChaseGameState.Playing || gameState is ChaseGameState.Caught
    val requestExit: () -> Unit = {
        if (inRun) {
            viewModel.pause()
            showExitDialog = true
        } else onBack()
    }

    LaunchedEffect(Unit) {
        showGuide = !ScreenStateManager.isChaseGuideSeen(context)
        guideChecked = true
    }
    LaunchedEffect(Unit) { viewModel.xpResult.collect { pendingXp = it } }
    LaunchedEffect(Unit) { viewModel.events.collect { playFeedback(it, soundManager, haptics) } }
    LaunchedEffect(isOnline, guideChecked, showGuide) {
        if (isOnline && guideChecked && !showGuide) viewModel.prepare()
    }

    // ── Agility lifeline: free once per run, then a rewarded ad (premium skips the ad) ──
    val isPremium = subscriptionState is SubscriptionState.Premium
    // Pauses the instant it's requested and shows a loader, so a slow ad can't cost the run.
    val agilityAd = rememberPausingRewardedAd(onPause = viewModel::pause, onRewarded = viewModel::earnAgility)
    // The run is already stopped on the Caught screen, so there's nothing to pause.
    val reviveAd = rememberPausingRewardedAd(onPause = {}, onRewarded = viewModel::revive)
    // Keep an ad warm for the whole run, so most taps play instantly.
    PreloadRewardedAd(enabled = inRun && !isPremium && isOnline)
    val onAgility: () -> Unit = {
        when {
            world.value.agilityCharges > 0 -> viewModel.useAgility()
            !viewModel.canEarnAgility() -> Unit
            isPremium -> viewModel.earnAgility()
            else -> agilityAd.request()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.pause() }
    BackHandler(enabled = inRun) { requestExit() }

    if (!isOnline && gameState is ChaseGameState.Idle) {
        NoInternetScreen(onRetry = { viewModel.prepare() })
        return
    }

    val running = (gameState as? ChaseGameState.Playing)?.isPaused == false
    if (running) FrameLoop(onFrame = viewModel::onFrame)

    XPOverlay(result = pendingXp, onDismiss = { pendingXp = null }) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.chase_title), color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = requestExit) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.pause(); showGuide = true }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = stringResource(R.string.chase_guide_title), tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ChaseBackground)
                )
            },
            containerColor = ChaseBackground
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).background(ChaseBackground)) {
                when (val state = gameState) {
                    ChaseGameState.Idle, ChaseGameState.Loading -> GameLoadingContent(
                        text = stringResource(R.string.chase_loading),
                        textColor = Color.White.copy(alpha = 0.85f),
                        spinnerColor = ChaseAccent
                    )
                    is ChaseGameState.Finished -> ChaseResult(
                        state = state,
                        onPlayAgain = viewModel::playAgain,
                        onBack = onBack
                    )
                    else -> {
                        ChaseTrack(
                            world = world,
                            sprites = sprites,
                            config = viewModel.config,
                            modifier = Modifier.fillMaxSize().laneSteering(
                                config = viewModel.config,
                                onPress = viewModel::struggle,
                                onSteer = viewModel::steerTo
                            )
                        )
                        if (state is ChaseGameState.Playing || state is ChaseGameState.Caught) {
                            ChaseHud(
                                world = world,
                                agilityMaxExtra = viewModel.config.agilityMaxExtra,
                                isPremium = isPremium,
                                onPause = viewModel::pause,
                                onThunderbolt = viewModel::thunderbolt,
                                onAgility = onAgility
                            )
                        }
                        when (state) {
                            is ChaseGameState.Ready -> ChaseReadyOverlay(state.bestScore, onStart = viewModel::start)
                            is ChaseGameState.Playing -> if (
                                state.isPaused && !showGuide && !showExitDialog && agilityAd.phase == AdRequestPhase.Idle
                            ) {
                                ChasePausedOverlay(onResume = viewModel::resume)
                            }
                            is ChaseGameState.Caught -> ChaseCaughtOverlay(
                                canRevive = state.canRevive,
                                isPremium = isPremium,
                                onRevive = viewModel::revive,
                                onReviveWithAd = reviveAd::request,
                                onGiveUp = viewModel::finish
                            )
                            else -> Unit
                        }
                        AdRequestOverlay(agilityAd, loadingText = stringResource(R.string.chase_agility_ad_loading))
                        AdRequestOverlay(reviveAd, loadingText = stringResource(R.string.chase_revive_ad_loading))
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.chase_leave_title)) },
            text = { Text(stringResource(R.string.chase_leave_message)) },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; onBack() }) {
                    Text(stringResource(R.string.chase_leave), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text(stringResource(R.string.chase_keep_running)) }
            }
        )
    }

    if (showGuide) {
        ChaseGuideOverlay(onDismiss = {
            showGuide = false
            scope.launch { ScreenStateManager.markChaseGuideSeen(context) }
        })
    }
}

/** Drives the simulation off Compose's frame clock, so it's vsync-aligned and stops when the screen leaves. */
@Composable
private fun FrameLoop(onFrame: (Long) -> Unit) {
    val currentOnFrame by rememberUpdatedState(onFrame)
    LaunchedEffect(Unit) {
        var last = withFrameMillis { it }
        while (true) {
            withFrameMillis { now ->
                currentOnFrame(now - last)
                last = now
            }
        }
    }
}

@Composable
private fun ChaseResult(
    state: ChaseGameState.Finished,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit
) {
    val soundManager: SoundManager = koinInject()
    LaunchedEffect(Unit) {
        soundManager.play(if (state.stars >= 2) SoundManager.Sound.GAME_WIN else SoundManager.Sound.GAME_LOSE)
    }
    GameResultLayout(
        title = stringResource(R.string.chase_result_title),
        subtitle = stringResource(R.string.chase_result_subtitle, state.meters),
        score = state.score.toString(),
        scoreLabel = stringResource(R.string.chase_score_label),
        heroColor = ChaseAccent,
        stars = state.stars,
        isNewBest = state.isNewBest,
        onPlayAgain = onPlayAgain,
        onBack = onBack,
        heroContent = { ResultHeroIcon(icon = Icons.Default.Bolt, heroColor = ChaseAccent) },
        statsContent = {
            ResultStatChips(
                stringResource(R.string.chase_stat_distance) to stringResource(R.string.chase_meters, state.meters),
                stringResource(R.string.chase_stat_berries) to "${state.berries}"
            )
        }
    )
}

private fun playFeedback(event: ChaseEvent, sound: SoundManager, haptics: HapticFeedback) {
    when (event) {
        is ChaseEvent.Hit -> {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            sound.play(SoundManager.Sound.WRONG_ANSWER)
        }
        is ChaseEvent.Collected -> sound.play(SoundManager.Sound.RUSH_CLICK, volume = 0.6f)
        ChaseEvent.AgilityStarted -> {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            sound.play(SoundManager.Sound.PIKACHU_CRY)
        }
        is ChaseEvent.Zapped -> {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            sound.play(SoundManager.Sound.RUSH_CLICK, volume = 0.4f)
        }
        is ChaseEvent.ThunderUsed -> {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            sound.play(SoundManager.Sound.PIKACHU_CRY)
        }
        ChaseEvent.AgilityEnded -> sound.play(SoundManager.Sound.TIMER_UP, volume = 0.35f)
        ChaseEvent.NetIncoming -> sound.play(SoundManager.Sound.TIMER_UP, volume = 0.5f)
        ChaseEvent.Snared -> {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            sound.play(SoundManager.Sound.WRONG_ANSWER, volume = 0.7f)
        }
        is ChaseEvent.Struggled -> {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            sound.play(SoundManager.Sound.RUSH_CLICK, volume = 0.5f)
        }
        ChaseEvent.BrokeFree, ChaseEvent.ShookOffRocket -> sound.play(SoundManager.Sound.CORRECT_ANSWER)
        ChaseEvent.Caught -> sound.play(SoundManager.Sound.GAME_LOSE)
        ChaseEvent.NetDodged -> Unit
    }
}
