package com.aditya1875.pokeverse.feature.game.cardclash.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashPhase
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashPokemon
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashUiState
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.MatchOutcome
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.RoundWinner
import com.aditya1875.pokeverse.feature.game.cardclash.presentation.components.ClashPokemonCard
import com.aditya1875.pokeverse.feature.game.cardclash.presentation.components.CardBack
import kotlinx.coroutines.delay

@Composable
fun CardClashGameScreen(
    state: ClashUiState,
    onSelectCard: (Int) -> Unit,
    onLockCard: () -> Unit,
    onAcknowledgeReveal: () -> Unit,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit,
    onForfeitOpponent: () -> Unit
) {
    // Local dismiss: resets whenever opponentDisconnected flips back to true
    var disconnectDismissed by remember(state.opponentDisconnected) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.phase) {
            ClashPhase.MATCH_FINISHED -> {
                MatchResultScreen(
                    state = state,
                    onPlayAgain = onPlayAgain,
                    onExit = onExit
                )
            }
            ClashPhase.REVEALING -> {
                RevealScreen(
                    state = state,
                    onContinue = onAcknowledgeReveal
                )
            }
            else -> {
                SelectingScreen(
                    state = state,
                    onSelectCard = onSelectCard,
                    onLockCard = onLockCard
                )
            }
        }

        // Disconnect overlay — shown on top of everything during SELECTING
        if (state.opponentDisconnected && !disconnectDismissed && state.phase == ClashPhase.SELECTING) {
            DisconnectOverlay(
                opponentName = state.opponentName,
                onWait = { disconnectDismissed = true },
                onClaimWin = onForfeitOpponent
            )
        }
    }
}

// ─── Selecting phase ──────────────────────────────────────────────────────────

