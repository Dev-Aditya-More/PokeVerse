package com.aditya1875.pokeverse.screens.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.data.remote.model.PokemonResponse

// Replace the IconButton in your Pokemon detail page with this:

@Composable
fun PokemonActionsMenu(
    pokemon: PokemonResponse?,
    isInTeam: Boolean,
    isInFavorites: Boolean,
    teamSize: Int,
    onAddToTeam: () -> Unit,
    onRemoveFromTeam: () -> Unit,
    onAddToFavorites: () -> Unit,
    onRemoveFromFavorites: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = Color.White
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF2A2A2A))
                .width(220.dp)
        ) {
            // Team Option
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isInTeam) Icons.Default.Remove else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isInTeam) Color.Red else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isInTeam) "Remove from Team" else "Add to Team",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            if (!isInTeam && teamSize >= 6) {
                                Text(
                                    text = "Team is full (6/6)",
                                    color = Color.Red.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                onClick = {
                    expanded = false
                    if (isInTeam) {
                        onRemoveFromTeam()
                    } else {
                        if (teamSize < 6) {
                            onAddToTeam()
                        }
                    }
                },
                enabled = isInTeam || teamSize < 6,
                colors = MenuDefaults.itemColors(
                    textColor = Color.White,
                    disabledTextColor = Color.White.copy(alpha = 0.4f)
                )
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.1f),
                thickness = 1.dp
            )

            // Favorites Option
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isInFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (isInFavorites) MaterialTheme.colorScheme.primary.copy(0.9f) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (isInFavorites) "Remove from Favorites" else "Add to Favorites",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                onClick = {
                    expanded = false
                    if (isInFavorites) {
                        onRemoveFromFavorites()
                    } else {
                        onAddToFavorites()
                    }
                },
                colors = MenuDefaults.itemColors(
                    textColor = Color.White,
                )
            )
        }
    }
}
