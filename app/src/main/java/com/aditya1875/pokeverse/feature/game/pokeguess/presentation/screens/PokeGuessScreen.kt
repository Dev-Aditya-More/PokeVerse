package com.aditya1875.pokeverse.feature.game.pokeguess.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.aditya1875.pokeverse.feature.game.pokeguess.domain.model.GuessDifficulty
import com.aditya1875.pokeverse.feature.game.pokeguess.domain.state.GuessGameState
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.game.pokeguess.presentation.components.PokeGuessResultScreen
import com.aditya1875.pokeverse.feature.game.pokeguess.presentation.viewmodels.PokeGuessViewModel
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import com.aditya1875.pokeverse.utils.SoundManager
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun PokeGuessGameScreen(
    difficulty: GuessDifficulty,
    onBack: () -> Unit,
    viewModel: PokeGuessViewModel = koinViewModel()
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val soundManager: SoundManager = koinInject()
    var pendingXp by remember { mutableStateOf<XPResult?>(null) }

    LaunchedEffect(Unit) { viewModel.xpResult.collect { pendingXp = it } }
    LaunchedEffect(gameState) {
        when (val s = gameState) {
            is GuessGameState.Revealing -> when {
                s.isTimeUp -> soundManager.play(SoundManager.Sound.TIMER_UP)
                s.isCorrect -> soundManager.play(SoundManager.Sound.CORRECT_ANSWER)
                else -> soundManager.play(SoundManager.Sound.WRONG_ANSWER)
            }

            else -> {}
        }
    }
    LaunchedEffect(difficulty) { viewModel.startGame(difficulty) }
    DisposableEffect(Unit) { onDispose { viewModel.resetGame() } }

    XPOverlay(result = pendingXp, onDismiss = { pendingXp = null }) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.background,
                        0.6f to MaterialTheme.colorScheme.background,
                        1f to MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
                .navigationBarsPadding()
        ) { paddingValues ->
            when (val state = gameState) {
                is GuessGameState.Idle -> {}
                is GuessGameState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            "Loading Pokémon…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is GuessGameState.ShowingSilhouette -> SilhouetteScreen(
                    state = state,
                    difficulty = difficulty,
                    onAnswerSelected = { answer ->
                        if (state.timeRemaining > 0)
                            viewModel.submitAnswer(answer, state.currentQuestionIndex, difficulty)
                    },
                    onBack = onBack,
                    modifier = Modifier.padding(paddingValues)
                )

                is GuessGameState.Revealing -> RevealScreen(
                    state = state,
                    onNext = { viewModel.nextQuestion(difficulty) }
                )

                is GuessGameState.Finished -> PokeGuessResultScreen(
                    score = state.score,
                    correctAnswers = state.correctAnswers,
                    totalQuestions = state.totalQuestions,
                    difficulty = state.difficulty,
                    onPlayAgain = { viewModel.startGame(difficulty) },
                    onBackToMenu = onBack
                )
            }
        }
    }
}

@Composable
private fun SilhouetteScreen(
    state: GuessGameState.ShowingSilhouette,
    difficulty: GuessDifficulty,
    onAnswerSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier
) {
    val timerColor by animateColorAsState(
        targetValue = when {
            state.timeRemaining <= 3 -> Color(0xFFFF1744)
            state.timeRemaining <= 5 -> Color(0xFFFF9800)
            else -> Color(0xFFFFD700)
        }, label = "tc"
    )
    val timerFraction = state.timeRemaining.toFloat() / difficulty.timePerQuestion
    val timeUp = state.timeRemaining <= 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
    ) {
        // HUD
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Close, null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${state.currentQuestionIndex + 1} / ${state.totalQuestions}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "${state.score}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Timer bar + countdown
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LinearProgressIndicator(
                progress = { timerFraction },
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = timerColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                "${state.timeRemaining}s",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = timerColor,
                modifier = Modifier.width(32.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Title
        Text(
            "Who's That Pokémon?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(16.dp))

        // Silhouette card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                val painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(state.question.spriteUrl).crossfade(true).build()
                )
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .graphicsLayer {
                            colorFilter = ColorFilter.tint(
                                Color.Black, BlendMode.SrcAtop
                            )
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Options
        state.question.options.forEachIndexed { i, option ->
            GuessOptionCard(
                text = option.split("-")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                index = i,
                enabled = !timeUp,
                onClick = { onAnswerSelected(option) }
            )
            if (i < state.question.options.size - 1) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GuessOptionCard(
    text: String,
    index: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    soundManager: SoundManager = koinInject()
) {
    val labels = listOf("A", "B", "C", "D")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                soundManager.play(SoundManager.Sound.BUTTON_CLICK); onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            else Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(if (enabled) 1.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.12f else 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    labels[index],
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.3f)
                )
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REVEAL screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RevealScreen(
    state: GuessGameState.Revealing,
    onNext: () -> Unit
) {
    val isCorrect = state.isCorrect
    val isTimeUp = state.isTimeUp

    val accentColor = when {
        isTimeUp -> Color(0xFFFF9800); isCorrect -> Color(0xFF4CAF50); else -> Color(0xFFFF1744)
    }
    val headlineText = when {
        isTimeUp -> "Time's Up!"; isCorrect -> "Correct!"; else -> "Wrong!"
    }
    val pokemonName = state.question.pokemonName.split("-")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(200); showContent = true }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        // Colour sweep from top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(
                    Brush.verticalGradient(
                        listOf(accentColor.copy(alpha = 0.1f), Color.Transparent)
                    )
                )
        )

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn() + slideInVertically { it / 6 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))

                // Result label
                Text(
                    headlineText,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(24.dp))

                // Pokémon revealed — circular frame with coloured ring
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.1f))
                        .border(3.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow radial
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(accentColor.copy(alpha = 0.12f), Color.Transparent)
                            )
                        )
                    }
                    val painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(state.question.spriteUrl).crossfade(true).build()
                    )
                    Image(
                        painter = painter,
                        contentDescription = pokemonName,
                        modifier = Modifier.fillMaxSize(0.78f),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Name
                Text(
                    "It's $pokemonName!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.weight(1f))

                // Result pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isTimeUp -> Icons.Default.Timer
                                isCorrect -> Icons.Default.Check
                                else -> Icons.Default.Close
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            when {
                                isTimeUp -> "Too slow!"; isCorrect -> "Nice one!"; else -> "Better luck next time!"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    val isLast = state.currentQuestionIndex >= state.totalQuestions - 1
                    Text(
                        if (isLast) "See Results" else "Next Pokémon",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward, null,
                        tint = Color.White, modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}