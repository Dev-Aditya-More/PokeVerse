package com.aditya1875.pokeverse.feature.game.pokequiz.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.feature.game.core.presentation.GameResultLayout
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatChips
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatRow
import com.aditya1875.pokeverse.feature.game.pokequiz.domain.model.QuizDifficulty
import com.aditya1875.pokeverse.utils.SoundManager
import org.koin.compose.koinInject

@Composable
fun QuizResultScreen(
    score: Int,
    correctAnswers: Int,
    totalQuestions: Int,
    difficulty: QuizDifficulty,
    stars: Int,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit,
    soundManager: SoundManager = koinInject()
) {
    LaunchedEffect(Unit) { soundManager.play(SoundManager.Sound.GAME_WIN) }

    val pct = (correctAnswers.toFloat() / totalQuestions * 100).toInt()
    val wrong = totalQuestions - correctAnswers
    val heroColor = when {
        stars == 3 -> Color(0xFFFFD700)
        stars == 2 -> Color(0xFF4CAF50)
        stars == 1 -> Color(0xFF2196F3)
        else       -> Color(0xFF9E9E9E)
    }
    val titleText = when (stars) {
        3 -> "Perfect!"
        2 -> "Great Job!"
        1 -> "Good Try!"
        else -> "Keep Practicing"
    }

    GameResultLayout(
        title = titleText,
        subtitle = difficulty.name.lowercase()
            .replaceFirstChar { it.uppercase() } + " difficulty",
        score = score.toString(),
        scoreLabel = "POINTS",
        heroColor = heroColor,
        stars = stars,
        onPlayAgain = onPlayAgain,
        onBack = onBackToMenu,
        heroContent = {
            Text(
                text = when (stars) { 3 -> "🏆"; 2 -> "🎖️"; 1 -> "🎯"; else -> "📚" },
                fontSize = 64.sp
            )
        },
        statsContent = {
            ResultStatChips(
                "Correct" to "$correctAnswers",
                "Wrong" to "$wrong",
                "Accuracy" to "$pct%"
            )
            Spacer(Modifier.height(16.dp))
            ResultStatRow(
                label = "Questions answered",
                value = "$correctAnswers / $totalQuestions",
                icon = Icons.Default.Quiz
            )
            ResultStatRow(
                label = "Score per question",
                value = if (totalQuestions > 0) "${score / totalQuestions}" else "0",
                icon = Icons.Default.BarChart
            )
            ResultStatRow(
                label = "Difficulty",
                value = difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                icon = Icons.Default.Speed,
                isLast = true
            )
        }
    )
}