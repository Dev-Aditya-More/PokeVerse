package com.aditya1875.pokeverse.feature.game.battle.presentation

import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.battle.data.SavedTeam
import com.aditya1875.pokeverse.feature.game.battle.data.SavedTeamsRepository

private val Gold = Color(0xFFFFC24B)

private fun art(id: Int) =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"

/**
 * "My teams": saved teams as cards, one tap to load a whole team, plus a button to save the team that is
 * picked right now. Teams that name Pokémon you no longer have say how many are still ready.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun SavedTeamsSection(
    saved: List<SavedTeam>,
    roster: Set<Int>,
    selected: List<Int>,
    activeName: String?,
    onLoad: (SavedTeam) -> Unit,
    onSave: (String) -> Unit,
    onDelete: (SavedTeam) -> Unit,
    onQuickFill: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var naming by remember { mutableStateOf(false) }
    val full = saved.size >= SavedTeamsRepository.MAX_TEAMS

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.battle_teams_title),
                color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${saved.size}/${SavedTeamsRepository.MAX_TEAMS}",
                color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium,
            )
        }
        // Building a team: fill it for you, clear it, save it, or save changes over the team you loaded.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TeamAction(Icons.Default.Shuffle, stringResource(R.string.battle_team_quickfill), enabled = true, onClick = onQuickFill)
            TeamAction(Icons.Default.Clear, stringResource(R.string.battle_team_clear), enabled = selected.isNotEmpty(), onClick = onClear)
            val activeTeam = saved.firstOrNull { it.name == activeName }
            if (activeTeam != null && selected.isNotEmpty() && activeTeam.ids != selected) {
                TeamAction(Icons.Default.Save, stringResource(R.string.battle_team_update, activeTeam.name), enabled = true) { onSave(activeTeam.name) }
            }
            TeamAction(Icons.Default.Save, stringResource(R.string.battle_team_save), enabled = selected.isNotEmpty() && !full) { naming = true }
        }
        if (saved.isEmpty()) {
            Text(
                stringResource(R.string.battle_teams_empty),
                color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(saved, key = { it.name }) { team ->
                    TeamCard(
                        team = team,
                        ready = team.ids.count { it in roster },
                        active = team.ids.toSet() == selected.toSet(),
                        onLoad = { onLoad(team) },
                        onDelete = { onDelete(team) },
                    )
                }
            }
            if (full) {
                Text(
                    stringResource(R.string.battle_teams_full, SavedTeamsRepository.MAX_TEAMS),
                    color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }

    if (naming) {
        val defaultName = stringResource(R.string.battle_team_default_name, saved.size + 1)
        var name by remember { mutableStateOf(defaultName) }
        AlertDialog(
            onDismissRequest = { naming = false },
            title = { Text(stringResource(R.string.battle_team_save_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(SavedTeamsRepository.MAX_NAME) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.battle_team_name_hint)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, cursorColor = Gold),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = SavedTeamsRepository.cleanName(name).isNotEmpty(),
                    onClick = { onSave(name); naming = false }
                ) { Text(stringResource(R.string.battle_team_save_confirm)) }
            },
            dismissButton = { TextButton(onClick = { naming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun TeamAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            labelColor = Color.White,
            leadingIconContentColor = Gold,
            disabledLabelColor = Color.White.copy(alpha = 0.35f),
            disabledLeadingIconContentColor = Color.White.copy(alpha = 0.25f),
        ),
        border = androidx.compose.material3.AssistChipDefaults.assistChipBorder(
            enabled = enabled,
            borderColor = Gold.copy(alpha = 0.5f),
            disabledBorderColor = Color.White.copy(alpha = 0.14f),
        ),
    )
}

@Composable
private fun TeamCard(team: SavedTeam, ready: Int, active: Boolean, onLoad: () -> Unit, onDelete: () -> Unit) {
    val deleteLabel = stringResource(R.string.battle_team_delete, team.name)
    Box(
        Modifier
            .width(168.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) Gold.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
            .border(BorderStroke(if (active) 2.dp else 1.dp, if (active) Gold else Color.White.copy(alpha = 0.16f)), RoundedCornerShape(16.dp))
            .clickable(onClick = onLoad)
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                team.name, color = Color.White, fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 28.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                team.ids.forEach { id ->
                    AsyncImage(art(id), contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Text(
                if (ready == team.ids.size) stringResource(R.string.battle_team_ready, ready)
                else stringResource(R.string.battle_team_partly, ready, team.ids.size),
                color = if (ready == team.ids.size) Color.White.copy(alpha = 0.65f) else Color(0xFFFFB74D),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.align(Alignment.TopEnd).size(28.dp).semantics { contentDescription = deleteLabel },
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
    }
}
