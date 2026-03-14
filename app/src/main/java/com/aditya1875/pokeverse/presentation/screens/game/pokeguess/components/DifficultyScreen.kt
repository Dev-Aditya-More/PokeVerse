package com.aditya1875.pokeverse.presentation.screens.game.pokeguess.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.data.billing.SubscriptionState
import com.aditya1875.pokeverse.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.presentation.screens.game.GameDifficultyLayout
import com.aditya1875.pokeverse.presentation.screens.game.GameResultLayout
import com.aditya1875.pokeverse.presentation.ui.viewmodel.PokeGuessViewModel
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokeGuessDifficultyScreen(
    onDifficultySelected: (GuessDifficulty) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: PokeGuessViewModel = koinViewModel()
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()

    val topScores by viewModel.topScores.collectAsStateWithLifecycle()

    var showPremiumSheet by remember { mutableStateOf(false) }

    val billingViewModel: BillingViewModel = koinViewModel()
    val monthly by billingViewModel.monthlyPrice.collectAsStateWithLifecycle()
    val yearly by billingViewModel.yearlyPrice.collectAsStateWithLifecycle()
    val monthlyProduct by billingViewModel.monthlyProduct.collectAsStateWithLifecycle()
    val yearlyProduct by billingViewModel.yearlyProduct.collectAsStateWithLifecycle()
    val isBillingReady = monthlyProduct != null || yearlyProduct != null

    val context = LocalContext.current
    val activity = context as? Activity

    val isPremium = subscriptionState is SubscriptionState.Premium

    GameDifficultyLayout(
        gameTitle = "Who's That Pokémon?",
        gameSubtitle = "Guess from silhouettes!",
        difficultyHint = "Identify Pokémon from shadows.",
        onBack = onBack,
        subscriptionState = subscriptionState
    ) {
        item {
            GuessDifficultyCard(
                difficulty = GuessDifficulty.EASY,
                locked = false,
                bestScore = topScores
                    .filter { it.difficulty == GuessDifficulty.EASY.name }
                    .maxByOrNull { it.score },
            ) { onDifficultySelected(GuessDifficulty.EASY) }
        }

        item {
            GuessDifficultyCard(
                difficulty = GuessDifficulty.MEDIUM,
                locked = false,
                bestScore = topScores
                    .filter { it.difficulty == GuessDifficulty.MEDIUM.name }
                    .maxByOrNull { it.score },
            ) { onDifficultySelected(GuessDifficulty.MEDIUM) }
        }

        item {
            GuessDifficultyCard(
                difficulty = GuessDifficulty.HARD,
                locked = !isPremium,
                bestScore = topScores
                    .filter { it.difficulty == GuessDifficulty.HARD.name }
                    .maxByOrNull { it.score },
            ) {
                if (isPremium) {
                    onDifficultySelected(GuessDifficulty.HARD)
                } else {
                    showPremiumSheet = true
                }
            }
        }
    }
}

@Composable
private fun GuessDifficultyCard(
    difficulty: GuessDifficulty,
    locked: Boolean,
    bestScore: GameScoreEntity?,
    onClick: () -> Unit
) {
    val color = when (difficulty) {
        GuessDifficulty.EASY -> Color(0xFF4CAF50)
        GuessDifficulty.MEDIUM -> Color(0xFFFF9800)
        GuessDifficulty.HARD -> Color(0xFFFF1744)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (!locked) color.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (difficulty) {
                        GuessDifficulty.EASY -> Icons.Default.SentimentSatisfied
                        GuessDifficulty.MEDIUM -> Icons.Default.LocalFireDepartment
                        GuessDifficulty.HARD -> Icons.Default.Whatshot
                    },
                    contentDescription = null,
                    tint = if (!locked) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = difficulty.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (!locked) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    if (locked) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "${difficulty.questionsPerGame} Pokémon • ${difficulty.timePerQuestion}s • ${difficulty.optionCount} options",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (!locked) 1f else 0.4f)
                )

                bestScore?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Best: ${it.score}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (!locked) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun PokeGuessResultScreen(
    score: Int,
    correctAnswers: Int,
    totalQuestions: Int,
    difficulty: GuessDifficulty,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val percentage = (correctAnswers.toFloat() / totalQuestions * 100).toInt()

    GameResultLayout(
        title = when {
            percentage == 100 -> "Perfect!"
            percentage >= 80 -> "Excellent!"
            percentage >= 60 -> "Great Job!"
            else -> "Keep Trying!"
        },
        subtitle = "Who's That Pokémon?",
        score = score.toString(),
        scoreLabel = "$correctAnswers / $totalQuestions correct • $percentage%",
        heroColor = Color.Yellow,
        onPlayAgain = onPlayAgain,
        onBack = onBackToMenu,
        heroContent = {
            Icon(
                Icons.Default.EmojiEvents,
                null,
                tint = Color.Yellow,
                modifier = Modifier.size(56.dp)
            )
        },
        statsContent = {
            DifficultyBadge(difficulty)
        }
    )
}

@Composable
fun DifficultyBadge(difficulty: GuessDifficulty) {

    // Difficulty badge
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (difficulty) {
                GuessDifficulty.EASY -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                GuessDifficulty.MEDIUM -> Color(0xFFFF9800).copy(alpha = 0.3f)
                GuessDifficulty.HARD -> Color(0xFFFF1744).copy(alpha = 0.3f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${difficulty.displayName} Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}