package com.aditya1875.pokeverse.feature.game.pokematch.presentation.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.game.core.data.ads.IRewardedAdManager
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.core.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.feature.game.core.presentation.AdUnlockDialog
import com.aditya1875.pokeverse.feature.game.core.presentation.GameDifficultyLayout
import com.aditya1875.pokeverse.feature.game.pokematch.presentation.viewmodels.MatchViewModel
import com.aditya1875.pokeverse.feature.game.pokematch.domain.model.Difficulty
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DifficultyScreen(
    onDifficultySelected: (Difficulty) -> Unit,
    onBack: () -> Unit,
    viewModel: MatchViewModel = koinViewModel()
) {
    val topScores by viewModel.topScores.collectAsStateWithLifecycle()
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()
    val isPremium = subscriptionState is SubscriptionState.Premium

    val adManager = koinInject<IRewardedAdManager>()
    val adState by adManager.adState.collectAsStateWithLifecycle()

    var showAdDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(adState) {
        if (adState is RewardedAdState.Idle) adManager.loadAd(context)
    }

    if (showAdDialog) {
        AdUnlockDialog(
            adState = adState,
            onWatchAd = {
                activity?.let { act ->
                    adManager.showAd(act) {
                        showAdDialog = false
                        onDifficultySelected(Difficulty.HARD)
                    }
                }
            },
            onDismiss = { showAdDialog = false }
        )
    }

    GameDifficultyLayout(
        gameTitle = "PokéMatch",
        gameSubtitle = "Match all Pokémon pairs to win!",
        difficultyHint = "Flip cards and match all pairs.",
        onBack = onBack,
        subscriptionState = subscriptionState
    ) {
        items(Difficulty.entries.toTypedArray()) { difficulty ->
            val canPlay = when (difficulty) {
                Difficulty.HARD -> isPremium
                else -> true
            }
            DifficultyCard(
                difficulty = difficulty,
                canPlay = canPlay,
                adAvailable = !canPlay && difficulty == Difficulty.HARD && adState is RewardedAdState.Ready,
                bestScore = topScores
                    .filter { it.difficulty == difficulty.name }
                    .maxByOrNull { it.score },
                onSelect = {
                    if (canPlay) onDifficultySelected(difficulty)
                    else if (difficulty == Difficulty.HARD) showAdDialog = true
                }
            )
        }
    }
}

@Composable
fun DifficultyCard(
    difficulty: Difficulty,
    canPlay: Boolean,
    bestScore: GameScoreEntity?,
    onSelect: () -> Unit,
    adAvailable: Boolean = false
) {
    val difficultyColor = when (difficulty) {
        Difficulty.EASY -> Color(0xFF4CAF50)
        Difficulty.MEDIUM -> Color(0xFFFF9800)
        Difficulty.HARD -> Color(0xFFFF1744)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp),
        border = BorderStroke(
            1.dp,
            if (canPlay) difficultyColor.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (canPlay) difficultyColor.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (canPlay) difficultyColor
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = difficulty.displayName,
                        fontWeight = FontWeight.Bold,
                        color = if (canPlay) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    if (!canPlay) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (adAvailable) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.OndemandVideo,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Watch ad to play this round",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2196F3)
                        )
                    }
                }

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
                tint = if (canPlay) difficultyColor
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

