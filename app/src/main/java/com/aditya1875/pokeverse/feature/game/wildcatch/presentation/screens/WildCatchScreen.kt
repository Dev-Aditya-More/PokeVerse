package com.aditya1875.pokeverse.feature.game.wildcatch.presentation.screens

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import kotlin.math.roundToInt
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchOdds
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import com.aditya1875.pokeverse.utils.Haptics
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.requiredSize
import com.aditya1875.pokeverse.feature.core.ui.components.LoadErrorState
import com.aditya1875.pokeverse.utils.LoadErrorKind
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import com.aditya1875.pokeverse.feature.pokemon.dex.PokeBallIcon
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.ThrowAccuracy
import com.aditya1875.pokeverse.utils.localizedTypeName
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType
import com.aditya1875.pokeverse.feature.game.core.presentation.GameTips
import com.aditya1875.pokeverse.feature.game.core.presentation.TipCard
import com.aditya1875.pokeverse.feature.game.wildcatch.presentation.components.BallIcon
import com.aditya1875.pokeverse.feature.game.wildcatch.presentation.components.accent
import com.aditya1875.pokeverse.feature.game.wildcatch.presentation.viewmodels.CatchBag
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.core.ui.components.LegendaryBadge
import com.aditya1875.pokeverse.feature.core.ui.components.NoInternetScreen
import com.aditya1875.pokeverse.feature.game.core.data.ads.IRewardedAdManager
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.core.presentation.requestRewardedAd
import com.aditya1875.pokeverse.feature.game.core.presentation.ComboLabel
import com.aditya1875.pokeverse.feature.game.core.presentation.GameLoadingContent
import com.aditya1875.pokeverse.feature.game.core.presentation.PbChip
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.WildCatchDifficulty
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.state.WildCatchGameState
import com.aditya1875.pokeverse.feature.game.wildcatch.presentation.viewmodels.WildCatchViewModel
import com.aditya1875.pokeverse.utils.SoundManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import com.aditya1875.pokeverse.utils.ConnectivityObserver
import com.aditya1875.pokeverse.utils.LegendaryPokemon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import com.aditya1875.pokeverse.feature.game.core.presentation.backdrop.GameBackdrop
import com.aditya1875.pokeverse.feature.game.core.presentation.backdrop.GameScene
import com.aditya1875.pokeverse.feature.game.core.presentation.backdrop.BackdropPulse
import com.aditya1875.pokeverse.feature.game.core.presentation.backdrop.WildBiome


