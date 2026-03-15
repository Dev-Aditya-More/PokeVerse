package com.aditya1875.pokeverse.presentation.screens.game.pokematch.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.presentation.screens.game.GameResultLayout
import com.aditya1875.pokeverse.presentation.screens.game.ResultStatChips
import com.aditya1875.pokeverse.presentation.screens.game.ResultStatRow
import com.aditya1875.pokeverse.utils.GameState
import com.aditya1875.pokeverse.utils.SoundManager
import org.koin.compose.koinInject

@Composable
fun VictoryScreen(
    victory: GameState.Victory,
    onPlayAgain: () -> Unit,
    onChangeDifficulty: () -> Unit,
    onHome: () -> Unit,
    soundManager: SoundManager = koinInject()
) {
    LaunchedEffect(Unit) { soundManager.play(SoundManager.Sound.GAME_WIN) }

    val heroColor = when (victory.stars) {
        3 -> Color(0xFFFFD700)
        2 -> Color(0xFF4CAF50)
        else -> Color(0xFF2196F3)
    }

    GameResultLayout(
        title = if (victory.isNewBest) "New Best!" else "You Win!",
        subtitle = "${victory.difficulty.displayName} cleared",
        score = victory.score.toString(),
        scoreLabel = "SCORE",
        heroColor = heroColor,
        stars = victory.stars,
        isNewBest = victory.isNewBest,
        onPlayAgain = onPlayAgain,
        onBack = onHome,
        heroContent = {
            Text(
                text = when (victory.stars) { 3 -> "🏆"; 2 -> "🎖️"; else -> "🃏" },
                fontSize = 64.sp
            )
        },
        statsContent = {
            ResultStatChips(
                "Score"  to victory.score.toString(),
                "Moves"  to victory.moves.toString(),
                "Time"   to "${victory.timeTaken}s"
            )
            Spacer(Modifier.height(16.dp))
            ResultStatRow(
                label = "Difficulty",
                value = victory.difficulty.displayName,
                icon = Icons.Default.Speed
            )
            ResultStatRow(
                label = "Moves used",
                value = victory.moves.toString(),
                icon = Icons.Default.TouchApp
            )
            ResultStatRow(
                label = "Time taken",
                value = "${victory.timeTaken}s",
                icon = Icons.Default.Timer,
                isLast = true
            )
        }
    )
}

@Composable
fun TimeUpScreen(
    timeUp: GameState.TimeUp,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit
) {
    val soundManager : SoundManager = koinInject()
    soundManager.play(SoundManager.Sound.TIMER_UP)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = Color(0xFFFF1744),
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = "Time's Up!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF1744)
            )

            Text(
                text = "${timeUp.matchesFound}/${timeUp.totalPairs} pairs found",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Try Again")
            }

            TextButton(onClick = onBack) {
                Text("Back to Menu")
            }
        }
    }
}