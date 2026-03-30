package com.aditya1875.pokeverse.feature.game.poketype.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.feature.game.core.presentation.GameResultLayout
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatChips
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatRow
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushState
import com.aditya1875.pokeverse.utils.SoundManager
import org.koin.compose.koinInject

@Composable
fun TypeRushResultScreen(
    state: TypeRushState.Finished,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    soundManager: SoundManager = koinInject()
) {
    LaunchedEffect(Unit) { soundManager.play(SoundManager.Sound.GAME_WIN) }

    val pct = state.correctRounds.toFloat() / state.totalRounds
    val stars = when { pct >= 0.9f -> 3; pct >= 0.7f -> 2; pct >= 0.5f -> 1; else -> 0 }
    val heroColor = when (stars) {
        3 -> Color(0xFFFFD700)
        2 -> Color(0xFF4CAF50)
        1 -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
    }

    GameResultLayout(
        title = when (stars) {
            3 -> "Type Master!"
            2 -> "Well Typed!"
            1 -> "Keep Rushing!"
            else -> "Type Learner"
        },
        subtitle = "Pokémon Type Rush",
        score = state.score.toString(),
        scoreLabel = "POINTS",
        heroColor = heroColor,
        stars = stars,
        onPlayAgain = onPlayAgain,
        onBack = onBack,
        heroContent = {
            Text("⚡", fontSize = 64.sp)
        },
        statsContent = {
            ResultStatChips(
                "Correct"  to "${state.correctRounds}/${state.totalRounds}",
                "Accuracy" to "${(pct * 100).toInt()}%",
                "Mode"     to state.difficulty.label
            )
            Spacer(Modifier.height(16.dp))
            ResultStatRow(
                label = "Rounds completed",
                value = "${state.correctRounds} / ${state.totalRounds}",
                icon = Icons.Default.CheckCircle,
                valueColor = heroColor
            )
            ResultStatRow(
                label = "Difficulty",
                value = state.difficulty.label,
                icon = Icons.Default.Speed,
                isLast = true
            )
        }
    )
}