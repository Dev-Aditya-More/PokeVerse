package com.aditya1875.pokeverse.feature.game.cardclash.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
    state: ClashUiState = ClashUiState(),
    onSelectCard: (Int) -> Unit = {},
    onLockCard: () -> Unit = {},
    onAcknowledgeReveal: () -> Unit = {},
    onPlayAgain: () -> Unit = {},
    onExit: () -> Unit = {},
    onForfeitOpponent: () -> Unit = {}
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

// ─── Battle arena background ──────────────────────────────────────────────────

@Composable
private fun GameBackground(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "bg_pulse").animateFloat(
        initialValue = 0.6f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(4500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Deep base
        drawRect(Color(0xFF07090F))

        // Player-side glow — top portion, blue-tinted
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1B3A9C).copy(alpha = 0.28f * pulse), Color.Transparent),
                center = Offset(w * 0.2f, h * 0.18f),
                radius = w * 0.7f
            ),
            radius = w * 0.7f,
            center = Offset(w * 0.2f, h * 0.18f)
        )

        // Opponent-side glow — bottom portion, red-tinted
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF9C1B1B).copy(alpha = 0.22f * pulse), Color.Transparent),
                center = Offset(w * 0.8f, h * 0.82f),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(w * 0.8f, h * 0.82f)
        )

        // Faint horizontal arena divider at mid-screen
        drawLine(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.2f to Color.White.copy(alpha = 0.05f),
                    0.8f to Color.White.copy(alpha = 0.05f),
                    1f to Color.Transparent
                )
            ),
            start = Offset(0f, h * 0.5f),
            end = Offset(w, h * 0.5f),
            strokeWidth = 1.dp.toPx()
        )

        // Faint corner accent arcs for the "arena" feel
        val arcRadius = w * 0.12f
        drawArc(
            color = Color.White.copy(alpha = 0.04f),
            startAngle = 0f, sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(0f, h * 0.5f - arcRadius),
            size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(width = 1.dp.toPx())
        )
        drawArc(
            color = Color.White.copy(alpha = 0.04f),
            startAngle = 180f, sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w - arcRadius * 2, h * 0.5f - arcRadius),
            size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

// ─── Selecting phase ──────────────────────────────────────────────────────────

