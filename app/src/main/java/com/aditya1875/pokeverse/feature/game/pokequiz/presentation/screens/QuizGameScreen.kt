package com.aditya1875.pokeverse.feature.game.pokequiz.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.game.pokequiz.domain.model.QuizDifficulty
import com.aditya1875.pokeverse.feature.game.pokequiz.domain.model.QuizGameState
import com.aditya1875.pokeverse.feature.game.pokequiz.domain.model.QuizUiState
import com.aditya1875.pokeverse.feature.game.pokequiz.presentation.components.QuizResultScreen
import com.aditya1875.pokeverse.feature.game.pokequiz.presentation.viewmodels.QuizViewModel
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import com.aditya1875.pokeverse.utils.SoundManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val OPTION_LABELS = listOf("A", "B", "C", "D")

@Composable
fun QuizGameScreen(
    difficulty: QuizDifficulty,
    onBack: () -> Unit,
    viewModel: QuizViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val soundManager: SoundManager = koinInject()
    var pendingXp by remember { mutableStateOf<XPResult?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.xpResult.collect { pendingXp = it } }
    BackHandler { showExitDialog = true }
    LaunchedEffect(difficulty) { viewModel.startQuiz(difficulty) }
    DisposableEffect(Unit) { onDispose { viewModel.resetQuiz() } }

    XPOverlay(result = pendingXp, onDismiss = { pendingXp = null }) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) { paddingValues ->
            when (val state = uiState) {
                is QuizUiState.Idle -> {}
                is QuizUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("🧠", fontSize = 48.sp)
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                            Text("Loading questions…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                is QuizUiState.Playing -> QuizPlayingContent(
                    gameState = state.gameState,
                    difficulty = difficulty,
                    onAnswerSelected = { viewModel.selectAnswer(it) },
                    onRequestExit = { showExitDialog = true },
                    modifier = Modifier.padding(paddingValues)
                )
                is QuizUiState.ShowingAnswer -> {
                    LaunchedEffect(state.isCorrect) {
                        soundManager.play(
                            if (state.isCorrect) SoundManager.Sound.CORRECT_ANSWER
                            else SoundManager.Sound.WRONG_ANSWER
                        )
                    }
                    QuizAnswerFeedbackContent(
                        gameState = state.gameState,
                        selectedAnswerIndex = state.selectedAnswerIndex,
                        isCorrect = state.isCorrect,
                        explanation = state.explanation,
                        onNext = { viewModel.nextQuestion() }
                    )
                }
                is QuizUiState.Finished -> QuizResultScreen(
                    score = state.score,
                    correctAnswers = state.correctAnswers,
                    totalQuestions = state.totalQuestions,
                    difficulty = state.difficulty,
                    stars = state.stars,
                    onPlayAgain = { viewModel.startQuiz(difficulty) },
                    onBackToMenu = { viewModel.onBackToMenu(); onBack() }
                )
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit quiz?") },
            text = { Text("Your progress will be lost.") },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; onBack() }) {
                    Text("Exit", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun QuizPlayingContent(
    gameState: QuizGameState,
    difficulty: QuizDifficulty,
    onAnswerSelected: (Int) -> Unit,
    onRequestExit: () -> Unit,
    modifier: Modifier
) {
    val currentQuestion = gameState.questions[gameState.currentQuestionIndex]
    val timerFraction = gameState.timeRemaining.toFloat() / gameState.totalTimePerQuestion
    val timerColor by animateColorAsState(
        targetValue = when {
            gameState.timeRemaining <= 5 -> Color(0xFFFF1744)
            gameState.timeRemaining <= 10 -> Color(0xFFFF9800)
            else -> MaterialTheme.colorScheme.primary
        }, label = "tc"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRequestExit, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Close, null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${gameState.currentQuestionIndex + 1} / ${gameState.questions.size}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "${gameState.score}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Timer
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LinearProgressIndicator(
                progress = { timerFraction },
                modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = timerColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                "${gameState.timeRemaining}s",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = timerColor,
                modifier = Modifier.width(32.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Question card ─────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentQuestion.question,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Answer options ────────────────────────────────────────────────────
        currentQuestion.options.forEachIndexed { index, option ->
            QuizAnswerOption(
                text = option,
                label = OPTION_LABELS[index],
                onClick = { onAnswerSelected(index) }
            )
            if (index < currentQuestion.options.size - 1) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun QuizAnswerOption(
    text: String,
    label: String,
    onClick: () -> Unit,
    soundManager: SoundManager = koinInject()
) {
    val interactionColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            soundManager.play(SoundManager.Sound.BUTTON_CLICK); onClick()
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Label circle
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(interactionColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = interactionColor
                )
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ANSWER FEEDBACK screen — redesigned with full-bleed colour sweep
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuizAnswerFeedbackContent(
    gameState: QuizGameState,
    selectedAnswerIndex: Int,
    isCorrect: Boolean,
    explanation: String,
    onNext: () -> Unit
) {
    val currentQuestion = gameState.questions[gameState.currentQuestionIndex]
    val accentColor = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF1744)

    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Colour sweep
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.42f)
                .background(Brush.verticalGradient(
                    listOf(accentColor.copy(alpha = 0.1f), Color.Transparent)
                ))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Result icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(entrance.value)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isCorrect) "Correct!" else "Incorrect",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = accentColor,
                modifier = Modifier.alpha(entrance.value)
            )

            if (!isCorrect) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Black)
                        Text(
                            currentQuestion.options[currentQuestion.correctAnswerIndex],
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Explanation card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lightbulb, null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp))
                        Text(
                            "Did you know?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                val isLast = gameState.currentQuestionIndex >= gameState.questions.size - 1
                Text(
                    if (isLast) "See Results →" else "Next Question →",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}