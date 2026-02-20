package com.aditya1875.pokeverse.presentation.screens.game.pokequiz


import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components.QuizDifficulty
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components.QuizGameState
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components.QuizQuestion
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components.QuizUiState
import com.aditya1875.pokeverse.presentation.ui.viewmodel.QuizViewModel
import com.aditya1875.pokeverse.utils.SoundManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun QuizGameScreen(
    difficulty: QuizDifficulty,
    onBack: () -> Unit,
    viewModel: QuizViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showExitDialog by remember { mutableStateOf(false) }

    val soundManager : SoundManager = koinInject()

    LaunchedEffect(difficulty) {
        viewModel.startQuiz(difficulty)
        soundManager.play(SoundManager.Sound.BUTTON_CLICK)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetQuiz()
        }
    }

    when (val state = uiState) {
        is QuizUiState.Idle -> {
            // Should not happen
        }
        is QuizUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is QuizUiState.Playing -> {
            QuizPlayingContent(
                gameState = state.gameState,
                onAnswerSelected = { viewModel.selectAnswer(it) },
                onBack = onBack
            )
        }
        is QuizUiState.ShowingAnswer -> {

            LaunchedEffect(state.isCorrect) {
                if (state.isCorrect) {
                    soundManager.play(SoundManager.Sound.CORRECT_ANSWER)
                } else {
                    soundManager.play(SoundManager.Sound.WRONG_ANSWER)
                }
            }

            QuizAnswerFeedbackContent(
                gameState = state.gameState,
                selectedAnswerIndex = state.selectedAnswerIndex,
                isCorrect = state.isCorrect,
                explanation = state.explanation,
                onNext = { viewModel.nextQuestion() }
            )
        }
        is QuizUiState.Finished -> {
            QuizResultScreen(
                score = state.score,
                correctAnswers = state.correctAnswers,
                totalQuestions = state.totalQuestions,
                difficulty = state.difficulty,
                stars = state.stars,
                onPlayAgain = { viewModel.startQuiz(difficulty) },
                onBackToMenu = {
                    viewModel.onBackToMenu()
                    showExitDialog = true
                }
            )
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit game?") },
            text = { Text("Are you sure you want to exit?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onBack()
                    }
                ) {
                    Text("Exit", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QuizPlayingContent(
    gameState: QuizGameState,
    onAnswerSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    val currentQuestion = gameState.questions[gameState.currentQuestionIndex]

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            // Top Bar
            QuizTopBar(
                currentQuestion = gameState.currentQuestionIndex + 1,
                totalQuestions = gameState.questions.size,
                score = gameState.score,
                onBack = onBack
            )

            // Timer
            QuizTimer(
                timeRemaining = gameState.timeRemaining,
                totalTime = gameState.totalTimePerQuestion
            )

            Spacer(Modifier.height(24.dp))

            // Question Card
            QuizQuestionCard(
                question = currentQuestion,
                onAnswerSelected = onAnswerSelected
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizTopBar(
    currentQuestion: Int,
    totalQuestions: Int,
    score: Int,
    onBack: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Question $currentQuestion/$totalQuestions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = { showExitDialog = true }) {
                Icon(Icons.Default.Close, "Exit quiz")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun QuizTimer(
    timeRemaining: Int,
    totalTime: Int
) {
    val progress = timeRemaining.toFloat() / totalTime
    val color by animateColorAsState(
        targetValue = when {
            timeRemaining <= 5 -> Color(0xFFFF1744)
            timeRemaining <= 10 -> Color(0xFFFF9800)
            else -> MaterialTheme.colorScheme.primary
        },
        label = "timer_color"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${timeRemaining}s",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun QuizQuestionCard(
    question: QuizQuestion,
    onAnswerSelected: (Int) -> Unit,
    soundManager: SoundManager = koinInject()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Question text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(20.dp),
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
        }

        Spacer(Modifier.height(32.dp))

        // Answer options
        question.options.forEachIndexed { index, option ->
            AnswerOption(
                text = option,
                index = index,
                onClick = {
                    soundManager.play(SoundManager.Sound.BUTTON_CLICK)
                    onAnswerSelected(index)
                }
            )
            if (index < question.options.size - 1) {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AnswerOption(
    text: String,
    index: Int,
    onClick: () -> Unit
) {
    val optionLabels = listOf("A", "B", "C", "D")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionLabels[index],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuizAnswerFeedbackContent(
    gameState: QuizGameState,
    selectedAnswerIndex: Int,
    isCorrect: Boolean,
    explanation: String,
    onNext: () -> Unit
) {
    val currentQuestion = gameState.questions[gameState.currentQuestionIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCorrect)
                            Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else
                            Color(0xFFFF1744).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF1744),
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = if (isCorrect) "Correct!" else "Incorrect",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF1744),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        if (!isCorrect) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Correct answer: ${currentQuestion.options[currentQuestion.correctAnswerIndex]}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(32.dp))

        // Explanation card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Did you know?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Next button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(bottom = 20.dp)
            ,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (gameState.currentQuestionIndex >= gameState.questions.size - 1)
                    "See Results"
                else
                    "Next Question",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}