@Composable
private fun SelectingScreen(
    state: ClashUiState,
    onSelectCard: (Int) -> Unit,
    onLockCard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Score bar
        ScoreRow(
            myScore = state.myScore,
            opponentScore = state.opponentScore,
            currentRound = state.currentRound,
            opponentName = state.opponentName
        )

        // Round timer
        TimerBar(timerSeconds = state.timerSeconds)

        // Opponent area
        OpponentArea(
            opponentName = state.opponentName,
            cardsRemaining = 6 - state.opponentRevealedCards.size,
            opponentLocked = state.opponentLocked
        )

        Spacer(Modifier.weight(1f))

        // My hand label
        Text(
            text = stringResource(R.string.clash_pick_card_hint),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )

        // Face-down card grid — mystery pick
        val availableCards = state.myHand.filter { it.id !in state.myUsedIds }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(availableCards, key = { index, card -> "${card.id}_$index" }) { index, pokemon ->
                SelectableCardBack(
                    position = index + 1,
                    isSelected = state.selectedCardId == pokemon.id,
                    isLocked = state.myLocked,
                    onClick = { onSelectCard(pokemon.id) }
                )
            }
        }

        // Lock In button
        Button(
            onClick = onLockCard,
            enabled = state.selectedCardId != null && !state.myLocked,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.myLocked) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (state.myLocked) stringResource(R.string.clash_locked_waiting) else stringResource(R.string.clash_lock_in),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ScoreRow(
    myScore: Float,
    opponentScore: Float,
    currentRound: Int,
    opponentName: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(stringResource(R.string.clash_you), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatScore(myScore),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.clash_round_format, currentRound),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(opponentName.take(12), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatScore(opponentScore),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun OpponentArea(
    opponentName: String,
    cardsRemaining: Int,
    opponentLocked: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Show face-down card stubs
            repeat(minOf(cardsRemaining, 4)) {
                CardBack(modifier = Modifier.size(44.dp))
            }
            if (cardsRemaining > 4) {
                Text("+${cardsRemaining - 4}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = opponentName.take(14),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (opponentLocked) stringResource(R.string.clash_opponent_locked) else stringResource(R.string.clash_opponent_choosing),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (opponentLocked) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Reveal phase ─────────────────────────────────────────────────────────────

@Composable
private fun RevealScreen(
    state: ClashUiState,
    onContinue: () -> Unit
) {
    val round = state.revealRound ?: return
    val myCard = state.revealMyCard ?: return
    val oppCard = state.revealOpponentCard ?: return

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 },
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Round ${round.roundNumber} Reveal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Cards side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.clash_you), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        ClashPokemonCard(
                            pokemon = myCard,
                            isSelected = false,
                            isLocked = false,
                            onClick = {},
                            modifier = Modifier.height(180.dp)
                        )
                        Text(
                            text = "Eff. Power: ${round.myScore.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.opponentName.take(12), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        ClashPokemonCard(
                            pokemon = oppCard,
                            isSelected = false,
                            isLocked = false,
                            onClick = {},
                            modifier = Modifier.height(180.dp)
                        )
                        Text(
                            text = "Eff. Power: ${round.opponentScore.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Result banner
                RoundResultBanner(winner = round.winner, myCard = myCard, oppCard = oppCard)

                Spacer(Modifier.weight(1f))

                if (state.currentRound <= 6) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.clash_next_round), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundResultBanner(
    winner: RoundWinner,
    myCard: ClashPokemon,
    oppCard: ClashPokemon
) {
    val (bgColor, label) = when (winner) {
        RoundWinner.ME -> Color(0xFF4CAF50) to "You win this round!"
        RoundWinner.OPPONENT -> Color(0xFFE53935) to "Opponent wins this round"
        RoundWinner.DRAW -> Color(0xFF757575) to "Draw!"
    }

    val typeAdvantageText = buildTypeText(winner, myCard, oppCard)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = bgColor
            )
            if (typeAdvantageText.isNotBlank()) {
                Text(
                    text = typeAdvantageText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun buildTypeText(winner: RoundWinner, myCard: ClashPokemon, oppCard: ClashPokemon): String {
    val myType = myCard.types.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: return ""
    val oppType = oppCard.types.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: return ""
    return when (winner) {
        RoundWinner.ME -> "$myType has the edge over $oppType"
        RoundWinner.OPPONENT -> "$oppType overpowers $myType"
        RoundWinner.DRAW -> "Equal matchup between $myType and $oppType"
    }
}

// ─── Match result phase ────────────────────────────────────────────────────────

@Composable
private fun MatchResultScreen(
    state: ClashUiState,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val outcome = state.matchOutcome ?: MatchOutcome.DRAW
    val (headline, color) = when (outcome) {
        MatchOutcome.WIN -> "Victory!" to Color(0xFF4CAF50)
        MatchOutcome.LOSE -> "Defeated" to Color(0xFFE53935)
        MatchOutcome.DRAW -> "Draw!" to Color(0xFF757575)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${formatScore(state.myScore)} — ${formatScore(state.opponentScore)}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "vs ${state.opponentName}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // Round history summary
        state.roundHistory.forEachIndexed { i, round ->
            RoundSummaryRow(roundNumber = i + 1, round = round)
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.action_play_again), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.clash_exit))
        }
    }
}

@Composable
private fun RoundSummaryRow(roundNumber: Int, round: com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashRound) {
    val color = when (round.winner) {
        RoundWinner.ME -> Color(0xFF4CAF50)
        RoundWinner.OPPONENT -> Color(0xFFE53935)
        RoundWinner.DRAW -> Color(0xFF757575)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("R$roundNumber  ${round.myCard.name}", style = MaterialTheme.typography.bodySmall)
        Text(
            text = when (round.winner) {
                RoundWinner.ME -> "W"
                RoundWinner.OPPONENT -> "L"
                RoundWinner.DRAW -> "D"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text("${round.opponentCard.name}", style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatScore(score: Float): String =
    if (score == score.toLong().toFloat()) score.toLong().toString() else score.toString()

// ─── Face-down selectable card ────────────────────────────────────────────────

@Composable
private fun SelectableCardBack(
    position: Int,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isLocked && isSelected -> Color(0xFF4CAF50)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }
    val borderWidth = if (isSelected) 2.5.dp else 1.dp
    val bgColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    else
        MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(enabled = !isLocked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "?",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "Card $position",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Timer bar ────────────────────────────────────────────────────────────────

@Composable
private fun TimerBar(timerSeconds: Int) {
    val progress by animateFloatAsState(
        targetValue = timerSeconds / 60f,
        animationSpec = tween(durationMillis = 900),
        label = "round_timer"
    )
    val barColor = when {
        timerSeconds > 30 -> Color(0xFF4CAF50)
        timerSeconds > 15 -> Color(0xFFFFC107)
        else -> Color(0xFFE53935)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.clash_round_timer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${timerSeconds}s",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ─── Disconnect overlay ───────────────────────────────────────────────────────

@Composable
private fun DisconnectOverlay(
    opponentName: String,
    onWait: () -> Unit,
    onClaimWin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.clash_connection_lost),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.clash_disconnected_message, opponentName.take(16)),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onClaimWin,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text(stringResource(R.string.clash_claim_win), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onWait,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.clash_wait_opponent))
                }
            }
        }
    }
}
