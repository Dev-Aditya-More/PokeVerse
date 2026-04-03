package com.aditya1875.pokeverse.feature.game.pokeguess.presentation.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.core.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.feature.game.pokeguess.presentation.viewmodels.PokeGuessViewModel
import com.aditya1875.pokeverse.feature.game.core.presentation.GameDifficultyLayout
import com.aditya1875.pokeverse.feature.game.core.presentation.GameResultLayout
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatChips
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatRow
import com.aditya1875.pokeverse.feature.game.pokeguess.domain.model.GuessDifficulty
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import com.aditya1875.pokeverse.utils.SoundManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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
    onBackToMenu: () -> Unit,
    soundManager: SoundManager = koinInject()
) {
    LaunchedEffect(Unit) { soundManager.play(SoundManager.Sound.GAME_WIN) }

    val pct = (correctAnswers.toFloat() / totalQuestions * 100).toInt()
    val heroColor = when {
        pct == 100 -> Color(0xFFFFD700)
        pct >= 80  -> Color(0xFF4CAF50)
        pct >= 60  -> Color(0xFF2196F3)
        else       -> Color(0xFF9E9E9E)
    }
    val titleText = when {
        pct == 100 -> "Perfect!"
        pct >= 80  -> "Excellent!"
        pct >= 60  -> "Great Job!"
        else       -> "Keep Going!"
    }
    val stars = when {
        pct >= 90 -> 3; pct >= 70 -> 2; pct >= 50 -> 1; else -> 0
    }

    GameResultLayout(
        title = titleText,
        subtitle = "Who's That Pokémon?",
        score = score.toString(),
        scoreLabel = "POINTS",
        heroColor = heroColor,
        stars = stars,
        onPlayAgain = onPlayAgain,
        onBack = onBackToMenu,
        heroContent = {
            Text(
                text = when {
                    pct == 100 -> "👁️‍🗨️"; pct >= 80 -> "👁️"; pct >= 60 -> "🔍"; else -> "❓"
                },
                fontSize = 64.sp
            )
        },
        statsContent = {
            ResultStatChips(
                "Correct" to "$correctAnswers",
                "Missed"  to "${totalQuestions - correctAnswers}",
                "Rate"    to "$pct%"
            )
            Spacer(Modifier.height(16.dp))
            ResultStatRow(
                label = "Total score",
                value = score.toString(),
                valueColor = heroColor,
                icon = Icons.Default.Star
            )
            ResultStatRow(
                label = "Questions",
                value = "$correctAnswers / $totalQuestions",
                icon = Icons.AutoMirrored.Filled.Help
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

