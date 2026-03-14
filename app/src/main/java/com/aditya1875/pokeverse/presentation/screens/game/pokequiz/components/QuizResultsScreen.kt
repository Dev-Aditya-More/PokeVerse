package com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.presentation.screens.game.GameResultLayout
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
    val maxScore = totalQuestions * 100
    val percentage = (score.toFloat() / maxScore * 100).toInt()

    LaunchedEffect(Unit) {
        soundManager.play(SoundManager.Sound.GAME_WIN)
    }

    GameResultLayout(
        title = when (stars) {
            3 -> "Perfect!"
            2 -> "Great Job!"
            1 -> "Good Try!"
            else -> "Keep Practicing!"
        },
        subtitle = "",
        score = score.toString(),
        scoreLabel = "points • $percentage%",
        heroColor = MaterialTheme.colorScheme.primary,
        onPlayAgain = onPlayAgain,
        onBack = onBackToMenu,
        heroContent = {

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD700).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    Icon(
                        imageVector = if (index < stars)
                            Icons.Default.Star
                        else
                            Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        statsContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Check,
                    label = "Correct",
                    value = "$correctAnswers/$totalQuestions",
                    color = Color(0xFF4CAF50)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Close,
                    label = "Wrong",
                    value = "${totalQuestions - correctAnswers}",
                    color = Color(0xFFFF1744)
                )
            }
        }
    )
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}