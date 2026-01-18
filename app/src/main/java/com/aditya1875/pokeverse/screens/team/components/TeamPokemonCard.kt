package com.aditya1875.pokeverse.screens.team.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.aditya1875.pokeverse.data.local.entity.TeamMemberEntity
import com.aditya1875.pokeverse.ui.viewmodel.PokemonViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun TeamPokemonCard(
    pokemon: TeamMemberEntity,
    navController: NavController,
    onRemove: (TeamMemberEntity) -> Unit,
    viewModel: PokemonViewModel = koinViewModel()
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "cardScale"
    )

    val team by viewModel.team.collectAsStateWithLifecycle()
    val teamMembershipMap = remember(team) { team.associate { it.name to true } }
    val isInTeam = teamMembershipMap[pokemon.name] ?: false

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                onClick = {
                    isPressed = true
                    navController.navigate("pokemon_detail/${pokemon.name}")
                },
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(pokemon.imageUrl)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = pokemon.name,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.15f))
            )

            Text(
                text = pokemon.name.replaceFirstChar { it.uppercase() },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            IconButton(onClick = { if (isInTeam) onRemove(pokemon) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from Team",
                    tint = Color(0xFFFF4444)
                )
            }
        }
    }
}