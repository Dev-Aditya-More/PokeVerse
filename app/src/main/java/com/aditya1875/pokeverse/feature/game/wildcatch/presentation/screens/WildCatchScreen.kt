package com.aditya1875.pokeverse.feature.game.wildcatch.presentation.screens

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

private val BattleSkyColor = Color(0xFF0D1B2A)
private val BattleGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0D1B2A),
        Color(0xFF162033),
        Color(0xFF1A2E1A),
        Color(0xFF0D2010)
    )
)

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
    var pendingXp by remember { mutableStateOf<XPResult?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    val hasActiveProgress = gameState is WildCatchGameState.Throwing || gameState is WildCatchGameState.ShakeResult
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
        NoInternetScreen(onRetry = { viewModel.startGame() })
        return
    }

    BackHandler(enabled = hasActiveProgress) { showExitDialog = true }

    XPOverlay(result = pendingXp, onDismiss = { pendingXp = null }) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Wild Catch", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = requestExit) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BattleSkyColor)
                )
            },
            containerColor = BattleSkyColor
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BattleGradient)
            ) {
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
                            modifier = Modifier.fillMaxSize().padding(padding)
                        )
                        is WildCatchGameState.Throwing -> ThrowingContent(
                            state = state,
                            modifier = Modifier.fillMaxSize().padding(padding),
                            onThrow = { fraction -> viewModel.throwBall(fraction) }
                        )
                        is WildCatchGameState.ShakeResult -> ShakeResultContent(
                            state = state,
                            modifier = Modifier.fillMaxSize().padding(padding),
                            onNext = { viewModel.nextRound() },
                            onRevive = { viewModel.reviveGame() },
                            isOnline = isOnline,
                            isPremium = isPremium
                        )
                        is WildCatchGameState.Finished -> FinishedContent(
                            state = state,
                            modifier = Modifier.fillMaxSize().padding(padding),
                            onPlayAgain = { viewModel.startGame() },
                            onBack = onBack
                        )
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

@Composable
private fun PokeballCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = minOf(size.width, size.height) / 2f * 0.92f
        val sw = r * 0.08f
        val topLeft = Offset(cx - r, cy - r)
        val ballSize = Size(r * 2, r * 2)

        drawArc(Color.White, 0f, 180f, useCenter = true, topLeft = topLeft, size = ballSize)
        drawArc(Color(0xFFE53935), 180f, 180f, useCenter = true, topLeft = topLeft, size = ballSize)
        drawLine(Color.Black, Offset(cx - r, cy), Offset(cx + r, cy), sw * 1.3f)
        drawCircle(Color.Black, r * 0.24f, Offset(cx, cy))
        drawCircle(Color.White, r * 0.16f, Offset(cx, cy))
        drawCircle(Color(0xFFCCCCCC), r * 0.09f, Offset(cx - r * 0.04f, cy - r * 0.04f))
        drawCircle(Color.Black, r, style = Stroke(sw))
    }
}

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
private fun LoadingContent(modifier: Modifier = Modifier) {
    GameLoadingContent(
        text = stringResource(R.string.wildcatch_loading),
        modifier = modifier,
        textColor = Color.White.copy(alpha = 0.85f),
        spinnerColor = Color.White
    )
}