private data class StarParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float  // 1f → 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WildCatchScreen(
    onBack: () -> Unit,
    viewModel: WildCatchViewModel = koinViewModel()
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val bag by viewModel.bag.collectAsStateWithLifecycle()
    // What would help THIS player most goes first (e.g. the shop tip for someone with no good balls).
    val hasGoodBalls = (bag.stock[BallType.GREAT] ?: 0) > 0 || (bag.stock[BallType.ULTRA] ?: 0) > 0
    val hasMasterBall = (bag.stock[BallType.MASTER] ?: 0) > 0
    val tips = remember(hasGoodBalls, hasMasterBall) { GameTips.wildCatch(hasGoodBalls, hasMasterBall) }
    var pendingXp by remember { mutableStateOf<XPResult?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    val hasActiveProgress = gameState is WildCatchGameState.Throwing || gameState is WildCatchGameState.ShakeResult ||
        (gameState as? WildCatchGameState.LoadFailed)?.inGame == true
    val requestExit: () -> Unit = { if (hasActiveProgress) showExitDialog = true else onBack() }

    val connectivityObserver: ConnectivityObserver = koinInject()
    val isOnline by connectivityObserver.isOnline.collectAsState(initial = true)
    val billingManager: IBillingManager = koinInject()
    val subscriptionState by billingManager.subscriptionState.collectAsStateWithLifecycle()
    val isPremium = subscriptionState is SubscriptionState.Premium

    LaunchedEffect(Unit) {
        viewModel.xpResult.collect { pendingXp = it }
    }

    LaunchedEffect(isOnline) {
        if (isOnline && gameState is WildCatchGameState.Idle) {
            viewModel.startGame()
        }
    }

    if (!isOnline && gameState is WildCatchGameState.Idle) {
        LoadErrorState(
            kind = LoadErrorKind.OFFLINE,
            onRetry = { viewModel.startGame() },
            onBack = onBack
        )
        return
    }

    BackHandler(enabled = hasActiveProgress) { showExitDialog = true }

    // Biome follows the wild Pokémon's primary type; it holds through results/game over.
    var biome by remember { mutableStateOf(WildBiome.FIELD) }
    val encounter = (gameState as? WildCatchGameState.Throwing)?.pokemon
        ?: (gameState as? WildCatchGameState.ShakeResult)?.pokemon
    LaunchedEffect(encounter) { encounter?.let { biome = WildBiome.forTypes(it.types) } }
    val shake = gameState as? WildCatchGameState.ShakeResult

    XPOverlay(result = pendingXp, onDismiss = { pendingXp = null }) {
        GameBackdrop(
            scene = GameScene.Wild(biome),
            pulseKey = shake,
            pulseColor = if (shake?.caught == true) BackdropPulse.Correct else BackdropPulse.Wrong
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.game_name_wildcatch_title), color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = requestExit) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = gameState,
                        transitionSpec = {
                            (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.95f))
                                .togetherWith(fadeOut(tween(200)))
                        },
                        label = "game_state"
                    ) { state ->
                        when (state) {
                            is WildCatchGameState.Idle,
                            is WildCatchGameState.Loading -> LoadingContent(
                                modifier = Modifier.fillMaxSize().padding(padding),
                                tips = tips
                            )
                            is WildCatchGameState.LoadFailed -> LoadErrorState(
                                kind = state.kind,
                                onRetry = { viewModel.retryLoad() },
                                // Mid-run, "back" means finishing with the score earned so far.
                                onBack = if (state.inGame) ({ viewModel.endRunAfterFailure() }) else onBack,
                                backLabel = if (state.inGame) R.string.wildcatch_end_run else R.string.action_go_back,
                                modifier = Modifier.padding(padding)
                            )
                            is WildCatchGameState.Throwing -> ThrowingContent(
                                state = state,
                                bag = bag,
                                modifier = Modifier.fillMaxSize().padding(padding),
                                onThrow = { fraction -> viewModel.throwBall(fraction) },
                                isPremium = isPremium,
                                onRunAway = { bypass -> viewModel.runAway(bypass) },
                                onSelectBall = viewModel::selectBall,
                                onUseLure = viewModel::useLure
                            )
                            is WildCatchGameState.ShakeResult -> ShakeResultContent(
                                state = state,
                                modifier = Modifier.fillMaxSize().padding(padding),
                                onNext = { viewModel.nextRound() },
                                onRevive = { viewModel.reviveGame() },
                                isOnline = isOnline,
                                isPremium = isPremium,
                                tips = tips
                            )
                            is WildCatchGameState.Finished -> FinishedContent(
                                state = state,
                                modifier = Modifier.fillMaxSize().padding(padding),
                                onPlayAgain = { viewModel.startGame() },
                                onBack = onBack,
                                tips = tips
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.dialog_exit_game_title)) },
            text = { Text(stringResource(R.string.dialog_exit_game_message)) },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; onBack() }) {
                    Text(stringResource(R.string.quiz_exit_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/** A ball drawn as the real thing (see [BallIcon]); the picked ball is the one in hand. */
@Composable
private fun PokeballCanvas(modifier: Modifier = Modifier, ball: BallType = BallType.POKE) {
    BallIcon(ball = ball, modifier = modifier)
}

private fun BallType.shellColor(): Color = accent()

private fun DrawScope.drawSparkle(x: Float, y: Float, life: Float) {
    val s = 10f * life
    val d = s * 0.6f
    val color = when {
        life > 0.7f -> Color(0xFFFFEB3B)
        life > 0.4f -> Color(0xFFFF9800)
        else -> Color(0xFFFFFFFF)
    }.copy(alpha = life * 0.9f)
    drawLine(color, Offset(x - s, y), Offset(x + s, y), 2.5f)
    drawLine(color, Offset(x, y - s), Offset(x, y + s), 2.5f)
    drawLine(color, Offset(x - d, y - d), Offset(x + d, y + d), 2f)
    drawLine(color, Offset(x - d, y + d), Offset(x + d, y - d), 2f)
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier, tips: List<Int> = emptyList()) {
    GameLoadingContent(
        text = stringResource(R.string.wildcatch_loading),
        modifier = modifier,
        textColor = Color.White.copy(alpha = 0.85f),
        spinnerColor = Color.White,
        tips = tips
    )
}

@Composable
private fun ThrowingContent(
    state: WildCatchGameState.Throwing,
    modifier: Modifier = Modifier,
    bag: CatchBag = CatchBag(),
    onThrow: (Float) -> Unit,
    isPremium: Boolean = false,
    /** Run from this Pokémon. [Boolean] says the limit was already paid for (a rewarded ad, or Pro). */
    onRunAway: (Boolean) -> Unit = {},
    onSelectBall: (BallType) -> Unit = {},
    onUseLure: () -> Unit = {}
) {
    val soundManager: SoundManager = koinInject()
    val haptics: Haptics = koinInject()
    val ringAnim = remember(state.pokemonCount) { Animatable(1f) }
    // The ad that pays for a run once the three free ones are used: loaded ahead, so it is ready when asked.
    val adManager: IRewardedAdManager = koinInject()
    val adState by adManager.adState.collectAsState()
    val adContext = LocalContext.current
    val adActivity = adContext as? Activity
    LaunchedEffect(adState, bag.runsLeft, isPremium) {
        if (!isPremium && bag.runsLeft <= 0 && adState is RewardedAdState.Idle) adManager.loadAd(adContext)
    }
    // Details (types, category, odds, best score) stay tucked away until the player asks for them.
    var showDetails by remember(state.pokemonCount) { mutableStateOf(false) }
    // The ring windows, tighter as the run goes on. The same numbers decide the throw and draw the guide rings.
    val thresholds = remember(state.pokemonCount) { WildCatchDifficulty.ringThresholds(state.pokemonCount) }

    LaunchedEffect(state.pokemonCount) {
        soundManager.play(SoundManager.Sound.RUSH_CLICK, 0.55f)
    }

    LaunchedEffect(state.pokemonCount, state.cycleDurationMs) {
        while (true) {
            ringAnim.snapTo(1f)
            ringAnim.animateTo(0f, tween(state.cycleDurationMs.toInt(), easing = LinearEasing))
        }
    }
    val ringColor = lerp(Color(0xFFE53935), Color(0xFF43A047), 1f - ringAnim.value)
    // A light tick each time the ring slides into a better window, so precision can be felt as well as seen.
    val zone = when {
        ringAnim.value <= thresholds.excellent -> 3
        ringAnim.value <= thresholds.great -> 2
        ringAnim.value <= thresholds.good -> 1
        else -> 0
    }
    LaunchedEffect(zone) { if (zone > 0) haptics.play(Haptics.Haptic.TICK) }

    // The Pokémon (ring included) wanders according to its tier: commons bob, legendaries dart.
    val density = LocalDensity.current.density
    val tier = state.pokemon.tier
    val wander = remember(state.pokemonCount) { Animatable(Offset.Zero, Offset.VectorConverter) }
    LaunchedEffect(state.pokemonCount, tier) {
        fun dp(v: Float) = v * density
        val ease = FastOutSlowInEasing
        when (tier) {
            CatchTier.COMMON -> while (true) {
                wander.animateTo(Offset(0f, dp(-6f)), tween(1200, easing = ease))
                wander.animateTo(Offset(0f, dp(6f)), tween(1200, easing = ease))
            }
            CatchTier.UNCOMMON -> while (true) {
                wander.animateTo(Offset(dp(-24f), dp(-4f)), tween(1000, easing = ease))
                wander.animateTo(Offset(dp(24f), dp(4f)), tween(1000, easing = ease))
            }
            CatchTier.RARE -> while (true) {
                for (p in listOf(Offset(-36f, -14f), Offset(0f, 14f), Offset(36f, -14f), Offset(0f, 14f))) {
                    wander.animateTo(Offset(dp(p.x), dp(p.y)), tween(700, easing = ease))
                }
            }
            CatchTier.EPIC -> while (true) {
                wander.animateTo(
                    Offset(dp(Random.nextFloat() * 112f - 56f), dp(Random.nextFloat() * 48f - 24f)),
                    tween(600, easing = ease)
                )
                delay(250)
            }
            CatchTier.LEGENDARY -> while (true) {
                wander.animateTo(
                    Offset(dp(Random.nextFloat() * 144f - 72f), dp(Random.nextFloat() * 64f - 32f)),
                    tween(180, easing = ease)
                )
                // Long enough that a throw released right after a dart still lands on it.
                delay(Random.nextLong(650L, 1100L))
            }
        }
    }
    // Only the quick tiers can actually dodge; the gentle ones never move far enough to matter.
    val dodgeRadiusPx = density * when (tier) {
        CatchTier.LEGENDARY -> 70f
        CatchTier.EPIC -> 80f
        else -> 100f
    }

    // Throw gesture state
    val coroutineScope = rememberCoroutineScope()
    val ballX = remember(state.pokemonCount) { Animatable(0f) }
    val ballY = remember(state.pokemonCount) { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isThrown by remember { mutableStateOf(false) }
    val velocityTracker = remember { VelocityTracker() }

    // Particle system
    val particles = remember { mutableStateListOf<StarParticle>() }
    var lastEmitMs by remember { mutableStateOf(0L) }
    var ballLayoutCenter by remember { mutableStateOf(Offset.Zero) }
    var ringCenterRoot by remember { mutableStateOf(Offset.Zero) }
    var ringCoords by remember(state.pokemonCount) { mutableStateOf<LayoutCoordinates?>(null) }
    // Read at throw time: onGloballyPositioned does not re-fire for graphicsLayer movement.
    fun currentRingCenter(): Offset? = ringCoords?.takeIf { it.isAttached }?.let {
        it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
    }
    var ballCenterRoot by remember { mutableStateOf(Offset.Zero) }
    val ballScale = remember(state.pokemonCount) { Animatable(1f) }

    // Animate particles every frame
    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            if (particles.isNotEmpty()) {
                for (i in particles.indices.reversed()) {
                    val p = particles[i]
                    if (p.life <= 0.04f) {
                        particles.removeAt(i)
                    } else {
                        particles[i] = p.copy(
                            x = p.x + p.vx,
                            y = p.y + p.vy,
                            vy = p.vy + 0.4f, // gentle gravity
                            life = p.life - 0.04f
                        )
                    }
                }
            }
        }
    }

    // Full-screen Box, gesture covers the whole content area so any swipe registers
    BoxWithConstraints(
        modifier = modifier
            .pointerInput(isThrown) {
                if (isThrown) return@pointerInput
                awaitEachGesture {
                    // awaitFirstDown has no slop, fires on the very first touch event
                    val down = awaitFirstDown(requireUnconsumed = false)
                    velocityTracker.resetTracking()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                    isDragging = true

                    var prev = down.position
                    // Authoritative running position, tracked synchronously in this (restricted-
                    // suspend) gesture loop. The animatable update below still has to be launched
                    // on a separate coroutine, awaitEachGesture's scope can't call arbitrary
                    // suspend functions like Animatable.snapTo directly, but launching one
                    // coroutine per move event used to compute its delta from ballX.value/
                    // ballY.value, which is racy: under a fast swipe, several launches could be
                    // queued before the dispatcher ran any of them, so each one read the same
                    // stale pre-update value and silently dropped the others' deltas. Tracking
                    // the position here instead means every launch carries the correct
                    // accumulated value at the moment it was scheduled, regardless of dispatch
                    // order or delay.
                    var posX = ballX.value
                    var posY = ballY.value
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break

                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val delta = change.position - prev
                        prev = change.position

                        posX += delta.x
                        posY += delta.y
                        val targetPosX = posX
                        val targetPosY = posY
                        coroutineScope.launch {
                            ballX.snapTo(targetPosX)
                            ballY.snapTo(targetPosY)
                        }

                        // Emit sparkles when moving fast (swirl detection)
                        val speed = sqrt(delta.x * delta.x + delta.y * delta.y)
                        val now = System.currentTimeMillis()
                        if (speed > 5f && now - lastEmitMs > 45L) {
                            lastEmitMs = now
                            val ox = ballLayoutCenter.x + posX
                            val oy = ballLayoutCenter.y + posY
                            repeat(3) {
                                val angle = (Random.nextFloat() * 2f * PI).toFloat()
                                val spd = Random.nextFloat() * 7f + 3f
                                particles.add(
                                    StarParticle(
                                        x = ox + (Random.nextFloat() * 24f - 12f),
                                        y = oy + (Random.nextFloat() * 24f - 12f),
                                        vx = cos(angle) * spd,
                                        vy = sin(angle) * spd - 3f,
                                        life = 1f
                                    )
                                )
                            }
                        }
                    }

                    isDragging = false
                    if (!isThrown) {
                        val velocity = velocityTracker.calculateVelocity()
                        // Velocity check only, no displacement check (displacement was causing the bug
                        // because snapTo is async and ballY.value could still be 0 at onDragEnd time)
                        if (velocity.y < -300f) {
                            isThrown = true
                            val captured = ringAnim.value
                            coroutineScope.launch {
                                soundManager.play(SoundManager.Sound.BUTTON_CLICK)
                                // Aim where the Pokémon is right now; if it has moved off by the time the ball lands, it dodged.
                                val aim = currentRingCenter() ?: ringCenterRoot
                                val targetX = aim.x - ballCenterRoot.x
                                val targetY = aim.y - ballCenterRoot.y
                                launch { ballScale.animateTo(0.4f, tween(450)) }
                                launch { ballX.animateTo(targetX, tween(450, easing = FastOutSlowInEasing)) }
                                ballY.animateTo(targetY, tween(450, easing = FastOutSlowInEasing))
                                delay(80)
                                val landed = currentRingCenter() ?: aim
                                val dodged = (landed - aim).getDistance() > dodgeRadiusPx
                                onThrow(if (dodged) 1f else captured)
                            }
                        } else {
                            coroutineScope.launch {
                                launch { ballX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                                ballY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                            }
                        }
                    }
                }
            }
    ) {
        // The ring and Pokémon shrink on short screens so nothing ever runs into the ball at the bottom.
        // Room is kept for the ball, the run button and the hint (about 150dp) and for the heading and tray above.
        val ringBox = (maxHeight - 400.dp).coerceIn(150.dp, 230.dp)

        // ── Static content column ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // HUD: hearts + pokemon count | score + streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(WildCatchViewModel.MAX_LIVES) { i ->
                                Icon(
                                    Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = if (i < state.lives) Color(0xFFE53935) else Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${state.score} pts",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD600)
                        )
                    }
                }
            }

            ComboLabel(combo = state.streak)

            // The name (smaller when it is a long one) with the info button beside it, and the Legendary badge
            // always on its own line underneath, so a big name never has to share a row with it.
            val displayName = state.pokemon.name.replaceFirstChar { it.uppercase() }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = displayName,
                    style = if (displayName.length > 12) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(onClick = { showDetails = !showDetails }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (showDetails) Icons.Filled.Info else Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.wildcatch_details),
                        tint = Color.White.copy(alpha = if (showDetails) 0.95f else 0.7f)
                    )
                }
            }
            if (LegendaryPokemon.isLegendary(state.pokemon.id)) {
                Spacer(Modifier.height(4.dp))
                LegendaryBadge(glow = true)
            }

            Spacer(Modifier.height(10.dp))

            // Pokemon + shrinking catch ring, wandering as one so the ring stays on the Pokémon
            Box(
                modifier = Modifier.graphicsLayer {
                    translationX = wander.value.x
                    translationY = wander.value.y
                }
            ) {
            Box(
                modifier = Modifier
                    .size(ringBox)
                    .onGloballyPositioned { coords ->
                        ringCoords = coords
                        val pos = coords.positionInRoot()
                        ringCenterRoot = Offset(
                            pos.x + coords.size.width / 2f,
                            pos.y + coords.size.height / 2f
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = state.pokemon.spriteUrl,
                    contentDescription = state.pokemon.name,
                    modifier = Modifier.size(ringBox * 0.826f),
                    contentScale = ContentScale.Fit
                )
                Canvas(modifier = Modifier.size(ringBox * 0.957f)) {
                    // Where each window sits: the shrinking ring has to be on (or inside) the matching guide.
                    val full = size.minDimension / 2f
                    val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    listOf(
                        thresholds.good to Color.White.copy(alpha = 0.30f),
                        thresholds.great to Color(0xFF64B5F6).copy(alpha = 0.55f),
                        thresholds.excellent to Color(0xFFFFD54F).copy(alpha = 0.95f),
                    ).forEach { (fraction, color) ->
                        drawCircle(color, radius = full * fraction, style = Stroke(width = 1.5.dp.toPx(), pathEffect = dash))
                    }
                    val radius = full * ringAnim.value
                    if (radius > 4f) {
                        drawCircle(
                            color = ringColor.copy(alpha = 0.9f),
                            radius = radius,
                            style = Stroke(width = 6.dp.toPx())
                        )
                        val innerRadius = (radius - 10.dp.toPx()).coerceAtLeast(0f)
                        if (innerRadius > 0f) {
                            drawCircle(color = ringColor.copy(alpha = 0.15f), radius = innerRadius)
                        }
                    }
                }
            }
            }

            // Ground shadow
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            Spacer(Modifier.height(12.dp))

            // Accuracy zone hint, windows tighten as more Pokémon appear
            val hintText = when {
                ringAnim.value <= thresholds.excellent -> stringResource(R.string.wildcatch_hint_excellent)
                ringAnim.value <= thresholds.great -> stringResource(R.string.wildcatch_hint_great)
                ringAnim.value <= thresholds.good -> stringResource(R.string.wildcatch_hint_good)
                else -> stringResource(R.string.wildcatch_hint_wait)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ringColor.copy(alpha = 0.18f))
                    .padding(horizontal = 16.dp, vertical = 5.dp)
            ) {
                Text(hintText, style = MaterialTheme.typography.bodyMedium, color = ringColor, fontWeight = FontWeight.Bold)
            }

            BallPicker(bag = bag, onSelectBall = onSelectBall, onUseLure = onUseLure)
        }

        // The details float over the screen, so opening them never moves or squeezes anything else.
        AnimatedVisibility(
            visible = showDetails && !isDragging && !isThrown,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 118.dp, start = 20.dp, end = 20.dp)
        ) {
            CatchDetails(state = state, ball = bag.selected)
        }

        // ── Particle canvas (full screen overlay) ─────────────────────────
        if (particles.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { p -> drawSparkle(p.x, p.y, p.life) }
            }
        }

        // ── Pokéball visual (moves with graphicsLayer, glow when dragging) ─
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .size(100.dp)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInParent()
                    ballLayoutCenter = Offset(
                        pos.x + coords.size.width / 2f,
                        pos.y + coords.size.height / 2f
                    )
                    val rootPos = coords.positionInRoot()
                    ballCenterRoot = Offset(
                        rootPos.x + coords.size.width / 2f,
                        rootPos.y + coords.size.height / 2f
                    )
                }
                .graphicsLayer {
                    translationX = ballX.value
                    translationY = ballY.value
                },
            contentAlignment = Alignment.Center
        ) {
            // Colored glow ring while dragging, shows current catch zone
            if (isDragging) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = ringColor.copy(alpha = 0.5f), radius = size.minDimension / 2f, style = Stroke(4.dp.toPx()))
                    drawCircle(color = ringColor.copy(alpha = 0.1f), radius = size.minDimension / 2f)
                }
            }
            PokeballCanvas(
                ball = bag.selected,
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        rotationZ = ballX.value * 0.45f
                        scaleX = ballScale.value
                        scaleY = ballScale.value
                    }
            )
        }

        // Run: skip this Pokémon. No life is lost, but the catch streak ends. Three free runs a game, then an ad.
        val needsAd = !isPremium && bag.runsLeft <= 0
        // Ten runs a day for everyone (Pro included); after that the button is gone until tomorrow.
        if (bag.dailyRunsLeft > 0) androidx.compose.material3.FilledTonalButton(
            onClick = {
                if (needsAd) requestRewardedAd(adContext, adActivity, adManager, adState) { onRunAway(true) }
                else onRunAway(isPremium)
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color.White.copy(alpha = 0.20f),
                contentColor = Color.White
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        ) {
            // Stacked, so the button stays narrow and clear of the ball in the middle of the screen.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.wildcatch_run), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text(
                    when {
                        isPremium -> stringResource(R.string.wildcatch_run_today, bag.dailyRunsLeft)
                        needsAd -> stringResource(R.string.wildcatch_run_ad)
                        else -> stringResource(R.string.wildcatch_run_left, bag.runsLeft)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    color = if (needsAd) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.75f)
                )
            }
        }

        // ── Throw hint ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .widthIn(max = 170.dp)
                .height(18.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isDragging && !isThrown) {
                Text(
                    stringResource(R.string.wildcatch_swipe_to_throw),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * The ball tray under the ring: the free Poké Ball plus any bought ball the trainer holds, and the Rare
 * Lure. Only appears once there is something besides the plain ball to choose, so a new player's screen
 * stays as clean as before.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun BallPicker(
    bag: CatchBag,
    onSelectBall: (BallType) -> Unit,
    onUseLure: () -> Unit,
) {
    val owned = BallType.entries.filter { it == BallType.POKE || (bag.stock[it] ?: 0) > 0 }
    if (owned.size == 1 && bag.lureStock == 0 && bag.lureLeft == 0) return

    Spacer(Modifier.height(8.dp))
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        owned.forEach { ball ->
            val selected = bag.selected == ball
            val label = stringResource(ballName(ball))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) ball.shellColor().copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.40f))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) ball.shellColor() else Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .semantics { contentDescription = label; role = Role.RadioButton }
                    .clickable { onSelectBall(ball) }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                PokeballCanvas(ball = ball, modifier = Modifier.size(18.dp))
                Text(
                    if (ball == BallType.POKE) "∞" else "×${bag.stock[ball] ?: 0}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (bag.lureStock > 0 || bag.lureLeft > 0) {
            val lit = bag.lureLeft > 0
            val lureLabel = stringResource(R.string.item_rare_lure)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (lit) Color(0xFFEC407A).copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.40f))
                    .border(1.dp, if (lit) Color(0xFFEC407A) else Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .semantics { contentDescription = lureLabel; role = Role.Button }
                    .clickable(enabled = !lit) { onUseLure() }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AsyncImage(
                    model = com.aditya1875.pokeverse.feature.progression.presentation.itemSpriteUrl("honey"),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.None
                )
                Text(
                    if (lit) "${bag.lureLeft}" else "×${bag.lureStock}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun ballName(ball: BallType): Int = when (ball) {
    BallType.POKE -> R.string.item_poke_ball
    BallType.GREAT -> R.string.item_great_ball
    BallType.ULTRA -> R.string.item_ultra_ball
    BallType.MASTER -> R.string.item_master_ball
}

@Composable
private fun ShakeResultContent(
    state: WildCatchGameState.ShakeResult,
    modifier: Modifier = Modifier,
    onNext: () -> Unit,
    onRevive: () -> Unit = {},
    isOnline: Boolean = true,
    isPremium: Boolean = false,
    tips: List<Int> = emptyList()
) {
    val soundManager: SoundManager = koinInject()
    val haptics: Haptics = koinInject()
    val shakeAnim = remember { Animatable(0f) }
    var showResult by remember { mutableStateOf(false) }
    val spriteAlpha = remember { Animatable(1f) }
    val spriteOffsetY = remember { Animatable(0f) }
    // The capture sequence: the Pokémon is pulled into the ball, the ball shakes while tension builds,
    // then either it clicks shut (flash, burst, hop) or breaks open and the Pokémon bolts.
    val spriteScale = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }
    val ballHop = remember { Animatable(0f) }
    val celebrate = remember { Animatable(0f) }
    val screenShake = remember { Animatable(0f) }
    var confetti by remember { mutableStateOf(false) }
    val density = LocalDensity.current.density

    val adManager: IRewardedAdManager = koinInject()
    val adState by adManager.adState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(adState, isOnline, isPremium) {
        if (!isPremium && isOnline && state.lives <= 0 && adState is RewardedAdState.Idle) {
            adManager.loadAd(context)
        }
    }

    LaunchedEffect(Unit) {
        // 1. Pulled into the ball.
        launch {
            flash.animateTo(0.8f, tween(80))
            flash.animateTo(0f, tween(260))
        }
        haptics.play(Haptics.Haptic.TICK)
        spriteScale.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
        spriteAlpha.snapTo(0f)

        // 2. The shakes. The pause between them is the suspense, and each one hits a little harder.
        val last = state.shakeCount - 1
        repeat(state.shakeCount) { i ->
            shakeAnim.animateTo(15f, tween(75))
            shakeAnim.animateTo(-15f, tween(75))
            soundManager.play(SoundManager.Sound.CARD_FLIP, 0.6f + 0.12f * i, haptic = false)
            haptics.play(
                when {
                    i == last -> Haptics.Haptic.HEAVY
                    i == 1 -> Haptics.Haptic.CLICK
                    else -> Haptics.Haptic.TICK
                }
            )
            if (i < last) delay(260L)
        }
        shakeAnim.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
        // The held breath before the verdict: longest on a near-miss.
        delay(if (state.accuracy == ThrowAccuracy.EXCELLENT && !state.caught) 560L else 300L)

        if (state.caught) {
            // 3a. Click. Flash, ring, star burst, a happy hop.
            soundManager.play(
                if (state.lifeRecovered) SoundManager.Sound.CORRECT_ANSWER else SoundManager.Sound.MATCH_FOUND,
                haptic = false
            )
            haptics.play(if (state.newCatch) Haptics.Haptic.LEVEL_UP else Haptics.Haptic.SUCCESS)
            confetti = state.newCatch || state.pokemon.tier >= CatchTier.RARE || state.lifeRecovered
            launch {
                flash.animateTo(0.55f, tween(70))
                flash.animateTo(0f, tween(320))
            }
            launch {
                celebrate.snapTo(0f)
                celebrate.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing))
            }
            launch {
                ballHop.animateTo(1f, tween(130))
                ballHop.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
            }
        } else {
            // 3b. It broke free: the ball rattles open, the screen shakes, and the Pokémon bolts.
            soundManager.play(SoundManager.Sound.WRONG_ANSWER, haptic = false)
            haptics.play(Haptics.Haptic.REJECT)
            launch {
                for (x in listOf(10f, -9f, 7f, -5f, 3f, 0f)) screenShake.animateTo(x, tween(45))
            }
            spriteScale.snapTo(0.2f)
            spriteAlpha.snapTo(1f)
            launch { spriteScale.animateTo(1.1f, spring(Spring.DampingRatioMediumBouncy)) }
            delay(420L)
            launch { spriteAlpha.animateTo(0f, tween(420)) }
            spriteOffsetY.animateTo(-38f, tween(420))
        }
        showResult = true
    }

    val resultColor = if (state.caught) Color(0xFF43A047) else Color(0xFFE53935)
    val resultText = when {
        state.lifeRecovered -> stringResource(R.string.wildcatch_gotcha_life)
        state.caught -> stringResource(R.string.wildcatch_gotcha)
        else -> stringResource(R.string.wildcatch_fled)
    }
    val gold = Color(0xFFFFD600)

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .graphicsLayer { translationX = screenShake.value * density },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Accuracy badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    stringResource(
                        when {
                            // A good throw that still lost must not read "Great!" above "It fled!".
                            !state.caught && state.accuracy != ThrowAccuracy.MISS -> R.string.throw_broke_free
                            // Caught despite a missed ring (a 3% fluke, or a Master Ball): never say "Missed".
                            state.caught && state.accuracy == ThrowAccuracy.MISS ->
                                if (state.ball == BallType.MASTER) R.string.throw_master else R.string.throw_lucky
                            else -> when (state.accuracy) {
                            ThrowAccuracy.EXCELLENT -> R.string.throw_excellent
                            ThrowAccuracy.GREAT -> R.string.throw_great
                            ThrowAccuracy.GOOD -> R.string.throw_good
                            ThrowAccuracy.MISS -> R.string.throw_miss
                            }
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White
                )
            }

            Spacer(Modifier.height(20.dp))

            // The Pokémon: sucked into the ball first, and (if it escapes) bursting back out.
            AsyncImage(
                model = state.pokemon.spriteUrl,
                contentDescription = state.pokemon.name,
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        alpha = spriteAlpha.value
                        translationY = spriteOffsetY.value.dp.toPx()
                        scaleX = spriteScale.value
                        scaleY = spriteScale.value
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(16.dp))

            // The ball, with the capture burst drawn around it.
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                CatchBurst(progress = celebrate.value, accent = gold)
                PokeballCanvas(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            val s = 1f + shakeAnim.value / 120f + 0.18f * ballHop.value
                            scaleX = s; scaleY = s
                            translationY = -26.dp.toPx() * ballHop.value
                        }
                        .offset { IntOffset(shakeAnim.value.dp.roundToPx(), 0) }
                )
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showResult,
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (state.lifeRecovered) Color(0xFFFF9800) else resultColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.pokemon.name.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    if (state.caught && state.scoreGain > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "+${state.scoreGain}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = gold
                        )
                        com.aditya1875.pokeverse.feature.evolution.presentation.ShardGainRow(
                            gained = state.shardsGained,
                            capped = state.shardsCapped,
                        )
                        if (state.streak >= 2) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                                Text(
                                    stringResource(R.string.wildcatch_streak, state.streak),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }
                    if (state.newCatch) {
                        Spacer(Modifier.height(8.dp))
                        NewCatchPill()
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.wildcatch_dex_total, state.caughtTotal),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    if (LegendaryPokemon.isLegendary(state.pokemon.id)) {
                        Spacer(Modifier.height(6.dp))
                        LegendaryBadge(glow = true)
                    }
                }
            }

            // The free space between the result and the buttons: a tip fits here when there is room for it.
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (showResult && tips.isNotEmpty() && maxHeight >= 96.dp) TipCard(tips = tips)
            }

            // Lives + score row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(WildCatchViewModel.MAX_LIVES) { i ->
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = if (i < state.lives) Color(0xFFE53935) else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text("${state.score} pts", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = gold)
            }

            Spacer(Modifier.height(12.dp))

            if (state.lives <= 0 && showResult) {
                OutlinedButton(
                    onClick = {
                        if (isPremium) onRevive()
                        else requestRewardedAd(context, activity, adManager, adState) { onRevive() }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = gold
                    ),
                    border = BorderStroke(1.5.dp, gold.copy(alpha = 0.6f))
                ) {
                    Text(
                        stringResource(if (isPremium) R.string.survivor_revive else R.string.survivor_revive_ad),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            Button(
                onClick = onNext,
                enabled = showResult,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (state.lives <= 0) Color(0xFF555555) else Color(0xFFE53935))
            ) {
                Text(
                    stringResource(if (state.lives <= 0) R.string.wildcatch_see_results else R.string.wildcatch_next_pokemon),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // The capture flash.
        if (flash.value > 0.01f) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
        }

        // Confetti for a new entry, a rare catch, or a heart won back.
        if (confetti) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
            val progress by animateLottieCompositionAsState(composition = composition, iterations = 1)
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** A gold ring and a spray of streaks that burst from the ball when a Pokémon is caught. */
@Composable
private fun CatchBurst(progress: Float, accent: Color) {
    if (progress <= 0f || progress >= 1f) return
    Canvas(modifier = Modifier.requiredSize(300.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        // Expanding ring.
        val ringR = (50.dp.toPx()) + 100.dp.toPx() * progress
        drawCircle(
            color = accent.copy(alpha = (1f - progress) * 0.85f),
            radius = ringR,
            center = c,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = (7f - 5f * progress).dp.toPx())
        )
        // Star streaks.
        val tints = listOf(accent, Color.White, Color(0xFF29C6E0), Color(0xFFFF7043))
        for (i in 0 until 22) {
            val angle = (i / 22f) * 2f * PI.toFloat() + (i % 3) * 0.07f
            val reach = (60 + (i % 4) * 22).dp.toPx() * (0.4f + 0.9f * progress)
            val from = 26.dp.toPx() + reach * 0.55f
            val dir = Offset(cos(angle), sin(angle))
            drawLine(
                color = tints[i % tints.size].copy(alpha = (1f - progress).coerceIn(0f, 1f)),
                start = c + dir * from,
                end = c + dir * (from + 14.dp.toPx() * (1f - progress * 0.5f)),
                strokeWidth = (3f - 1.5f * progress).dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun FinishedContent(
    state: WildCatchGameState.Finished,
    modifier: Modifier = Modifier,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    tips: List<Int> = emptyList()
) {
    val soundManager: SoundManager = koinInject()
    val catchRate = if (state.pokemonCount > 0) state.catches.toFloat() / state.pokemonCount else 0f
    val stars = when {
        catchRate >= 0.85f -> 3
        catchRate >= 0.60f -> 2
        else -> 1
    }

    LaunchedEffect(Unit) {
        soundManager.play(if (stars >= 2) SoundManager.Sound.GAME_WIN else SoundManager.Sound.GAME_LOSE)
    }

    // Centred when it fits, scrollable when the tip makes it taller than the screen.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PokeballCanvas(modifier = Modifier.size(88.dp))

        Spacer(Modifier.height(20.dp))

        Text(stringResource(R.string.wildcatch_over), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
        if (state.isNewBest) {
            Spacer(Modifier.height(6.dp))
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFFD700).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
            ) {
                Text(
                    stringResource(R.string.duel_new_best),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD700)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("${state.catches} / ${state.pokemonCount} Pokémon caught", style = MaterialTheme.typography.titleLarge, color = Color(0xFFFFD600), fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index ->
                Icon(
                    if (index < stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = if (index < stars) Color(0xFFFFD600) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.4f)).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${state.score}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD600))
                Text("points", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.wildcatch_catch_rate, (catchRate * 100).toInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
        ) {
            Text(stringResource(R.string.action_play_again), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = Color.White)
        ) {
            Text(stringResource(R.string.action_back_to_games), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (tips.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            TipCard(tips = tips)
        }
    }
    }
}

/**
 * What is worth knowing about the Pokémon in front of you, shown only when the info button is tapped: its
 * types and category, the catch odds for the ball in hand at each throw quality, and the run so far.
 */
@Composable
private fun CatchDetails(state: WildCatchGameState.Throwing, ball: BallType, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.60f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.pokemon.types.forEach { type -> TypeChip(type) }
            TierChip(state.pokemon.tier)
        }
        Text(
            stringResource(R.string.wildcatch_details_chance, stringResource(ballName(ball))),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.75f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ThrowAccuracy.GOOD to R.string.wildcatch_zone_good,
                ThrowAccuracy.GREAT to R.string.wildcatch_zone_great,
                ThrowAccuracy.EXCELLENT to R.string.wildcatch_zone_excellent,
            ).forEach { (accuracy, label) ->
                val pct = (CatchOdds.chance(accuracy, state.pokemonCount, state.pokemon.tier, ball) * 100).roundToInt()
                Column(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("$pct%", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(label), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.wildcatch_details_run, state.pokemonCount, state.catches),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            PbChip(bestScore = state.bestScore, currentScore = state.score)
        }
    }
}

/** Category marker (Common to Ultra Rare), same pill as the type chips. Legendaries have their own badge, so show nothing here. */
@Composable
private fun TierChip(tier: CatchTier) {
    val (label, color) = when (tier) {
        CatchTier.COMMON -> R.string.wildcatch_tier_common to Color(0xFF9E9E9E)
        CatchTier.UNCOMMON -> R.string.wildcatch_tier_uncommon to Color(0xFF66BB6A)
        CatchTier.RARE -> R.string.wildcatch_tier_rare to Color(0xFF42A5F5)
        CatchTier.EPIC -> R.string.wildcatch_tier_epic to Color(0xFFAB47BC)
        else -> return
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.25f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TypeChip(type: String) {
    val color = typeColor(type)
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.25f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(localizedTypeName(type), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

private fun typeColor(type: String): Color = when (type.lowercase()) {
    "fire" -> Color(0xFFFF6D00)
    "water" -> Color(0xFF2196F3)
    "grass" -> Color(0xFF4CAF50)
    "electric" -> Color(0xFFFFD600)
    "psychic" -> Color(0xFFE91E8C)
    "ice" -> Color(0xFF80DEEA)
    "dragon" -> Color(0xFF7B1FA2)
    "dark" -> Color(0xFF4E342E)
    "fairy" -> Color(0xFFF06292)
    "fighting" -> Color(0xFFD32F2F)
    "flying" -> Color(0xFF90CAF9)
    "poison" -> Color(0xFF9C27B0)
    "ground" -> Color(0xFFD4A017)
    "rock" -> Color(0xFF8D6E63)
    "bug" -> Color(0xFF8BC34A)
    "ghost" -> Color(0xFF4527A0)
    "steel" -> Color(0xFF90A4AE)
    "normal" -> Color(0xFF9E9E9E)
    else -> Color(0xFF9E9E9E)
}



/** Same dark pill as the accuracy badge above it, so a first catch reads as part of the result, not an overlay. */
@Composable
private fun NewCatchPill() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PokeBallIcon(Modifier.size(14.dp))
        Text(
            stringResource(R.string.wildcatch_new_catch),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}
