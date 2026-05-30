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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashRound
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashUiState
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.MatchOutcome
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.RoundWinner
import com.aditya1875.pokeverse.feature.game.cardclash.presentation.components.CardBack
import com.aditya1875.pokeverse.feature.game.cardclash.presentation.components.ClashPokemonCard
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
    var disconnectDismissed by remember(state.opponentDisconnected) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.phase) {
            ClashPhase.MATCH_FINISHED -> MatchResultScreen(state, onPlayAgain, onExit)
            ClashPhase.REVEALING -> RevealScreen(state, onAcknowledgeReveal)
            else -> SelectingScreen(state, onSelectCard, onLockCard)
        }

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
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            ScoreRow(
                myScore = state.myScore,
                opponentScore = state.opponentScore,
                currentRound = state.currentRound,
                opponentName = state.opponentName,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        TimerBar(timerSeconds = state.timerSeconds)

        OpponentArea(
            opponentName = state.opponentName,
            cardsRemaining = 6 - state.opponentRevealedCards.size,
            opponentLocked = state.opponentLocked
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = stringResource(R.string.clash_pick_card_hint),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

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

        Button(
            onClick = onLockCard,
            enabled = state.selectedCardId != null && !state.myLocked,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.myLocked) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (state.myLocked) stringResource(R.string.clash_locked_waiting)
                else stringResource(R.string.clash_lock_in),
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
    opponentName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                stringResource(R.string.clash_you),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            Text(
                opponentName.take(12),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
private fun OpponentArea(opponentName: String, cardsRemaining: Int, opponentLocked: Boolean) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(minOf(cardsRemaining, 4)) { CardBack(modifier = Modifier.size(44.dp)) }
            if (cardsRemaining > 4) {
                Text(
                    "+${cardsRemaining - 4}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = opponentName.take(14),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (opponentLocked) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                        )
                    }
                    Text(
                        text = if (opponentLocked) stringResource(R.string.clash_opponent_locked)
                        else stringResource(R.string.clash_opponent_choosing),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (opponentLocked) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Reveal phase ─────────────────────────────────────────────────────────────

@Composable
private fun RevealScreen(state: ClashUiState, onContinue: () -> Unit) {
    val round = state.revealRound ?: return
    val myCard = state.revealMyCard ?: return
    val oppCard = state.revealOpponentCard ?: return

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 },
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.clash_round_reveal, round.roundNumber),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            stringResource(R.string.clash_you),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        ClashPokemonCard(
                            pokemon = myCard,
                            isSelected = false,
                            isLocked = false,
                            onClick = {},
                            modifier = Modifier.height(180.dp)
                        )
                        Text(
                            stringResource(R.string.clash_power, round.myScore.toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            state.opponentName.take(12),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        ClashPokemonCard(
                            pokemon = oppCard,
                            isSelected = false,
                            isLocked = false,
                            onClick = {},
                            modifier = Modifier.height(180.dp)
                        )
                        Text(
                            stringResource(R.string.clash_power, round.opponentScore.toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
private fun RoundResultBanner(winner: RoundWinner, myCard: ClashPokemon, oppCard: ClashPokemon) {
    val winColor = MaterialTheme.colorScheme.tertiary
    val loseColor = MaterialTheme.colorScheme.error
    val drawColor = MaterialTheme.colorScheme.onSurfaceVariant
    val winLabel = stringResource(R.string.clash_you_win_round)
    val loseLabel = stringResource(R.string.clash_opponent_wins_round)
    val drawLabel = stringResource(R.string.clash_draw)
    val (bgColor, label) = when (winner) {
        RoundWinner.ME -> winColor to winLabel
        RoundWinner.OPPONENT -> loseColor to loseLabel
        RoundWinner.DRAW -> drawColor to drawLabel
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor.copy(alpha = 0.12f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = bgColor
                )
                val typeText = buildTypeText(winner, myCard, oppCard)
                if (typeText.isNotBlank()) {
                    Text(
                        text = typeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
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
private fun MatchResultScreen(state: ClashUiState, onPlayAgain: () -> Unit, onExit: () -> Unit) {
    val outcome = state.matchOutcome ?: MatchOutcome.DRAW
    val (headline, color) = when (outcome) {
        MatchOutcome.WIN -> stringResource(R.string.clash_victory) to MaterialTheme.colorScheme.tertiary
        MatchOutcome.LOSE -> stringResource(R.string.clash_defeated) to MaterialTheme.colorScheme.error
        MatchOutcome.DRAW -> stringResource(R.string.clash_draw) to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Result hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color.copy(alpha = 0.10f))
                    .padding(vertical = 36.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = "${formatScore(state.myScore)}  —  ${formatScore(state.opponentScore)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.clash_vs, state.opponentName),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (state.roundHistory.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.clash_round_history),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        state.roundHistory.forEachIndexed { i, round ->
                            RoundSummaryRow(roundNumber = i + 1, round = round)
                            if (i < state.roundHistory.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_play_again), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.clash_exit))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RoundSummaryRow(roundNumber: Int, round: ClashRound) {
    val (label, color) = when (round.winner) {
        RoundWinner.ME -> "W" to MaterialTheme.colorScheme.tertiary
        RoundWinner.OPPONENT -> "L" to MaterialTheme.colorScheme.error
        RoundWinner.DRAW -> "D" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "R$roundNumber",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )
        Text(
            round.myCard.name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
            }
        }
        Text(
            round.opponentCard.name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

private fun formatScore(score: Float): String =
    if (score == score.toLong().toFloat()) score.toLong().toString() else score.toString()

// ─── Face-down selectable card ────────────────────────────────────────────────

@Composable
private fun SelectableCardBack(position: Int, isSelected: Boolean, isLocked: Boolean, onClick: () -> Unit) {
    val borderColor = when {
        isLocked && isSelected -> MaterialTheme.colorScheme.tertiary
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
                text = "?",  // intentionally not a string resource — universal symbol
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = stringResource(R.string.clash_card_number, position),
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
    val safeColor = MaterialTheme.colorScheme.tertiary
    val warnColor = MaterialTheme.colorScheme.secondary
    val dangerColor = MaterialTheme.colorScheme.error
    val barColor = when {
        timerSeconds > 30 -> safeColor
        timerSeconds > 15 -> warnColor
        else -> dangerColor
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
                text = stringResource(R.string.clash_timer_seconds, timerSeconds),
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
private fun DisconnectOverlay(opponentName: String, onWait: () -> Unit, onClaimWin: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(30) }
    val canClaim = secondsLeft <= 0

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.clash_connection_lost),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (canClaim)
                        stringResource(R.string.clash_disconnected_message, opponentName.take(16))
                    else
                        stringResource(R.string.clash_disconnect_timer, opponentName.take(16), secondsLeft),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (canClaim) {
                    Button(
                        onClick = onClaimWin,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(stringResource(R.string.clash_claim_win), fontWeight = FontWeight.Bold)
                    }
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
