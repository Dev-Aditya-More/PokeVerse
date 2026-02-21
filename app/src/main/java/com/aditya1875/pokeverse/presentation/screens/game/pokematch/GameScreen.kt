package com.aditya1875.pokeverse.presentation.screens.game.pokematch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.presentation.screens.game.pokematch.components.GameTimer
import com.aditya1875.pokeverse.presentation.screens.game.pokematch.components.PokemonCard
import com.aditya1875.pokeverse.presentation.ui.viewmodel.MatchViewModel
import com.aditya1875.pokeverse.utils.Difficulty
import com.aditya1875.pokeverse.utils.GameState
import com.aditya1875.pokeverse.utils.SoundManager
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameScreen(
    difficulty: Difficulty,
    onBack: () -> Unit,
    viewModel: MatchViewModel = koinViewModel()
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        showExitDialog = true
    }

    val soundManager : SoundManager = koinInject()

    LaunchedEffect(difficulty) {
        viewModel.startGame(difficulty)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            when (val state = gameState) {
                is GameState.Loading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularWavyProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Preparing Pokémon...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                is GameState.Playing, is GameState.Paused -> {
                    val playing = state as? GameState.Playing ?: (state as GameState.Paused).playing
                    val isPaused = state is GameState.Paused

                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(16.dp)
                        ) {
                            // Top bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Score
                                Column {
                                    Text(
                                        text = "Score",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = playing.score.toString(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Timer - center
                                GameTimer(
                                    timeRemaining = playing.timeRemaining,
                                    totalTime = playing.difficulty.timeSeconds,
                                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                                )

                                // Moves + Pause
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Moves",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = playing.moves.toString(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isPaused) viewModel.resumeGame()
                                        else viewModel.pauseGame()
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Default.PlayArrow
                                        else Icons.Default.Pause,
                                        contentDescription = if (isPaused) "Resume" else "Pause",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            // Progress indicator
                            Text(
                                text = "${playing.matchedPairs.size}/${playing.difficulty.pairs} pairs found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Card Grid
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(playing.difficulty.gridColumns),
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                userScrollEnabled = true
                            ) {
                                itemsIndexed(playing.cards) { index, card ->
                                    PokemonCard(
                                        card = card,
                                        onClick = {
                                            if (!isPaused) {
                                                soundManager.play(SoundManager.Sound.CARD_FLIP)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.onCardFlipped(index)
                                            }
                                        },
                                        modifier = Modifier.aspectRatio(0.75f)
                                    )
                                }
                            }
                        }
                    }

                    // Pause overlay
                    if (isPaused) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {},
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Paused",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.resumeGame()
                                            soundManager.play(SoundManager.Sound.BUTTON_CLICK)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.PlayArrow, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Resume")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.restartGame() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Refresh, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Restart")
                                    }
                                    TextButton(
                                        onClick = { showExitDialog = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Exit Game", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                is GameState.Victory -> VictoryScreen(
                    victory = state,
                    onPlayAgain = { viewModel.startGame(state.difficulty) },
                    onChangeDifficulty = onBack,
                    onHome = {
                        showExitDialog = true
                    }
                )

                is GameState.TimeUp -> TimeUpScreen(
                    timeUp = state,
                    onPlayAgain = { viewModel.startGame(state.difficulty) },
                    onBack = { showExitDialog = true }
                )

                else -> {}
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
                            viewModel.returnToMenu()
                            onBack()
                        }
                    ) {
                        Text("Exit", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}