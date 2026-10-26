package com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.core.ui.components.DialogueBox
import com.aditya1875.pokeverse.feature.core.ui.components.darkFieldColors
import com.aditya1875.pokeverse.feature.core.ui.components.rememberTypewriter
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile
import com.aditya1875.pokeverse.utils.SoundManager
import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import org.koin.compose.koinInject

/** How a trainer likes to play — shown as a chip on the profile. [id] is what gets persisted. */
enum class PlayStyle(
    val id: String,
    val emoji: String,
    @StringRes val label: Int,
    @StringRes val desc: Int,
) {
    CATCHER("catcher", "🎯", R.string.tc_style_catcher, R.string.tc_style_catcher_desc),
    BATTLER("battler", "⚔️", R.string.tc_style_battler, R.string.tc_style_battler_desc),
    COLLECTOR("collector", "📖", R.string.tc_style_collector, R.string.tc_style_collector_desc),
    SCHOLAR("scholar", "🔬", R.string.tc_style_scholar, R.string.tc_style_scholar_desc);

    companion object {
        fun fromId(id: String): PlayStyle? = entries.firstOrNull { it.id == id }
    }
}

private enum class Step { INTRO, NAME, TYPE, STYLE, PHRASE, CARD }

private val TRAINER_TYPES = listOf(
    "normal", "fire", "water", "electric", "grass", "ice",
    "fighting", "poison", "ground", "flying", "psychic", "bug",
    "rock", "ghost", "dragon", "dark", "steel", "fairy"
)

private val PHRASES = listOf(
    R.string.tc_phrase_1, R.string.tc_phrase_2, R.string.tc_phrase_3,
    R.string.tc_phrase_4, R.string.tc_phrase_5, R.string.tc_phrase_6,
)

private const val NAME_MAX = 16
private const val PHRASE_MAX = 40
private val InkBg = Color(0xFF0B0E17)
private val DialogueFill = Color(0xFF101626)

