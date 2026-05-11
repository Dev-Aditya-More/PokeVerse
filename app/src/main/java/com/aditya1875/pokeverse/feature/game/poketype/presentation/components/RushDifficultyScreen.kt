package com.aditya1875.pokeverse.feature.game.poketype.presentation.components

import android.app.Activity
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.game.core.data.ads.IRewardedAdManager
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.poketype.presentation.viewmodels.TypeRushViewModel
import com.aditya1875.pokeverse.feature.game.core.presentation.AdUnlockDialog
import com.aditya1875.pokeverse.feature.game.core.presentation.GameDifficultyLayout
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushDifficulty
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeRushDifficultyScreen(
    onDifficultySelected: (TypeRushDifficulty) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: TypeRushViewModel = koinViewModel()
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()
    val topScores by viewModel.topScores.collectAsStateWithLifecycle()
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
                        onDifficultySelected(TypeRushDifficulty.HARD)
                    }
                }
            },
            onDismiss = { showAdDialog = false }
        )
    }

    GameDifficultyLayout(
        gameTitle = "Type Rush",
        gameSubtitle = "Guess Pokémon types as fast as you can!",
        difficultyHint = "Speed matters. Think fast!",
        onBack = onBack,
        subscriptionState = subscriptionState
    ) {
        items(TypeRushDifficulty.entries.toTypedArray()) { difficulty ->
            val canPlay = when (difficulty) {
                TypeRushDifficulty.HARD -> isPremium
                else -> true
            }
            TypeRushDifficultyCard(
                difficulty = difficulty,
                canPlay = canPlay,
                adAvailable = !canPlay && difficulty == TypeRushDifficulty.HARD && adState is RewardedAdState.Ready,
                bestScore = topScores
                    .filter { it.difficulty == difficulty.name }
                    .maxByOrNull { it.score },
                onSelect = {
                    if (canPlay) onDifficultySelected(difficulty)
                    else if (difficulty == TypeRushDifficulty.HARD) showAdDialog = true
                }
            )
        }
    }
}