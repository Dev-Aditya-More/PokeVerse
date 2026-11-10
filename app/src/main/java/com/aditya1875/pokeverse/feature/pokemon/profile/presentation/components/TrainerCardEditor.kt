package com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile
import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.utils.pokemonTypeColor

private const val EDIT_NAME_MAX = 16
private const val EDIT_PHRASE_MAX = 40

/**
 * Editing an existing profile: one short sheet with everything on it, no dialogue or staging.
 * It still covers the Trainer Card choices (favorite type, play style), because those are what
 * tell other players what kind of trainer this is. The staged [TrainerCardSetup] is only for the
 * very first setup.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrainerCardEditor(
    profile: UserProfile,
    onSave: (name: String, favoriteType: String, playStyle: String, catchphrase: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(profile.username) }
    var phrase by remember { mutableStateOf(profile.bio) }
    var type by remember { mutableStateOf(profile.favoriteType) }
    var style by remember { mutableStateOf(PlayStyle.fromId(profile.playStyle)) }

    val changed = name.trim() != profile.username || phrase.trim() != profile.bio ||
        type != profile.favoriteType || (style?.id ?: "") != profile.playStyle
    val canSave = name.isNotBlank() && changed

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.profile_edit_display_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(EDIT_NAME_MAX) },
                label = { Text(stringResource(R.string.profile_name_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = phrase,
                onValueChange = { phrase = it.take(EDIT_PHRASE_MAX) },
                label = { Text(stringResource(R.string.tc_phrase_own_hint)) },
                supportingText = { Text(stringResource(R.string.tc_phrase_count, phrase.length, EDIT_PHRASE_MAX)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.tc_edit_fav_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TRAINER_TYPE_IDS.forEach { t ->
                        val color = pokemonTypeColor(t)
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(localizedTypeName(t)) },
                            leadingIcon = null,
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.35f),
                            ),
                            border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = type == t,
                                borderColor = color.copy(alpha = 0.5f),
                                selectedBorderColor = color,
                            ),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.tc_edit_play_style),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PlayStyle.entries.forEach { s ->
                        FilterChip(
                            selected = style == s,
                            onClick = { style = s },
                            label = { Text("${s.emoji} ${stringResource(s.label)}") },
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(
                    enabled = canSave,
                    onClick = {
                        onSave(name.trim(), type, style?.id ?: "", phrase.trim())
                        onDismiss()
                    },
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
