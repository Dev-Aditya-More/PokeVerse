package com.aditya1875.pokeverse.feature.game.survivor.presentation.screens

import com.aditya1875.pokeverse.R
import androidx.compose.ui.res.stringResource
import com.aditya1875.pokeverse.utils.localizedTypeName
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aditya1875.pokeverse.feature.game.survivor.domain.model.MatchupPokemon
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aditya1875.pokeverse.feature.core.ui.components.LegendaryBadge
import com.aditya1875.pokeverse.feature.core.ui.components.NoInternetScreen
import com.aditya1875.pokeverse.feature.game.core.data.ads.IRewardedAdManager
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.core.presentation.ComboLabel
import com.aditya1875.pokeverse.feature.game.core.presentation.GameLoadingContent
import com.aditya1875.pokeverse.feature.game.core.presentation.GameResultLayout
import com.aditya1875.pokeverse.feature.game.core.presentation.LivesRow
import com.aditya1875.pokeverse.feature.game.core.presentation.PbChip
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultHeroIcon
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatChips
import com.aditya1875.pokeverse.feature.game.core.presentation.requestRewardedAd
import com.aditya1875.pokeverse.feature.game.survivor.domain.model.Modifier as SurvivorModifier
import com.aditya1875.pokeverse.feature.game.survivor.domain.model.WeatherKind
import com.aditya1875.pokeverse.feature.game.survivor.domain.state.SurvivorGameState
import com.aditya1875.pokeverse.feature.game.survivor.presentation.viewmodels.SurvivorViewModel
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import com.aditya1875.pokeverse.utils.ConnectivityObserver
import com.aditya1875.pokeverse.utils.LegendaryPokemon
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.aditya1875.pokeverse.utils.SoundManager
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.aditya1875.pokeverse.feature.game.core.presentation.backdrop.GameBackdrop
import com.aditya1875.pokeverse.feature.game.core.presentation.backdrop.GameScene
import com.aditya1875.pokeverse.feature.game.core.presentation.backdrop.BackdropPulse
import com.aditya1875.pokeverse.feature.game.core.presentation.backdrop.SkyWeather

private val AmberAccent = Color(0xFFFFC107)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurvivorScreen(
    onBack: () -> Unit,
    viewModel: SurvivorViewModel = koinViewModel()
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    var pendingXp by remember { mutableStateOf<XPResult?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    val hasActiveProgress = gameState is SurvivorGameState.Playing || gameState is SurvivorGameState.RoundResult
    val requestExit: () -> Unit = { if (hasActiveProgress) showExitDialog = true else onBack() }

    val connectivityObserver: ConnectivityObserver = koinInject()
    val isOnline by connectivityObserver.isOnline.collectAsState(initial = true)
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()
    val isPremium = subscriptionState is SubscriptionState.Premium

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var guideChecked by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showGuide = !ScreenStateManager.isSurvivorGuideSeen(context)
        guideChecked = true
    }

    LaunchedEffect(Unit) {
        viewModel.xpResult.collect { pendingXp = it }
    }

    // Held off until the "how to play" guide has been dismissed (or was already seen).
    LaunchedEffect(isOnline, guideChecked, showGuide) {
        if (isOnline && guideChecked && !showGuide && gameState is SurvivorGameState.Idle) {
            viewModel.startGame()
        }
    }

    if (!isOnline && gameState is SurvivorGameState.Idle) {
        NoInternetScreen(onRetry = { viewModel.startGame() })
        return
    }

    BackHandler(enabled = hasActiveProgress) { showExitDialog = true }

    val activeRound = (gameState as? SurvivorGameState.Playing)?.round
        ?: (gameState as? SurvivorGameState.RoundResult)?.round
    val sky = when (activeRound?.activeModifiers?.filterIsInstance<SurvivorModifier.Weather>()?.firstOrNull()?.kind) {
        WeatherKind.RAIN -> SkyWeather.RAIN
        WeatherKind.SUN -> SkyWeather.SUN
        null -> SkyWeather.CLEAR
    }
    val roundResult = gameState as? SurvivorGameState.RoundResult

    XPOverlay(result = pendingXp, onDismiss = { pendingXp = null }) {
        GameBackdrop(
            scene = GameScene.Weather(sky),
            pulseKey = roundResult,
            pulseColor = if (roundResult?.wasCorrect == true) BackdropPulse.Correct else BackdropPulse.Wrong
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.game_name_survivor_title), color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = requestExit) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showGuide = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = stringResource(R.string.game_how_to_play),
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
                        // Animate only when the *phase* changes. Keyed on the whole state, every
                        // 100 ms timer tick restarted the fade/scale transition and the screen
                        // ghosted over itself continuously — the "blurry" look.
                        contentKey = { it::class },
                        transitionSpec = {
                            (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.95f))
                                .togetherWith(fadeOut(tween(200)))
                        },
                        label = "survivor_state"
                    ) { state ->
                        when (state) {
                            is SurvivorGameState.Idle,
                            is SurvivorGameState.Loading -> GameLoadingContent(
                                text = stringResource(R.string.survivor_loading),
                                modifier = Modifier.fillMaxSize().padding(padding),
                                textColor = Color.White.copy(alpha = 0.85f),
                                spinnerColor = AmberAccent
                            )
                            is SurvivorGameState.Playing -> PlayingContent(
                                state = state,
                                modifier = Modifier.fillMaxSize().padding(padding),
                                onAnswer = { type -> viewModel.submitAnswer(type) }
                            )
                            is SurvivorGameState.RoundResult -> RoundResultContent(
                                state = state,
                                modifier = Modifier.fillMaxSize().padding(padding),
                                isOnline = isOnline,
                                isPremium = isPremium,
                                onRevive = { viewModel.reviveGame() }
                            )
                            is SurvivorGameState.Finished -> FinishedContent(
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
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.game_leave_title)) },
            text = { Text(stringResource(R.string.game_leave_message)) },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; onBack() }) {
                    Text(stringResource(R.string.game_leave), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showGuide) {
        SurvivorGuideOverlay(
            onDismiss = {
                showGuide = false
                scope.launch { ScreenStateManager.markSurvivorGuideSeen(context) }
            }
        )
    }
}