@Composable
private fun SelectingScreen(
    state: ClashUiState,
    onSelectCard: (Int) -> Unit,
    onLockCard: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        GameBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScoreRow(
                myScore = state.myScore,
                opponentScore = state.opponentScore,
                currentRound = state.currentRound,
                opponentName = state.opponentName
            )

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
                color = Color.White.copy(alpha = 0.75f)
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
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                stringResource(R.string.clash_you),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Text(
                formatScore(myScore),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Round badge
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        stringResource(R.string.clash_round_format, currentRound),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                opponentName.take(12),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
            Text(
                formatScore(opponentScore),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun OpponentArea(opponentName: String, cardsRemaining: Int, opponentLocked: Boolean) {
    val lockedPulse by rememberInfiniteTransition(label = "opp_lock").animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lock_pulse"
    )
    val borderAlpha = if (opponentLocked) lockedPulse * 0.4f else 0.10f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, if (opponentLocked) MaterialTheme.colorScheme.tertiary.copy(alpha = borderAlpha) else Color.White.copy(alpha = borderAlpha), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(minOf(cardsRemaining, 4)) { CardBack(modifier = Modifier.size(40.dp)) }
            if (cardsRemaining > 4) {
                Text(
                    "+${cardsRemaining - 4}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = opponentName.take(14),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
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
                        else Color.White.copy(alpha = 0.45f)
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
    LaunchedEffect(Unit) { delay(100); visible = true }

    Box(modifier = Modifier.fillMaxSize()) {
        GameBackground()
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
                // Round reveal header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.clash_round_reveal, round.roundNumber),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                            pokemon = myCard, isSelected = false, isLocked = false, onClick = {},
                            modifier = Modifier.height(180.dp)
                        )
                        Text(
                            stringResource(R.string.clash_power, round.myScore.toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
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
                            pokemon = oppCard, isSelected = false, isLocked = false, onClick = {},
                            modifier = Modifier.height(180.dp)
                        )
                        Text(
                            stringResource(R.string.clash_power, round.opponentScore.toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                RoundResultBanner(winner = round.winner, myCard = myCard, oppCard = oppCard)

                Spacer(Modifier.weight(1f))

                if (state.currentRound < 6) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.clash_next_round), fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Last round — auto-advance once Firestore delivers "finished"
                    OutlinedButton(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("See final results", fontWeight = FontWeight.Bold, color = Color.White)
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
                Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = bgColor)
                val typeText = buildTypeText(winner, myCard, oppCard)
                if (typeText.isNotBlank()) {
                    Text(
                        text = typeText, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
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
                    Text(text = headline, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = color)
                    Text(
                        text = "${formatScore(state.myScore)}  —  ${formatScore(state.opponentScore)}",
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(text = stringResource(R.string.clash_vs, state.opponentName), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))

            if (state.roundHistory.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.clash_round_history), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(20.dp), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        state.roundHistory.forEachIndexed { i, round ->
                            RoundSummaryRow(roundNumber = i + 1, round = round)
                            if (i < state.roundHistory.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.action_play_again), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("R$roundNumber", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(24.dp))
        Text(round.myCard.name.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
        Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(26.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color) }
        }
        Text(round.opponentCard.name.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

private fun formatScore(score: Float): String =
    if (score == score.toLong().toFloat()) score.toLong().toString() else score.toString()

// ─── Face-down selectable card ────────────────────────────────────────────────

@Composable
private fun SelectableCardBack(position: Int, isSelected: Boolean, isLocked: Boolean, onClick: () -> Unit) {
    val shimmer by rememberInfiniteTransition(label = "card_shimmer_$position").animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shimmer"
    )

    val borderColor = when {
        isLocked && isSelected -> MaterialTheme.colorScheme.tertiary
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.White.copy(alpha = 0.12f)
    }
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgBrush = if (isSelected)
        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), Color(0xFF0D1525)))
    else
        Brush.verticalGradient(listOf(Color(0xFF131828), Color(0xFF09101C)))

    Box(
        modifier = Modifier
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .background(bgBrush)
            .clickable(enabled = !isLocked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Radial glow overlay when selected and unlocked
        if (isSelected && !isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = shimmer * 0.18f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "?",  // intentionally universal — not a string resource
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else Color.White.copy(alpha = 0.35f)
            )
            Text(
                text = stringResource(R.string.clash_card_number, position),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = if (isSelected) 0.75f else 0.25f)
            )
        }
    }
}

// ─── Timer bar ────────────────────────────────────────────────────────────────

@Composable
private fun TimerBar(timerSeconds: Int) {
    val progress by animateFloatAsState(
        targetValue = timerSeconds / 60f, animationSpec = tween(durationMillis = 900), label = "round_timer"
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.clash_round_timer), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
            Text(text = stringResource(R.string.clash_timer_seconds, timerSeconds), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = barColor)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = Color.White.copy(alpha = 0.08f)
        )
    }
}

// ─── Disconnect overlay ───────────────────────────────────────────────────────

@Composable
private fun DisconnectOverlay(opponentName: String, onWait: () -> Unit, onClaimWin: () -> Unit) {
    var secondsLeft by remember { mutableStateOf(30) }
    val canClaim = secondsLeft <= 0

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) { delay(1000L); secondsLeft-- }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(horizontal = 32.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = stringResource(R.string.clash_connection_lost), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = if (canClaim) stringResource(R.string.clash_disconnected_message, opponentName.take(16))
                    else stringResource(R.string.clash_disconnect_timer, opponentName.take(16), secondsLeft),
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (canClaim) {
                    Button(
                        onClick = onClaimWin, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) { Text(stringResource(R.string.clash_claim_win), fontWeight = FontWeight.Bold) }
                }
                OutlinedButton(onClick = onWait, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.clash_wait_opponent))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardClashGameScreenPreview() {
    CardClashGameScreen(
        state = ClashUiState(
            phase = ClashPhase.DEALING,
            opponentName = "Ash",
            currentRound = 1,
            myScore = 2f,
            opponentScore = 1f,
            timerSeconds = 45,

            myHand = listOf(
                ClashPokemon(
                    id = 25,
                    name = "Pikachu",
                    types = listOf("electric"),
                    bst = 320,
                    spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png"
                ),
                ClashPokemon(
                    id = 6,
                    name = "Charizard",
                    types = listOf("fire", "flying"),
                    bst = 534,
                    spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/6.png"
                ),
                ClashPokemon(
                    id = 149,
                    name = "Dragonite",
                    types = listOf("dragon", "flying"),
                    bst = 600,
                    spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/149.png"
                ),
                ClashPokemon(
                    id = 448,
                    name = "Lucario",
                    types = listOf("fighting", "steel"),
                    bst = 525,
                    spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/448.png"
                ),
                ClashPokemon(
                    id = 94,
                    name = "Gengar",
                    types = listOf("ghost", "poison"),
                    bst = 500,
                    spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/94.png"
                )
            ),

            myUsedIds = setOf(25),

            opponentLocked = false,
            myLocked = false
        ),
        onSelectCard = {},
        onLockCard = {},
        onAcknowledgeReveal = {},
        onPlayAgain = {},
        onExit = {},
        onForfeitOpponent = {}
    )
}
