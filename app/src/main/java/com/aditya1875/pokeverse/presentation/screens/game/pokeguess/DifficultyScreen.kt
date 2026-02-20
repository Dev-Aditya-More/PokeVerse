package com.aditya1875.pokeverse.presentation.screens.game.pokeguess

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.BuildConfig
import com.aditya1875.pokeverse.presentation.screens.game.pokeguess.components.GuessDifficulty
import com.aditya1875.pokeverse.presentation.screens.game.pokematch.components.PremiumBanner
import com.aditya1875.pokeverse.presentation.ui.viewmodel.MatchViewModel
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import com.aditya1875.pokeverse.utils.SubscriptionState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokeGuessDifficultyScreen(
    onDifficultySelected: (GuessDifficulty) -> Unit,
    onBack: () -> Unit
) {
    val matchViewModel : MatchViewModel = koinViewModel()
    val subscriptionState by matchViewModel.subscriptionState.collectAsStateWithLifecycle()
    var showPremiumSheet by remember { mutableStateOf(false) }

    val billingViewModel: BillingViewModel = koinViewModel()
    val monthly by billingViewModel.monthlyPrice.collectAsStateWithLifecycle()
    val yearly by billingViewModel.yearlyPrice.collectAsStateWithLifecycle()
    val monthlyProduct by billingViewModel.monthlyProduct.collectAsStateWithLifecycle()
    val yearlyProduct by billingViewModel.yearlyProduct.collectAsStateWithLifecycle()
    val isBillingReady = monthlyProduct != null || yearlyProduct != null

    val canPlayHard = subscriptionState is SubscriptionState.Premium

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Who's That Pokémon?", fontWeight = FontWeight.Bold)
                        Text(
                            "Guess from silhouettes!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
            }
            item {
                GuessDifficultyCard(
                    difficulty = GuessDifficulty.EASY,
                    locked = false
                ) { onDifficultySelected(GuessDifficulty.EASY) }
            }

            item {
                GuessDifficultyCard(
                    difficulty = GuessDifficulty.MEDIUM,
                    locked = false
                ) { onDifficultySelected(GuessDifficulty.MEDIUM) }
            }

            item {
                GuessDifficultyCard(
                    difficulty = GuessDifficulty.HARD,
                    locked = !canPlayHard
                ) {
                    if (canPlayHard) {
                        onDifficultySelected(GuessDifficulty.HARD)
                    } else {
                        showPremiumSheet = true
                    }
                }
            }

            if (BuildConfig.ENABLE_BILLING && subscriptionState is SubscriptionState.Free) {
                item {
                    PremiumBanner(
                        price = monthly,
                        onSubscribe = { showPremiumSheet = true }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun GuessDifficultyCard(
    difficulty: GuessDifficulty,
    locked: Boolean,
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
            modifier = Modifier.fillMaxWidth().padding(20.dp),
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),
                        Color(0xFF0D47A1)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.Yellow.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.Yellow,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = when {
                    percentage == 100 -> "Perfect! 🎉"
                    percentage >= 80 -> "Excellent! ⭐"
                    percentage >= 60 -> "Great Job! 👍"
                    else -> "Keep Trying! 💪"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Score card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Yellow,
                        fontSize = 64.sp
                    )
                    Text(
                        text = "points",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "$correctAnswers / $totalQuestions correct",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

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

            Spacer(Modifier.weight(1f))

            // Buttons
            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Yellow,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Replay, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Play Again",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBackToMenu,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Back to Menu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}