@Composable
private fun SurvivorGuideOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1710),
            border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = AmberAccent,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.survivor_guide_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(Modifier.height(18.dp))

                GuideRow(Icons.Default.Bolt, AmberAccent, stringResource(R.string.survivor_guide_types))
                Spacer(Modifier.height(14.dp))
                GuideRow(Icons.Default.WaterDrop, Color(0xFF4FC3F7), stringResource(R.string.survivor_guide_weather))
                Spacer(Modifier.height(14.dp))
                GuideRow(Icons.Default.Timer, Color(0xFFE53935), stringResource(R.string.survivor_guide_timer))
                Spacer(Modifier.height(14.dp))
                GuideRow(Icons.Default.Favorite, Color(0xFFE53935), stringResource(R.string.survivor_guide_lives))

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Text(stringResource(R.string.survivor_guide_go), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

@Composable
private fun GuideRow(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * A round's sprite is only as fast as `SurvivorViewModel`'s prefetch — this adds
 * a crossfade + a light pop-in on top so even a cache-miss doesn't feel like a
 * jarring pop, and a fresh sprite always announces itself with a little life.
 */
@Composable
private fun SpriteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val entrance = remember(url) { Animatable(0.7f) }
    val alpha = remember(url) { Animatable(0f) }
    LaunchedEffect(url) {
        launch { alpha.animateTo(1f, tween(260)) }
        entrance.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(200)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier.graphicsLayer {
            scaleX = entrance.value
            scaleY = entrance.value
            this.alpha = alpha.value
        },
        contentScale = ContentScale.Fit
    )
}

/**
 * The "wild Pokémon appeared!" reveal, timed to [SurvivorViewModel.INTRO_MS]:
 *  0–380 ms   slides in as a black silhouette onto a type-tinted battle platform
 *  420 ms     white flash burst; silhouette resolves into full colour with a bounce
 *  ~600 ms    "A wild X appeared!" + name
 *  ~650 ms+   type chips pop in one by one
 * The clock starts when it ends, so the reveal gives time to read the types.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EncounterStage(defender: MatchupPokemon) {
    val key = defender.id
    val slide = remember(key) { Animatable(1f) }      // 1 = off-screen right, 0 = in place
    val reveal = remember(key) { Animatable(0f) }     // 0 = silhouette, 1 = full colour
    val flash = remember(key) { Animatable(0f) }
    val bounce = remember(key) { Animatable(1f) }
    val textIn = remember(key) { Animatable(0f) }
    val chipsShown = remember(key) { mutableStateOf(0) }
    val platformColor = typeColor(defender.types.firstOrNull().orEmpty())

    LaunchedEffect(key) {
        slide.animateTo(0f, tween(380, easing = FastOutSlowInEasing))
        delay(40)
        launch {
            flash.animateTo(1f, tween(90))
            flash.animateTo(0f, tween(220))
        }
        launch { reveal.animateTo(1f, tween(180)) }
        bounce.snapTo(1.12f)
        launch { bounce.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
        delay(140)
        launch { textIn.animateTo(1f, tween(200)) }
        delay(60)
        repeat(defender.types.size) {
            chipsShown.value = it + 1
            delay(90)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(width = 240.dp, height = 200.dp), contentAlignment = Alignment.BottomCenter) {
            // Battle platform: an oval glowing in the defender's primary type colour.
            Canvas(modifier = Modifier.size(width = 220.dp, height = 46.dp)) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(platformColor.copy(alpha = 0.55f), platformColor.copy(alpha = 0.08f), Color.Transparent),
                        center = center,
                        radius = size.width / 2
                    )
                )
                drawOval(color = platformColor.copy(alpha = 0.5f), style = Stroke(width = 2.dp.toPx()))
            }
            Box(
                modifier = Modifier
                    .padding(bottom = 18.dp)
                    .size(170.dp)
                    .graphicsLayer {
                        translationX = slide.value * size.width * 1.6f
                        scaleX = bounce.value
                        scaleY = bounce.value
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
            ) {
                val request = ImageRequest.Builder(LocalContext.current).data(defender.spriteUrl).build()
                // Silhouette underneath, full colour fading in on top: "Who's that…" then the reveal.
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(0xFF0B0A12)),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                AsyncImage(
                    model = request,
                    contentDescription = defender.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = reveal.value }
                )
            }
            // Reveal flash: a white burst behind/over the Pokémon.
            if (flash.value > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension * (0.25f + 0.5f * flash.value)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.85f * flash.value), Color.Transparent),
                            center = Offset(center.x, center.y - 10.dp.toPx()),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(center.x, center.y - 10.dp.toPx())
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = textIn.value
                translationY = (1f - textIn.value) * 12.dp.toPx()
            }
        ) {
            Text(
                stringResource(R.string.survivor_wild_appeared, defender.displayName()),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.65f)
            )
            Text(
                text = defender.displayName(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            if (LegendaryPokemon.isLegendary(defender.id)) {
                Spacer(Modifier.height(2.dp))
                LegendaryBadge()
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(26.dp)) {
            defender.types.take(chipsShown.value).forEach { type -> PopIn { TypeChip(type) } }
        }
    }
}

private fun MatchupPokemon.displayName() = name.replaceFirstChar { it.uppercase() }

/** Springs its content in from small — used for type chips and answers appearing. */
@Suppress("EffectKeys")
@Composable
private fun PopIn(delayMs: Long = 0L, content: @Composable () -> Unit) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        launch { alpha.animateTo(1f, tween(140)) }
        scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }
    Box(modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value }) {
        content()
    }
}

