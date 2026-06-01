package com.aditya1875.pokeverse.feature.game.cardclash.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashPhase
import com.aditya1875.pokeverse.feature.game.cardclash.presentation.CardClashViewModel
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CardClashScreen(
    onBack: () -> Unit = {},
    initialCode: String = "",
    viewModel: CardClashViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var pendingXp by remember { mutableStateOf<XPResult?>(null) }

    LaunchedEffect(initialCode) {
        if (initialCode.isNotEmpty()) viewModel.updateEnteredCode(initialCode)
    }

    LaunchedEffect(Unit) {
        viewModel.xpResult.collect { pendingXp = it }
    }

    val isInGame = state.phase in setOf(
        ClashPhase.SELECTING,
        ClashPhase.REVEALING,
        ClashPhase.MATCH_FINISHED
    )

    XPOverlay(result = pendingXp, onDismiss = { pendingXp = null }) {
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
                onBack = onBack,
                onPlayRandom = viewModel::joinRandom,
                onCreateFriendRoom = viewModel::createMatch,
                onJoinByCode = viewModel::joinByCode,
                onCodeChanged = viewModel::updateEnteredCode,
                onCancelWait = viewModel::reset
            )
        }
    }
}
