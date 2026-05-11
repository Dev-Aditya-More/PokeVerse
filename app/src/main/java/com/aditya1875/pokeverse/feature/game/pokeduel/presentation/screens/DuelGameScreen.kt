package com.aditya1875.pokeverse.feature.game.pokeduel.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.game.pokeduel.domain.model.DuelGameState
import com.aditya1875.pokeverse.feature.game.pokeduel.domain.model.DuelOutcome
import com.aditya1875.pokeverse.feature.game.pokeduel.presentation.components.DuelResultOverlay
import com.aditya1875.pokeverse.feature.game.pokeduel.presentation.components.PokemonDuelCard
import com.aditya1875.pokeverse.feature.game.pokeduel.presentation.viewmodels.DuelViewModel
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import com.aditya1875.pokeverse.utils.SoundManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun DuelGameScreen(
    onBack: () -> Unit,
    viewModel: DuelViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingXp by remember { mutableStateOf<XPResult?>(null) }
    val soundManager: SoundManager = koinInject()

    LaunchedEffect(Unit) {
        viewModel.xpResult.collect { pendingXp = it }
    }

    val currentState = state
    LaunchedEffect(
        (currentState as? DuelGameState.Dueling)?.isCorrect
    ) {
        val s = currentState as? DuelGameState.Dueling ?: return@LaunchedEffect
        when (s.isCorrect) {
            true -> soundManager.play(SoundManager.Sound.CORRECT_ANSWER)
            false -> soundManager.play(SoundManager.Sound.WRONG_ANSWER)
            null -> Unit
        }
    }

    LaunchedEffect(state is DuelGameState.GameOver) {
        if (state is DuelGameState.GameOver) {
            soundManager.play(SoundManager.Sound.GAME_LOSE)
        }
    }

    XPOverlay(
        result = pendingXp,
        onDismiss = { pendingXp = null }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) { innerPadding ->
            when (val s = state) {
                is DuelGameState.Idle -> DuelIdleScreen(
                    onStart = {
                        soundManager.play(SoundManager.Sound.BUTTON_CLICK)
                        viewModel.startGame()
                    },
                    onBack = onBack,
                    modifier = Modifier.padding(innerPadding)
                )

                is DuelGameState.Loading -> LoadingScreen(
                    modifier = Modifier.padding(innerPadding)
                )

                is DuelGameState.Dueling -> DuelingScreen(
                    state = s,
                    onChoice = { choice ->
                        soundManager.play(SoundManager.Sound.BUTTON_CLICK)
                        viewModel.onChoice(choice)
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                is DuelGameState.GameOver -> DuelGameOverScreen(
                    state = s,
                    onPlayAgain = {
                        soundManager.play(SoundManager.Sound.BUTTON_CLICK)
                        viewModel.startGame()
                    },
                    onBack = onBack,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "swords")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -15f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "rock"
    )
    Box(
        modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "⚔️",
                fontSize = 52.sp,
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Finding opponents...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DuelIdleScreen(
    onStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulsing sword icon
            val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                initialValue = 1f, targetValue = 1.12f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "scale"
            )
            Text("⚔️", fontSize = 72.sp, modifier = Modifier.scale(pulse))

            Spacer(Modifier.height(20.dp))

            Text(
                "PokéDuel",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Two Pokémon face off!\nPick the winner based on type advantage.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Mini type hint chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                listOf("🔥 Fire > 🌿 Grass", "💧 Water > 🔥 Fire").forEach {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            it,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Start Dueling", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Back")
            }
        }
    }
}

// ── Main Game ────────────────────

@Composable
private fun DuelingScreen(
    state: DuelGameState.Dueling,
    onChoice: (DuelOutcome) -> Unit,
    modifier: Modifier = Modifier
) {
    val answered = state.result != null

    // Highlight colors for winner/loser
    val leftColor = when {
        !answered -> Color.Transparent
        state.result.outcome == DuelOutcome.LEFT_WINS -> Color(0xFF4CAF50)
        state.result.outcome == DuelOutcome.RIGHT_WINS -> Color(0xFFF44336)
        else -> Color(0xFFFFD700) // draw = gold
    }
    val rightColor = when {
        !answered -> Color.Transparent
        state.result.outcome == DuelOutcome.RIGHT_WINS -> Color(0xFF4CAF50)
        state.result.outcome == DuelOutcome.LEFT_WINS -> Color(0xFFF44336)
        else -> Color(0xFFFFD700)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(15.dp)
            .padding(top = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    Icon(
                        imageVector = if (i < state.lives) Icons.Default.Favorite
                        else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Text(
                "Round ${state.round}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${state.score} pts",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(30.dp))

        Text(
            "Who wins in a battle?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(30.dp))

        // Pokemon cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PokemonDuelCard(
                pokemon = state.left,
                highlight = leftColor,
                modifier = Modifier.weight(1f)
            )
            PokemonDuelCard(
                pokemon = state.right,
                highlight = rightColor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Result overlay
        if (answered) {
            DuelResultOverlay(state)
            Spacer(Modifier.height(16.dp))
        }

        if (!answered) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onChoice(DuelOutcome.LEFT_WINS) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "⬅ ${state.left.name.replaceFirstChar { it.uppercase() }} wins",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = { onChoice(DuelOutcome.RIGHT_WINS) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "${state.right.name.replaceFirstChar { it.uppercase() }} wins ➡",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedButton(
                    onClick = { onChoice(DuelOutcome.DRAW) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🤝 It's a Draw", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DuelGameOverScreen(
    state: DuelGameState.GameOver,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Game Over",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Round ${state.round} • ${state.score} pts",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.isNewBest) {
            Spacer(Modifier.height(8.dp))
            Text(
                "🏆 New Best!",
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) { Text("Play Again") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}