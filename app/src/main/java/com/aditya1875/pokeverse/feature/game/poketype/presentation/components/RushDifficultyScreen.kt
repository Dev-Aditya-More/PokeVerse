package com.aditya1875.pokeverse.feature.game.poketype.presentation.components

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.poketype.presentation.viewmodels.TypeRushViewModel
import com.aditya1875.pokeverse.feature.game.core.presentation.GameDifficultyLayout
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushDifficulty
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

    GameDifficultyLayout(
        gameTitle = "Type Rush",
        gameSubtitle = "Guess Pokémon types as fast as you can!",
        difficultyHint = "Speed matters. Think fast!",
        onBack = onBack,
        subscriptionState = subscriptionState
    ) {

        items(TypeRushDifficulty.entries.toTypedArray()) { difficulty ->

            val canPlay = when (difficulty) {
                TypeRushDifficulty.EASY -> true
                TypeRushDifficulty.MEDIUM -> true
                TypeRushDifficulty.HARD -> isPremium
            }

            TypeRushDifficultyCard(
                difficulty = difficulty,
                canPlay = canPlay,
                bestScore = topScores
                    .filter { it.difficulty == difficulty.name }
                    .maxByOrNull { it.score },
                onSelect = {
                    if (canPlay) onDifficultySelected(difficulty)
                }
            )
        }
    }
}