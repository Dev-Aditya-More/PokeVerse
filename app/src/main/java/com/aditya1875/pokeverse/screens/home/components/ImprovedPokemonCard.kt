package com.aditya1875.pokeverse.screens.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aditya1875.pokeverse.data.remote.model.PokemonResult
import kotlinx.coroutines.delay

@Composable
fun ImprovedPokemonCard(
    pokemon: PokemonResult,
    isInTeam: Boolean,
    isInFavorites: Boolean,
    teamSize: Int,
    onAddToTeam: () -> Unit,
    onRemoveFromTeam: () -> Unit,
    onAddToFavorites: () -> Unit,
    onRemoveFromFavorites: () -> Unit,
    onClick: () -> Unit
) {
    val pokemonId = remember(pokemon.url) {
        pokemon.url.trimEnd('/')
            .split("/")
            .lastOrNull()
            ?.toIntOrNull() ?: 0
    }

    val spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$pokemonId.png"

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "cardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable {
                isPressed = true
                onClick()
            },
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pokemon Sprite
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(spriteUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = pokemon.name,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.width(16.dp))

            // Pokemon Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = pokemon.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "#${pokemonId.toString().padStart(4, '0')}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            // Action Buttons Column
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Team Button
                IconButton(
                    onClick = {
                        if (isInTeam) {
                            onRemoveFromTeam()
                        } else {
                            if (teamSize < 6) {
                                onAddToTeam()
                            }
                        }
                    },
                    enabled = isInTeam || teamSize < 6,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isInTeam) Icons.Default.Remove else Icons.Default.Add,
                        contentDescription = if (isInTeam) "Remove from Team" else "Add to Team",
                        tint = if (isInTeam) {
                            MaterialTheme.colorScheme.error
                        } else {
                            if (teamSize >= 6) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Favorites Button
                IconButton(
                    onClick = {
                        if (isInFavorites) {
                            onRemoveFromFavorites()
                        } else {
                            onAddToFavorites()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isInFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isInFavorites) "Remove from Favorites" else "Add to Favorites",
                        tint = if (isInFavorites) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(150)
            isPressed = false
        }
    }
}