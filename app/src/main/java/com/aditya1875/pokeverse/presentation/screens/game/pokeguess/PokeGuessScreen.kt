package com.aditya1875.pokeverse.presentation.screens.game.pokeguess

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.aditya1875.pokeverse.presentation.screens.game.pokeguess.components.GuessDifficulty
import com.aditya1875.pokeverse.presentation.screens.game.pokeguess.components.GuessGameState
import com.aditya1875.pokeverse.presentation.screens.home.components.Route
import com.aditya1875.pokeverse.presentation.ui.viewmodel.PokeGuessViewModel
import com.aditya1875.pokeverse.utils.SoundManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PokeGuessGameScreen(
    difficulty: GuessDifficulty,
    onBack: () -> Unit,
    viewModel: PokeGuessViewModel = koinViewModel()
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val soundManager: SoundManager = koinInject()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(gameState) {
        when (val state = gameState) {
            is GuessGameState.Revealing -> {
                when {
                    state.isTimeUp -> {
                        soundManager.play(SoundManager.Sound.TIMER_UP)
                    }
                    state.isCorrect -> {
                        soundManager.play(SoundManager.Sound.CORRECT_ANSWER)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    else -> {
                        soundManager.play(SoundManager.Sound.WRONG_ANSWER)
                    }
                }
            }
            else -> {}
        }
    }

    LaunchedEffect(difficulty) {
        viewModel.startGame(difficulty)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetGame()
        }
    }

    Scaffold(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary
                    )
                )
            ).navigationBarsPadding()

    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            when (val state = gameState) {
                is GuessGameState.Idle -> {}
                is GuessGameState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularWavyProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                is GuessGameState.ShowingSilhouette -> {
                    SilhouetteScreen(
                        state = state,
                        timeUp = state.timeRemaining <= 0,
                        onAnswerSelected = { answer ->
                            if (state.timeRemaining > 0) {
                                viewModel.submitAnswer(answer, state.currentQuestionIndex, difficulty)
                            }
                        },
                        onBack = onBack
                    )
                }

                is GuessGameState.Revealing -> {
                    RevealScreen(
                        state = state,
                        onNext = { viewModel.nextQuestion(difficulty) },
                        difficulty = difficulty
                    )
                }

                is GuessGameState.Finished -> {
                    PokeGuessResultScreen(
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
}

@Composable
private fun SilhouetteScreen(
    state: GuessGameState.ShowingSilhouette,
    timeUp: Boolean,
    onAnswerSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Question ${state.currentQuestionIndex + 1}/${state.totalQuestions}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Score: ${state.score}",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Timer
        GuessTimer(timeRemaining = state.timeRemaining)

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Who's That Pokémon?",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Yellow,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(state.question.spriteUrl)
                    .crossfade(true)
                    .build()
            )

            Image(
                painter = painter,
                contentDescription = "Pokemon silhouette",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
                    .graphicsLayer {
                        alpha = 1f
                        colorFilter = ColorFilter.tint(
                            Color.Black,
                            blendMode = BlendMode.SrcAtop
                        )
                    },
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.weight(1f))

        state.question.options.forEach { option ->
            PokeGuessOption(
                text = option.replaceFirstChar { it.uppercase() },
                enabled = !timeUp,
                onClick = { onAnswerSelected(option) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GuessTimer(timeRemaining: Int) {
    val color by animateColorAsState(
        targetValue = when {
            timeRemaining <= 3 -> Color(0xFFFF1744)
            timeRemaining <= 5 -> Color(0xFFFF9800)
            else -> Color.Yellow
        },
        label = "timer_color"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${timeRemaining}s",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun PokeGuessOption(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                Color.White.copy(alpha = 0.15f)
            else
                Color.Gray.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = if (enabled) 1f else 0.5f)
            )
        }
    }
}

@Composable
private fun RevealScreen(
    state: GuessGameState.Revealing,
    onNext: () -> Unit,
    difficulty: GuessDifficulty
) {
    var showReveal by remember { mutableStateOf(false) }

    val titleText = when {
        state.isTimeUp -> "Time's up!"
        state.isCorrect -> "It's ${state.question.pokemonName.replaceFirstChar { it.uppercase() }}!"
        else -> "It was ${state.question.pokemonName.replaceFirstChar { it.uppercase() }}!"
    }

    val resultText = when {
        state.isTimeUp -> "Too slow!"
        state.isCorrect -> "Correct!"
        else -> "Wrong!"
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        showReveal = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))

        AnimatedVisibility(
            visible = showReveal,
            enter = fadeIn() + scaleIn()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        state.isTimeUp -> Color(0xFFFF9800)   // orange for timeout
                        state.isCorrect -> Color(0xFF4CAF50)
                        else -> Color(0xFFFF1744)
                    },
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(30.dp))

                // Revealed Pokemon
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.isCorrect)
                                Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else
                                Color(0xFFFF1744).copy(alpha = 0.2f)
                        )
                        .border(
                            width = 4.dp,
                            color = if (state.isCorrect) Color(0xFF4CAF50) else Color(0xFFFF1744),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(state.question.spriteUrl)
                            .crossfade(true)
                            .build()
                    )

                    Image(
                        painter = painter,
                        contentDescription = state.question.pokemonName,
                        modifier = Modifier.fillMaxSize(0.8f),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(Modifier.height(18.dp))

                Icon(
                    imageVector = when {
                        state.isTimeUp -> Icons.Default.Timer
                        state.isCorrect -> Icons.Default.Check
                        else -> Icons.Default.Close
                    },
                    contentDescription = null,
                    tint = when {
                        state.isTimeUp -> Color(0xFFFF9800)
                        state.isCorrect -> Color(0xFF4CAF50)
                        else -> Color(0xFFFF1744)
                    },
                    modifier = Modifier.size(60.dp)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = resultText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(bottom = 20.dp)
            ,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Yellow,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (state.currentQuestionIndex >= state.totalQuestions - 1)
                    "See Results"
                else
                    "Next Pokémon",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}