@Composable
private fun ThrowingContent(
    state: WildCatchGameState.Throwing,
    modifier: Modifier = Modifier,
    onThrow: (Float) -> Unit
) {
    val soundManager: SoundManager = koinInject()
    val ringAnim = remember(state.pokemonCount) { Animatable(1f) }

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

    // Full-screen Box — gesture covers the whole content area so any swipe registers
    Box(
        modifier = modifier
            .pointerInput(isThrown) {
                if (isThrown) return@pointerInput
                awaitEachGesture {
                    // awaitFirstDown has no slop — fires on the very first touch event
                    val down = awaitFirstDown(requireUnconsumed = false)
                    velocityTracker.resetTracking()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                    isDragging = true

                    var prev = down.position
                    // Authoritative running position, tracked synchronously in this (restricted-
                    // suspend) gesture loop. The animatable update below still has to be launched
                    // on a separate coroutine — awaitEachGesture's scope can't call arbitrary
                    // suspend functions like Animatable.snapTo directly — but launching one
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
                        // Velocity check only — no displacement check (displacement was causing the bug
                        // because snapTo is async and ballY.value could still be 0 at onDragEnd time)
                        if (velocity.y < -300f) {
                            isThrown = true
                            val captured = ringAnim.value
                            coroutineScope.launch {
                                soundManager.play(SoundManager.Sound.BUTTON_CLICK)
                                val targetX = ringCenterRoot.x - ballCenterRoot.x
                                val targetY = ringCenterRoot.y - ballCenterRoot.y
                                launch { ballScale.animateTo(0.4f, tween(450)) }
                                launch { ballX.animateTo(targetX, tween(450, easing = FastOutSlowInEasing)) }
                                ballY.animateTo(targetY, tween(450, easing = FastOutSlowInEasing))
                                delay(80)
                                onThrow(captured)
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
                                Text(
                                    text = if (i < state.lives) "❤️" else "🖤",
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Text(
                            "#${state.pokemonCount}  •  ${state.catches} caught",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
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
                    PbChip(bestScore = state.bestScore, currentScore = state.score)
                }
            }

            ComboLabel(combo = state.streak)

            Text(
                text = state.pokemon.name.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            if (LegendaryPokemon.isLegendary(state.pokemon.id)) {
                Spacer(Modifier.height(2.dp))
                LegendaryBadge()
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.pokemon.types.forEach { type -> TypeChip(type) }
            }

            Spacer(Modifier.height(16.dp))

            // Pokemon + shrinking catch ring
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .onGloballyPositioned { coords ->
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
                    modifier = Modifier.size(190.dp),
                    contentScale = ContentScale.Fit
                )
                Canvas(modifier = Modifier.size(220.dp)) {
                    val radius = (size.minDimension / 2f) * ringAnim.value
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

            // Ground shadow
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            Spacer(Modifier.height(12.dp))

            // Accuracy zone hint — windows tighten as more Pokémon appear
            val thresholds = remember(state.pokemonCount) {
                WildCatchDifficulty.ringThresholds(state.pokemonCount)
            }
            val hintText = when {
                ringAnim.value <= thresholds.perfect -> "✨  Perfect!"
                ringAnim.value <= thresholds.great -> "🎯  Great"
                ringAnim.value <= thresholds.nice -> "👍  Nice"
                else -> "Wait for the ring..."
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ringColor.copy(alpha = 0.18f))
                    .padding(horizontal = 16.dp, vertical = 5.dp)
            ) {
                Text(hintText, style = MaterialTheme.typography.bodyMedium, color = ringColor, fontWeight = FontWeight.Bold)
            }
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
            // Colored glow ring while dragging — shows current catch zone
            if (isDragging) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = ringColor.copy(alpha = 0.5f), radius = size.minDimension / 2f, style = Stroke(4.dp.toPx()))
                    drawCircle(color = ringColor.copy(alpha = 0.1f), radius = size.minDimension / 2f)
                }
            }
            PokeballCanvas(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        rotationZ = ballX.value * 0.45f
                        scaleX = ballScale.value
                        scaleY = ballScale.value
                    }
            )
        }

        // ── Throw hint ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .height(18.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isDragging && !isThrown) {
                Text(
                    "Swipe up anywhere to throw!",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ShakeResultContent(
    state: WildCatchGameState.ShakeResult,
    modifier: Modifier = Modifier,
    onNext: () -> Unit,
    onRevive: () -> Unit = {},
    isOnline: Boolean = true,
    isPremium: Boolean = false
) {
    val soundManager: SoundManager = koinInject()
    val shakeAnim = remember { Animatable(0f) }
    var showResult by remember { mutableStateOf(false) }
    val spriteAlpha = remember { Animatable(1f) }
    val spriteOffsetY = remember { Animatable(0f) }

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
        repeat(state.shakeCount) { i ->
            shakeAnim.animateTo(15f, tween(75))
            shakeAnim.animateTo(-15f, tween(75))
            soundManager.play(SoundManager.Sound.CARD_FLIP, 0.65f)
            if (i < state.shakeCount - 1) delay(90L)
        }
        shakeAnim.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
        delay(110L)
        soundManager.play(
            if (state.caught) {
                if (state.lifeRecovered) SoundManager.Sound.CORRECT_ANSWER else SoundManager.Sound.MATCH_FOUND
            } else SoundManager.Sound.WRONG_ANSWER
        )
        showResult = true
        if (state.caught) {
            spriteAlpha.animateTo(0.2f, tween(350))
        } else {
            launch { spriteAlpha.animateTo(0f, tween(420)) }
            spriteOffsetY.animateTo(-38f, tween(420))
        }
    }

    val resultColor = if (state.caught) Color(0xFF43A047) else Color(0xFFE53935)
    val resultText = when {
        state.lifeRecovered -> "Gotcha! ❤️ +1 Life!"
        state.caught -> "Gotcha! 🎉"
        else -> "Oh no! It fled!"
    }

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
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
            Text(state.accuracy.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }

        Spacer(Modifier.height(20.dp))

        // Pokemon sprite — ghost when caught, floats away when fled
        AsyncImage(
            model = state.pokemon.spriteUrl,
            contentDescription = state.pokemon.name,
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    alpha = spriteAlpha.value
                    translationY = spriteOffsetY.value.dp.toPx()
                },
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(16.dp))

        // Shaking Pokéball
        PokeballCanvas(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer {
                    val s = 1f + shakeAnim.value / 120f
                    scaleX = s; scaleY = s
                }
                .offset { IntOffset(shakeAnim.value.dp.roundToPx(), 0) }
        )

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
                if (LegendaryPokemon.isLegendary(state.pokemon.id)) {
                    Spacer(Modifier.height(6.dp))
                    LegendaryBadge()
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Lives + score row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(WildCatchViewModel.MAX_LIVES) { i ->
                    Text(if (i < state.lives) "❤️" else "🖤", fontSize = 18.sp)
                }
            }
            Text("${state.score} pts", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFFFFD600))
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
                    contentColor = Color(0xFFFFD600)
                ),
                border = BorderStroke(1.5.dp, Color(0xFFFFD600).copy(alpha = 0.6f))
            ) {
                Text(
                    if (isPremium) "Revive  ❤️" else "📺  Watch Ad to Revive  ❤️",
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
                if (state.lives <= 0) "See Results" else "Next Pokémon →",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FinishedContent(
    state: WildCatchGameState.Finished,
    modifier: Modifier = Modifier,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit
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

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PokeballCanvas(modifier = Modifier.size(88.dp))

        Spacer(Modifier.height(20.dp))

        Text("Wild Catch Over!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
        if (state.isNewBest) {
            Spacer(Modifier.height(6.dp))
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFFD700).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
            ) {
                Text(
                    "🏆 NEW BEST!",
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
            repeat(3) { index -> Text(if (index < stars) "⭐" else "☆", fontSize = 36.sp) }
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
                    "Catch rate: ${(catchRate * 100).toInt()}%",
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
            Text("Play Again", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = Color.White)
        ) {
            Text("Back to Games", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
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
        Text(type.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
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

