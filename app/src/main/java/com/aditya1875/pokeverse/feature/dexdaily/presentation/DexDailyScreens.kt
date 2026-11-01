package com.aditya1875.pokeverse.feature.dexdaily.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.dexdaily.data.DexDailyContent
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyCalendar
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyClue
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyRecord
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyRules
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.ProChip
import com.aditya1875.pokeverse.feature.game.premium.LocalPremium
import com.aditya1875.pokeverse.feature.game.premium.components.PremiumBottomSheet
import com.aditya1875.pokeverse.utils.SoundManager
import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// ─────────────────────────────────────────────────────────────────────────────
// Home card
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Top-of-Home entry for today's puzzle. Unplayed: a shadow and Play. Finished: the revealed
 * Pokémon, the result, Share and a countdown. The action sits on its own row so narrow
 * phones never squeeze the text column.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DexDailyCard(
    viewModel: DexDailyViewModel,
    isPremium: Boolean,
    onPlay: (firstOpen: Boolean) -> Unit,
    onOpenArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today by viewModel.today.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val loads by viewModel.loads.collectAsStateWithLifecycle()
    val record = records[today] ?: DexDailyRecord()
    val content = (loads[today] as? DexDailyLoad.Ready)?.content
    var sharing by remember { mutableStateOf(false) }
    val started = record != DexDailyRecord()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = { if (content != null) onPlay(!started) else viewModel.retry(today) }
    ) {
        Column(
            Modifier
                .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), Color.Transparent)))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    if (content != null) {
                        ShadowStage(content.artworkUrl, revealed = record.finished, size = 72.dp, animate = false)
                    } else {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.dex_daily_label, DexDailyCalendar.number(today)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        when {
                            loads[today] is DexDailyLoad.Failed -> stringResource(R.string.trivia_error)
                            record.solved -> stringResource(R.string.dex_daily_its, content?.answerName.orEmpty())
                            record.failed -> stringResource(R.string.trivia_it_was, content?.answerName.orEmpty())
                            else -> stringResource(R.string.dex_daily_title_new)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (record.finished || streak > 0) {
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            if (record.finished) ResultBadge(record.solved, size = 18.dp)
                            if (streak > 0) StreakChip(streak)
                            if (record.finished) {
                                Text(
                                    stringResource(R.string.dex_daily_next_in, rememberNextPuzzleCountdown()),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenArchive)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarMonth, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.dex_daily_archive),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (!isPremium) {
                        Spacer(Modifier.width(6.dp))
                        ProChip(onClick = onOpenArchive)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (record.finished && content != null) {
                    FilledTonalButton(onClick = { sharing = true }, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                        Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.dex_daily_share), maxLines = 1)
                    }
                } else {
                    Button(onClick = { onPlay(!started) }, enabled = content != null) {
                        Text(stringResource(if (started) R.string.dex_daily_continue else R.string.dex_daily_play), maxLines = 1)
                    }
                }
            }
        }
    }

    if (sharing && content != null) {
        DexDailyShareDialog(DexDailyCalendar.number(today), content.artworkUrl, record, streak) { sharing = false }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Play sheet
// ─────────────────────────────────────────────────────────────────────────────

/** Plays the puzzle for [date] (today, or a past day from the archive). One guess. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexDailySheet(viewModel: DexDailyViewModel, date: String, onDismiss: () -> Unit) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val loads by viewModel.loads.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val record = records[date] ?: DexDailyRecord()
    val isPremium = LocalPremium.current
    val soundManager: SoundManager = koinInject()
    var sharing by remember { mutableStateOf(false) }
    var showPlans by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { fb ->
            if (fb is DexDailyFeedback.Finished) soundManager.play(
                if (fb.solved) SoundManager.Sound.CORRECT_ANSWER else SoundManager.Sound.WRONG_ANSWER
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.dex_daily_label, DexDailyCalendar.number(date)),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.trivia_subtitle),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            if (!record.finished) {
                Text(
                    stringResource(R.string.dex_daily_one_guess),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))

            when (val load = loads[date]) {
                is DexDailyLoad.Ready -> PuzzleBody(
                    content = load.content,
                    record = record,
                    isArchive = record.archive || date != today,
                    isPremium = isPremium,
                    streak = streak,
                    onGuess = { viewModel.guess(date, it) },
                    onUseHints = { viewModel.useHints(date, isPremium) },
                    onLockedHints = { showPlans = true },
                    onShare = { sharing = true },
                    onClose = onDismiss
                )
                DexDailyLoad.Failed -> {
                    Text(stringResource(R.string.trivia_error), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { viewModel.retry(date) }) { Text(stringResource(R.string.action_retry)) }
                }
                else -> Box(Modifier.height(260.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }

    val content = (loads[date] as? DexDailyLoad.Ready)?.content
    if (sharing && content != null) {
        DexDailyShareDialog(DexDailyCalendar.number(date), content.artworkUrl, record, if (record.archive) 0 else streak) { sharing = false }
    }
    if (showPlans) PremiumBottomSheet(onDismiss = { showPlans = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PuzzleBody(
    content: DexDailyContent,
    record: DexDailyRecord,
    isArchive: Boolean,
    isPremium: Boolean,
    streak: Int,
    onGuess: (Int) -> Unit,
    onUseHints: () -> Unit,
    onLockedHints: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
) {
    ShadowStage(artworkUrl = content.artworkUrl, revealed = record.finished, size = 200.dp)
    Spacer(Modifier.height(10.dp))

    AnimatedVisibility(visible = record.finished, enter = fadeIn() + expandVertically()) {
        Text(
            if (record.solved) stringResource(R.string.dex_daily_its, content.answerName)
            else stringResource(R.string.trivia_it_was, content.answerName),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = if (record.solved) DexGreen else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }

    // Hints: shown after the guess for everyone; before it only for Pro, by choice.
    val clues = DexDailyRules.cluesFor(record)
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        when {
            clues.isNotEmpty() -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                content.types.forEach { type -> ClueChip(localizedTypeName(type), pokemonTypeColor(type)) }
                ClueChip(stringResource(R.string.trivia_generation, content.generation), MaterialTheme.colorScheme.tertiary)
                ClueChip(stringResource(R.string.dex_daily_clue_letter, content.answerName.take(1)), MaterialTheme.colorScheme.secondary)
            }
            isPremium -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedButton(onClick = onUseHints) {
                    Icon(Icons.Default.Lightbulb, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.dex_daily_show_hints))
                }
                if (!isArchive) {
                    Text(
                        stringResource(R.string.dex_daily_hints_cost),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(onClick = onLockedHints)
                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.dex_daily_hints_locked),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                ProChip(onClick = onLockedHints)
            }
        }
    }

    // Six choices, two columns.
    content.options.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (id, name) ->
                val state = when {
                    id in record.wrongIds -> OptionState.Wrong
                    record.finished && id == content.puzzle.answerId -> OptionState.Correct
                    record.finished -> OptionState.Dimmed
                    else -> OptionState.Open
                }
                OptionButton(name, state, Modifier.weight(1f)) { onGuess(id) }
            }
        }
    }

    AnimatedVisibility(visible = record.finished, enter = fadeIn(tween(400)) + expandVertically()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 14.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ResultBadge(record.solved, size = 22.dp)
                Text(
                    stringResource(if (record.solved) R.string.dex_daily_got_it else R.string.dex_daily_missed),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val xp = DexDailyRules.xpFor(record)
                if (xp > 0) Text("+$xp XP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                if (!isArchive && streak > 0) StreakChip(streak)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (isArchive) stringResource(R.string.dex_daily_archive_note)
                else stringResource(R.string.dex_daily_next_in, rememberNextPuzzleCountdown()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_close), maxLines = 1)
                }
                Button(onClick = onShare, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.dex_daily_share), maxLines = 1)
                }
            }
        }
    }
}

private enum class OptionState { Open, Wrong, Correct, Dimmed }

@Composable
private fun OptionButton(text: String, state: OptionState, modifier: Modifier, onClick: () -> Unit) {
    val (container, content, border) = when (state) {
        OptionState.Open -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface, null)
        OptionState.Wrong -> Triple(DexRed.copy(alpha = 0.14f), DexRed, BorderStroke(1.5.dp, DexRed.copy(alpha = 0.6f)))
        OptionState.Correct -> Triple(DexGreen.copy(alpha = 0.18f), DexGreen, BorderStroke(1.5.dp, DexGreen))
        OptionState.Dimmed -> Triple(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), null)
    }
    Surface(
        onClick = onClick,
        enabled = state == OptionState.Open,
        shape = RoundedCornerShape(14.dp),
        color = container,
        border = border,
        modifier = modifier.heightIn(min = 52.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            when (state) {
                OptionState.Wrong -> Icon(Icons.Default.Close, null, Modifier.size(16.dp), tint = content)
                OptionState.Correct -> Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = content)
                else -> {}
            }
            if (state == OptionState.Wrong || state == OptionState.Correct) Spacer(Modifier.width(4.dp))
            Text(text, color = content, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ClueChip(text: String, color: Color) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = color,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Share dialog: shows the card being shared, then hands it to the share sheet.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DexDailyShareDialog(number: Int, artworkUrl: String, record: DexDailyRecord, streak: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    var busy by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DexDailyShareCard(
                number = number, artworkUrl = artworkUrl, record = record, streak = streak,
                modifier = Modifier.drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
                Button(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                shareDexDaily(context, layer, number, record)
                                onDismiss()
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.dex_daily_share))
                }
            }
        }
    }
}
