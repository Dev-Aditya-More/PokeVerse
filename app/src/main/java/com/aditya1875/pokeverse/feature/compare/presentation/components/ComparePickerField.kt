package com.aditya1875.pokeverse.feature.compare.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.feature.pokemon.home.presentation.components.SuggestionRow
import com.aditya1875.pokeverse.utils.SearchUiState
import com.aditya1875.pokeverse.utils.pokemonTypeColor

/**
 * One side of the compare tool's Pokémon picker — a search field with a
 * suggestion dropdown before a pick is made, or a glowing type-tinted card
 * (with a way to swap the pick) afterward.
 */
@Composable
fun ComparePickerField(
    label: String,
    accent: Color,
    query: String,
    searchState: SearchUiState,
    selected: PokemonResponse?,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (selected != null) {
            val color = pokemonTypeColor(selected.types.firstOrNull()?.type?.name ?: "normal")
            val sprite = selected.sprites.other?.officialArtwork?.frontDefault
                ?: selected.sprites.front_default

            val scale = remember(selected.name) { Animatable(0.8f) }
            LaunchedEffect(selected.name) {
                scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                border = BorderStroke(1.5.dp, color.copy(alpha = 0.5f)),
                modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.08f))
                            )
                        )
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Change pick", tint = color)
                        }
                    }
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent))
                                )
                        )
                        AsyncImage(
                            model = sprite,
                            contentDescription = selected.name,
                            modifier = Modifier.size(76.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Text(
                        text = selected.name.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(label) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accent) },
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accent, strokeWidth = 2.dp)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = accent,
                    focusedLabelColor = accent,
                    cursorColor = accent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (searchState.showSuggestions) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    Column {
                        searchState.suggestions.forEach { result ->
                            SuggestionRow(searchResult = result, onClick = { onSelect(result.pokemon.name) })
                        }
                    }
                }
            }
        }
    }
}
