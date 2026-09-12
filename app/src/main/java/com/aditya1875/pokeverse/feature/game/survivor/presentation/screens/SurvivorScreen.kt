package com.aditya1875.pokeverse.feature.game.survivor.presentation.screens

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

private val SurvivorBg = Brush.verticalGradient(
    colors = listOf(Color(0xFF1A1206), Color(0xFF130D1E), Color(0xFF0C0A14))
)
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

    XPOverlay(result = pendingXp, onDismiss = { pendingXp = null }) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pokémon Survivor", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = requestExit) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showGuide = true }) {
                            Icon(
                                Icons.Default.HelpOutline,
                                contentDescription = "How to play",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color(0xFF130D1E)
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurvivorBg)
            ) {
                AnimatedContent(
                    targetState = gameState,
                    transitionSpec = {
                        (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.95f))
                            .togetherWith(fadeOut(tween(200)))
                    },
                    label = "survivor_state"
                ) { state ->
                    when (state) {
                        is SurvivorGameState.Idle,
                        is SurvivorGameState.Loading -> GameLoadingContent(
                            text = "A wild Pokémon is approaching…",
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

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Leave game?") },
            text = { Text("Your run will be lost if you exit now.") },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; onBack() }) {
                    Text("Leave", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Cancel") } }
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
                    "How to Survive",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(Modifier.height(18.dp))

                GuideRow(Icons.Default.Bolt, AmberAccent, "A wild Pokémon appears — tap the type that's super effective against it.")
                Spacer(Modifier.height(14.dp))
                GuideRow(Icons.Default.WaterDrop, Color(0xFF4FC3F7), "Watch for weather — Rain boosts Water and weakens Fire, Sun does the opposite.")
                Spacer(Modifier.height(14.dp))
                GuideRow(Icons.Default.Timer, Color(0xFFE53935), "Answer before the bar runs out — a timeout counts as a miss.")
                Spacer(Modifier.height(14.dp))
                GuideRow(Icons.Default.Favorite, Color(0xFFE53935), "You've got 3 lives. Chain correct answers for streak bonuses and XP.")

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Text("Let's go!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
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

@Composable
private fun ModifierBadge(modifiers: List<SurvivorModifier>) {
    if (modifiers.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        modifiers.forEach { modifier ->
            val emoji = when (modifier) {
                is SurvivorModifier.Weather -> when (modifier.kind) {
                    WeatherKind.RAIN -> "🌧"
                    WeatherKind.SUN -> "☀️"
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmberAccent.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    "$emoji ${modifier.label}",
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
                    "${state.score} pts",
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

        SpriteImage(
            url = round.defender.spriteUrl,
            contentDescription = round.defender.name,
            modifier = Modifier.size(180.dp)
        )

        Text(
            text = round.defender.name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        if (LegendaryPokemon.isLegendary(round.defender.id)) {
            Spacer(Modifier.height(2.dp))
            LegendaryBadge()
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            round.defender.types.forEach { type -> TypeChip(type) }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "What's super effective?",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            round.options.forEach { type ->
                AnswerChip(type = type, onClick = { onAnswer(type) })
            }
        }
    }
}

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
            text = if (state.wasCorrect) "Super effective! 🎯" else if (state.selectedType == null) "Too slow!" else "Not effective enough…",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (state.wasCorrect) Color(0xFF43A047) else Color(0xFFE53935)
        )

        Spacer(Modifier.height(6.dp))
        Text(
            "The right call: ${state.round.correctTypes.joinToString(" / ") { it.replaceFirstChar { c -> c.uppercase() } }}",
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
                    if (isPremium) "Revive  ❤️" else "📺  Watch Ad to Revive  ❤️",
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
            title = "Run Over!",
            subtitle = "You survived ${state.roundsPlayed} rounds",
            score = state.score.toString(),
            scoreLabel = "points",
            heroColor = AmberAccent,
            stars = stars,
            isNewBest = state.isNewBest,
            onPlayAgain = onPlayAgain,
            onBack = onBack,
            heroContent = { ResultHeroIcon(icon = Icons.Default.Bolt, heroColor = AmberAccent) },
            statsContent = {
                ResultStatChips(
                    "Best Streak" to "${state.bestStreak}",
                    "Rounds" to "${state.roundsPlayed}",
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
        Text(type.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
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
            type.replaceFirstChar { it.uppercase() },
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