@Composable
private fun ModifierBadge(modifiers: List<SurvivorModifier>) {
    if (modifiers.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        modifiers.forEach { modifier ->
            val (emoji, label) = when (modifier) {
                is SurvivorModifier.Weather -> when (modifier.kind) {
                    WeatherKind.RAIN -> "🌧" to stringResource(R.string.survivor_weather_rain)
                    WeatherKind.SUN -> "☀️" to stringResource(R.string.survivor_weather_sun)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmberAccent.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    "$emoji $label",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AmberAccent
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayingContent(
    state: SurvivorGameState.Playing,
    modifier: Modifier = Modifier,
    onAnswer: (String) -> Unit
) {
    val round = state.round
    val fraction = (state.timeRemainingMs.toFloat() / round.timeBudgetMs.toFloat()).coerceIn(0f, 1f)
    val timerColor = lerp(Color(0xFFE53935), Color(0xFF43A047), fraction)

    Column(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LivesRow(lives = state.lives, maxLives = 3)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.game_points_short, state.score),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AmberAccent
                )
                PbChip(bestScore = state.bestScore, currentScore = state.score)
            }
        }

        Spacer(Modifier.height(4.dp))
        ComboLabel(combo = state.streak)

        // Countdown bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(timerColor)
            )
        }

        Spacer(Modifier.height(16.dp))

        ModifierBadge(round.activeModifiers)
        Spacer(Modifier.height(10.dp))

        EncounterStage(round.defender)

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.survivor_whats_super_effective),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(10.dp))

        // Answers appear (and unlock) the moment the reveal ends and the clock starts.
        Box(modifier = Modifier.heightIn(min = 120.dp)) {
            if (!state.isIntro) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    round.options.forEachIndexed { index, type ->
                        PopIn(delayMs = index * 45L) { AnswerChip(type = type, onClick = { onAnswer(type) }) }
                    }
                }
            }
        }
    }
}

