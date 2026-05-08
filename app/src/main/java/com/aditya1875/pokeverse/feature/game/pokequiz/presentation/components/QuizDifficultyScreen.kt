package com.aditya1875.pokeverse.feature.game.pokequiz.presentation.components

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
import com.aditya1875.pokeverse.feature.game.pokematch.presentation.components.DifficultyCard
import com.aditya1875.pokeverse.feature.game.core.presentation.AdUnlockDialog
import com.aditya1875.pokeverse.feature.game.core.presentation.GameDifficultyLayout
import com.aditya1875.pokeverse.feature.game.pokequiz.presentation.viewmodels.QuizViewModel
import com.aditya1875.pokeverse.feature.game.pokematch.domain.model.Difficulty
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDifficultyScreen(
    onDifficultySelected: (Difficulty) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: QuizViewModel = koinViewModel()
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
                        onDifficultySelected(Difficulty.HARD)
                    }
                }
            },
            onDismiss = { showAdDialog = false }
        )
    }

    GameDifficultyLayout(
        gameTitle = "PokéQuiz",
        gameSubtitle = "Test your Pokémon knowledge!",
        difficultyHint = "Answer Pokémon questions correctly.",
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