/**
 * The profile "Trainer Card" setup, staged like a Pokémon-game intro: a professor-style
 * dialogue box types out each question, then the answer controls appear. Tapping the box
 * skips the typing. Existing trainers skip the intro and start at their name.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainerCardSetup(
    profile: UserProfile,
    onSave: (name: String, favoriteType: String, playStyle: String, catchphrase: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sound: SoundManager = koinInject()
    val hasName = profile.username.isNotBlank() && profile.username != "Trainer" && profile.username != "Guest Trainer"

    var step by remember { mutableStateOf(if (hasName) Step.NAME else Step.INTRO) }
    var name by remember { mutableStateOf(if (hasName) profile.username else "") }
    var type by remember { mutableStateOf(profile.favoriteType) }
    var style by remember { mutableStateOf(PlayStyle.fromId(profile.playStyle)) }
    var phrase by remember { mutableStateOf(profile.bio) }
    var writingOwn by remember { mutableStateOf(false) }

    val presetTexts = PHRASES.map { stringResource(it) }
    // Open the free-text field if the saved bio isn't one of the presets.
    LaunchedEffect(Unit) {
        if (profile.bio.isNotBlank() && profile.bio !in presetTexts) writingOwn = true
    }

    val accent = if (type.isNotEmpty()) pokemonTypeColor(type) else MaterialTheme.colorScheme.primary
    val safeName = name.trim().ifBlank { profile.username }
    val typeLabel = if (type.isNotEmpty()) localizedTypeName(type) else ""
    val styleLabel = style?.let { stringResource(it.label) } ?: ""

    val prompt = when (step) {
        Step.INTRO -> stringResource(R.string.tc_intro)
        Step.NAME -> stringResource(R.string.tc_ask_name)
        Step.TYPE -> stringResource(R.string.tc_ask_type, safeName)
        Step.STYLE -> stringResource(R.string.tc_ask_style, typeLabel)
        Step.PHRASE -> stringResource(R.string.tc_ask_phrase)
        Step.CARD -> stringResource(R.string.tc_done_line, safeName)
    }

    val typewriter = rememberTypewriter(prompt)
    val typing = typewriter.typing

    val canAdvance = when (step) {
        Step.INTRO, Step.CARD -> true
        Step.NAME -> name.isNotBlank()
        Step.TYPE -> type.isNotEmpty()
        Step.STYLE -> style != null
        Step.PHRASE -> phrase.isNotBlank()
    }

    fun advance() {
        sound.play(SoundManager.Sound.BUTTON_CLICK)
        val next = Step.entries.getOrNull(step.ordinal + 1)
        if (next != null) {
            step = next
        } else {
            sound.play(SoundManager.Sound.LEVEL_UP)
            onSave(name.trim(), type, style?.id ?: "", phrase.trim())
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(InkBg, accent.copy(alpha = 0.22f), InkBg)))
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Step.entries.forEach { s ->
                        Box(
                            Modifier
                                .size(width = if (s == step) 22.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (s.ordinal <= step.ordinal) accent else Color.White.copy(alpha = 0.18f)
                                )
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.tc_close),
                        tint = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
                },
                label = "trainerStep",
            ) { current ->
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AnimatedVisibility(visible = !typing || current != step, enter = fadeIn()) {
                        when (current) {
                            Step.INTRO -> Text("🔬", fontSize = 96.sp)

                            Step.NAME -> OutlinedTextField(
                                value = name,
                                onValueChange = { name = it.take(NAME_MAX) },
                                label = { Text(stringResource(R.string.profile_name_label)) },
                                singleLine = true,
                                colors = darkFieldColors(accent),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Step.TYPE -> FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                TRAINER_TYPES.forEach { t ->
                                    TypePill(
                                        label = localizedTypeName(t),
                                        color = pokemonTypeColor(t),
                                        selected = type == t,
                                        onClick = {
                                            sound.play(SoundManager.Sound.BUTTON_CLICK)
                                            type = t
                                        },
                                    )
                                }
                            }

                            Step.STYLE -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PlayStyle.entries.forEach { s ->
                                    ChoiceRow(
                                        selected = style == s,
                                        accent = accent,
                                        onClick = {
                                            sound.play(SoundManager.Sound.BUTTON_CLICK)
                                            style = s
                                        },
                                    ) {
                                        Text(s.emoji, fontSize = 28.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                stringResource(s.label),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                            )
                                            Text(
                                                stringResource(s.desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.65f),
                                            )
                                        }
                                    }
                                }
                            }

                            Step.PHRASE -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                presetTexts.forEach { text ->
                                    ChoiceRow(
                                        selected = !writingOwn && phrase == text,
                                        accent = accent,
                                        onClick = {
                                            sound.play(SoundManager.Sound.BUTTON_CLICK)
                                            writingOwn = false
                                            phrase = text
                                        },
                                    ) {
                                        Text("“$text”", color = Color.White, fontStyle = FontStyle.Italic)
                                    }
                                }
                                ChoiceRow(
                                    selected = writingOwn,
                                    accent = accent,
                                    onClick = {
                                        sound.play(SoundManager.Sound.BUTTON_CLICK)
                                        if (!writingOwn) phrase = ""
                                        writingOwn = true
                                    },
                                ) {
                                    Text(
                                        stringResource(R.string.tc_phrase_own),
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                AnimatedVisibility(visible = writingOwn) {
                                    OutlinedTextField(
                                        value = phrase,
                                        onValueChange = { phrase = it.take(PHRASE_MAX) },
                                        label = { Text(stringResource(R.string.tc_phrase_own_hint)) },
                                        supportingText = {
                                            Text(stringResource(R.string.tc_phrase_count, phrase.length, PHRASE_MAX))
                                        },
                                        maxLines = 2,
                                        colors = darkFieldColors(accent),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }

                            Step.CARD -> TrainerCardPreview(
                                name = safeName,
                                level = profile.level,
                                type = type,
                                style = style,
                                phrase = phrase.trim(),
                                accent = accent,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            DialogueBox(typewriter = typewriter, accent = accent)

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val prev = Step.entries.getOrNull(step.ordinal - 1)
                if (prev != null && !(hasName && prev == Step.INTRO)) {
                    TextButton(onClick = { step = prev }) {
                        Text(stringResource(R.string.tc_back), color = Color.White.copy(alpha = 0.7f))
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = ::advance,
                    enabled = canAdvance && !typing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text(
                        when {
                            step != Step.CARD -> stringResource(R.string.tc_next)
                            hasName -> stringResource(R.string.tc_confirm_edit)
                            else -> stringResource(R.string.tc_confirm)
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TypePill(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else color.copy(alpha = 0.14f))
            .border(BorderStroke(1.5.dp, color), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (selected) Color.Black else Color.White,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun ChoiceRow(
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f))
            .border(
                BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else Color.White.copy(alpha = 0.15f)),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) { content() }
}

/** The finished card — used as the last step and reusable anywhere the card should be shown. */
@Composable
fun TrainerCardPreview(
    name: String,
    level: Int,
    type: String,
    style: PlayStyle?,
    phrase: String,
    accent: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.55f), DialogueFill)))
            .border(BorderStroke(2.dp, accent), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(R.string.tc_card_title).uppercase(),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (type.isNotEmpty()) TrainerChip(localizedTypeName(type), pokemonTypeColor(type))
            if (style != null) TrainerChip("${style.emoji} ${stringResource(style.label)}", Color.White)
            TrainerChip(stringResource(R.string.level_short, level), accent)
        }
        if (phrase.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "“$phrase”",
                color = Color.White,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/** Small outlined chip used on the card and under the bio on the profile header. */
@Composable
fun TrainerChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.8f)), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