@Suppress("EffectKeys")
@Composable
private fun RoundResultContent(
    state: SurvivorGameState.RoundResult,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
    isPremium: Boolean = false,
    onRevive: () -> Unit = {}
) {
    val soundManager: SoundManager = koinInject()
    val adManager: IRewardedAdManager = koinInject()
    val adState by adManager.adState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        soundManager.play(if (state.wasCorrect) SoundManager.Sound.CORRECT_ANSWER else SoundManager.Sound.WRONG_ANSWER)
    }

    LaunchedEffect(adState, isOnline, isPremium) {
        if (!isPremium && isOnline && state.lives <= 0 && adState is RewardedAdState.Idle) {
            adManager.loadAd(context)
        }
    }

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SpriteImage(
            url = state.round.defender.spriteUrl,
            contentDescription = state.round.defender.name,
            modifier = Modifier.size(160.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(
                when {
                    state.wasCorrect -> R.string.survivor_result_correct
                    state.selectedType == null -> R.string.survivor_result_timeout
                    else -> R.string.survivor_result_wrong
                }
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (state.wasCorrect) Color(0xFF43A047) else Color(0xFFE53935)
        )

        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.survivor_right_call, state.round.correctTypes.map { localizedTypeName(it) }.joinToString(" / ")),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            LivesRow(lives = state.lives, maxLives = 3)
        }

        Spacer(Modifier.height(20.dp))

        if (state.lives <= 0) {
            OutlinedButton(
                onClick = {
                    if (isPremium) onRevive()
                    else requestRewardedAd(context, activity, adManager, adState) { onRevive() }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAccent),
                border = BorderStroke(1.5.dp, AmberAccent.copy(alpha = 0.6f))
            ) {
                Text(
                    stringResource(if (isPremium) R.string.survivor_revive else R.string.survivor_revive_ad),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FinishedContent(
    state: SurvivorGameState.Finished,
    modifier: Modifier = Modifier,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit
) {
    val soundManager: SoundManager = koinInject()
    val stars = when {
        state.bestStreak >= 20 -> 3
        state.bestStreak >= 10 -> 2
        else -> 1
    }

    LaunchedEffect(Unit) {
        soundManager.play(if (stars >= 2) SoundManager.Sound.GAME_WIN else SoundManager.Sound.GAME_LOSE)
    }

    Box(modifier = modifier) {
        GameResultLayout(
            title = stringResource(R.string.survivor_result_title),
            subtitle = stringResource(R.string.survivor_result_subtitle, state.roundsPlayed),
            score = state.score.toString(),
            scoreLabel = stringResource(R.string.game_score_label_points),
            heroColor = AmberAccent,
            stars = stars,
            isNewBest = state.isNewBest,
            onPlayAgain = onPlayAgain,
            onBack = onBack,
            heroContent = { ResultHeroIcon(icon = Icons.Default.Bolt, heroColor = AmberAccent) },
            statsContent = {
                ResultStatChips(
                    stringResource(R.string.game_stat_best_streak) to "${state.bestStreak}",
                    stringResource(R.string.game_stat_rounds) to "${state.roundsPlayed}",
                )
            }
        )
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

@Composable
private fun AnswerChip(type: String, onClick: () -> Unit) {
    val color = typeColor(type)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            localizedTypeName(type),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
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
