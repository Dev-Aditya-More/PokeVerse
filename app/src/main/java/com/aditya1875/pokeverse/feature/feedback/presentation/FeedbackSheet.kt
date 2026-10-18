package com.aditya1875.pokeverse.feature.feedback.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.feedback.data.FeedbackRepository
import com.aditya1875.pokeverse.feature.feedback.data.FeedbackWish

private fun FeedbackWish.emoji() = when (this) {
    FeedbackWish.MORE_GAMES -> "🎮"
    FeedbackWish.MULTIPLAYER -> "⚔️"
    FeedbackWish.TEAM_BUILDER -> "🛡️"
    FeedbackWish.SHINY_HUNTING -> "✨"
    FeedbackWish.POKEMON_GO -> "📍"
    FeedbackWish.COMPETITIVE -> "🏆"
    FeedbackWish.REGIONAL_DEX -> "📟"
    FeedbackWish.MORE_LANGUAGES -> "🌍"
    FeedbackWish.WIDGETS -> "🧩"
    FeedbackWish.OTHER -> "💡"
}

private fun FeedbackWish.label() = when (this) {
    FeedbackWish.MORE_GAMES -> R.string.feedback_wish_more_games
    FeedbackWish.MULTIPLAYER -> R.string.feedback_wish_multiplayer
    FeedbackWish.TEAM_BUILDER -> R.string.feedback_wish_team_builder
    FeedbackWish.SHINY_HUNTING -> R.string.feedback_wish_shiny
    FeedbackWish.POKEMON_GO -> R.string.feedback_wish_go
    FeedbackWish.COMPETITIVE -> R.string.feedback_wish_competitive
    FeedbackWish.REGIONAL_DEX -> R.string.feedback_wish_regional_dex
    FeedbackWish.MORE_LANGUAGES -> R.string.feedback_wish_languages
    FeedbackWish.WIDGETS -> R.string.feedback_wish_widgets
    FeedbackWish.OTHER -> R.string.feedback_wish_other
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedbackSheet(viewModel: FeedbackViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { viewModel.reset() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.sent) {
                Text("💌", fontSize = 44.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.feedback_thanks_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.feedback_thanks_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (state.xpAwarded > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "+${state.xpAwarded} XP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.feedback_close)) }
                return@Column
            }

            Text("💡", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.feedback_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.feedback_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FeedbackWish.entries.forEach { wish ->
                    FilterChip(
                        selected = wish in state.selected,
                        onClick = { viewModel.toggle(wish) },
                        label = { Text("${wish.emoji()} ${stringResource(wish.label())}") }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.message,
                onValueChange = viewModel::onMessage,
                placeholder = { Text(stringResource(R.string.feedback_message_hint)) },
                supportingText = { Text("${state.message.length}/${FeedbackRepository.MAX_MESSAGE}") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp)
            )

            if (state.failed) {
                Text(
                    stringResource(R.string.feedback_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (state.tooSoon) {
                Text(
                    stringResource(R.string.feedback_too_soon),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = viewModel::submit,
                enabled = state.canSend && !state.tooSoon,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (state.sending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.feedback_send), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FeedbackPromptDialog(onOpen: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        shape = RoundedCornerShape(24.dp),
        icon = { Text("💡", fontSize = 36.sp) },
        title = { Text(stringResource(R.string.feedback_popup_title), fontWeight = FontWeight.Black, textAlign = TextAlign.Center) },
        text = {
            Text(
                stringResource(R.string.feedback_popup_body),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(onClick = onOpen, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.feedback_popup_cta), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.dex_popup_later)) } }
    )
}
