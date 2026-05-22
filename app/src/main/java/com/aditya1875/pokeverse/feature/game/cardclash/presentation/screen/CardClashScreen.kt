package com.aditya1875.pokeverse.feature.game.cardclash.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashPhase
import com.aditya1875.pokeverse.feature.game.cardclash.presentation.CardClashViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CardClashScreen(
    viewModel: CardClashViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val isInGame = state.phase in setOf(
        ClashPhase.SELECTING,
        ClashPhase.REVEALING,
        ClashPhase.MATCH_FINISHED
    )

    if (isInGame) {
        CardClashGameScreen(
            state = state,
            onSelectCard = viewModel::selectCard,
            onLockCard = viewModel::lockCard,
            onAcknowledgeReveal = viewModel::acknowledgeReveal,
            onPlayAgain = viewModel::reset,
            onExit = viewModel::reset,
            onForfeitOpponent = viewModel::forfeitOpponent
        )
    } else {
        CardClashLobbyScreen(
            state = state,
            onPlayRandom = viewModel::joinRandom,
            onCreateFriendRoom = viewModel::createMatch,
            onJoinByCode = viewModel::joinByCode,
            onCodeChanged = viewModel::updateEnteredCode
        )
    }
}
