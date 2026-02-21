package com.aditya1875.pokeverse.presentation.screens.game.pokematch.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.aditya1875.pokeverse.BuildConfig
import com.aditya1875.pokeverse.data.billing.SubscriptionState
import com.aditya1875.pokeverse.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.presentation.ui.viewmodel.MatchViewModel
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import com.aditya1875.pokeverse.utils.Difficulty
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultyScreen(
    onDifficultySelected: (Difficulty) -> Unit,
    onBack: () -> Unit,
    viewModel: MatchViewModel = koinViewModel()
) {
    val topScores by viewModel.topScores.collectAsStateWithLifecycle()
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = (context as? Activity)

    val billingViewModel: BillingViewModel = koinViewModel()
    val monthly by billingViewModel.monthlyPrice.collectAsStateWithLifecycle()
    val yearly by billingViewModel.yearlyPrice.collectAsStateWithLifecycle()
    val monthlyProduct by billingViewModel.monthlyProduct.collectAsStateWithLifecycle()
    val yearlyProduct by billingViewModel.yearlyProduct.collectAsStateWithLifecycle()
    val isBillingReady = monthlyProduct != null || yearlyProduct != null

    var showPremiumSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PokéMatch",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                Column {
                    Text(
                        text = "Select Difficulty",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Match all Pokémon pairs to win!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            items(Difficulty.entries.toTypedArray()) { difficulty ->
                val canPlay = viewModel.canPlayDifficulty(difficulty)
                DifficultyCard(
                    difficulty = difficulty,
                    canPlay = canPlay,
                    bestScore = topScores
                        .filter { it.difficulty == difficulty.name }
                        .maxByOrNull { it.score },
                    onSelect = {
                        if (canPlay) {
                            onDifficultySelected(difficulty)
                        } else {
                            showPremiumSheet = true
                        }
                    }
                )
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

    if (showPremiumSheet) {
        PremiumBottomSheet(
            onDismiss = { showPremiumSheet = false },
            onSubscribeMonthly = {
                showPremiumSheet = false
                activity?.let { billingViewModel.purchaseMonthly(it) }
            },
            onSubscribeYearly = {
                showPremiumSheet = false
                activity?.let { billingViewModel.purchaseYearly(it) }
            },
            monthlyPrice = monthly,
            yearlyPrice = yearly,
            isSubscribeEnabled = isBillingReady
        )
    }

}

@Composable
fun DifficultyCard(
    difficulty: Difficulty,
    canPlay: Boolean,
    bestScore: GameScoreEntity?,
    onSelect: () -> Unit
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

                Text(
                    text = "${difficulty.pairs} pairs • ${difficulty.timeSeconds}s • ${difficulty.gridColumns}×${difficulty.gridRows}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (canPlay) 1f else 0.4f)
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
                tint = if (canPlay) difficultyColor
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

