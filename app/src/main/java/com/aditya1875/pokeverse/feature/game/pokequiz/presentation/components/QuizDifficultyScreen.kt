package com.aditya1875.pokeverse.feature.game.pokequiz.presentation.components

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.pokematch.presentation.components.DifficultyCard
import com.aditya1875.pokeverse.feature.game.core.presentation.GameDifficultyLayout
import com.aditya1875.pokeverse.feature.game.pokequiz.presentation.viewmodels.QuizViewModel
import com.aditya1875.pokeverse.feature.game.pokematch.domain.model.Difficulty
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

    GameDifficultyLayout(
        gameTitle = "PokéQuiz",
        gameSubtitle = "Test your Pokémon knowledge!",
        difficultyHint = "Answer Pokémon questions correctly.",
        onBack = onBack,
        subscriptionState = subscriptionState
    ) {

        items(Difficulty.entries.toTypedArray()) { difficulty ->

            val canPlay = when (difficulty) {
                Difficulty.EASY -> true
                Difficulty.MEDIUM -> true
                Difficulty.HARD -> isPremium
            }

            DifficultyCard(
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
