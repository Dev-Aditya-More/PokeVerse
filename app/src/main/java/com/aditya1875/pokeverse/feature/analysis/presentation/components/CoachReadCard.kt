package com.aditya1875.pokeverse.feature.analysis.presentation.components

import com.aditya1875.pokeverse.feature.pokemon.dex.MascotBall
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors
import com.aditya1875.pokeverse.feature.analysis.presentation.viewmodels.CoachUiState
import kotlinx.coroutines.delay

/**
 * The AI "Coach's Read" on the analysis screen. The numbers above it come from the rule engine;
 * this card is the personal, written take layered on top.
 */
@Composable
fun CoachReadCard(
    state: CoachUiState,
    onAsk: () -> Unit,
) {
    val accent = AnalysisColors.PURPLE
    val gold = Color(0xFFFFD54F)
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1B1530), AnalysisColors.CARD, Color(0xFF1A1710))
                )
            )
            .border(
                BorderStroke(1.dp, Brush.linearGradient(listOf(accent.copy(alpha = 0.7f), Color.White.copy(alpha = 0.06f), gold.copy(alpha = 0.55f)))),
                shape,
            )
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent), radius = 700f))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MascotBall(talking = state is CoachUiState.Loading, size = 34.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.coach_title).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.5.sp,
                        color = gold,
                    )
                    Text(
                        stringResource(R.string.coach_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.55f),
                    )
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        when (state) {
            is CoachUiState.Idle -> {
                AskButton(stringResource(R.string.coach_ask), accent, onAsk)
                RemainingLabel(state.remaining)
            }

            CoachUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.coach_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }

            is CoachUiState.Ready -> {
                RevealedText(text = state.text, animate = state.fresh)
                RemainingLabel(state.remaining)
            }

            CoachUiState.Limit -> Text(
                stringResource(R.string.coach_limit),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
            )

            is CoachUiState.Error -> {
                Text(
                    stringResource(if (state.network) R.string.coach_error else R.string.coach_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
                // Debug builds only: the real reason, so a misconfigured backend is diagnosable from a screenshot.
                state.detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }
                AskButton(stringResource(R.string.coach_retry), accent, onAsk)
            }
        }
        }
    }
}

@Composable
private fun AskButton(label: String, accent: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF14101F)),
        modifier = Modifier.fillMaxWidth().height(50.dp),
    ) {
        Text(label.uppercase(), fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun RemainingLabel(remaining: Int) {
    Text(
        if (remaining < 0) stringResource(R.string.coach_unlimited) else stringResource(R.string.coach_left, remaining),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.45f),
    )
}

/** Fresh reads type in; saved ones appear instantly. */
@Composable
private fun RevealedText(text: String, animate: Boolean) {
    var shown by remember(text) { mutableIntStateOf(if (animate) 0 else text.length) }
    LaunchedEffect(text) {
        while (shown < text.length) {
            delay(14)
            shown = (shown + 2).coerceAtMost(text.length)
        }
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(AnalysisColors.PURPLE, Color(0xFFFFD54F))))
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = text.take(shown),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 16.sp,
            lineHeight = 26.sp,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(0.dp))
}
