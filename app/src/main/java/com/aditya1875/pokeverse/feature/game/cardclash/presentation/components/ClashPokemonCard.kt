package com.aditya1875.pokeverse.feature.game.cardclash.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashPokemon
import org.koin.compose.koinInject

private val typeColors = mapOf(
    "fire" to Color(0xFFFF6B35),
    "water" to Color(0xFF4FC3F7),
    "grass" to Color(0xFF81C784),
    "electric" to Color(0xFFFFD54F),
    "psychic" to Color(0xFFF48FB1),
    "ice" to Color(0xFF80DEEA),
    "dragon" to Color(0xFF7986CB),
    "dark" to Color(0xFF616161),
    "fairy" to Color(0xFFF8BBD0),
    "fighting" to Color(0xFFFF7043),
    "poison" to Color(0xFFCE93D8),
    "ground" to Color(0xFFBCAAA4),
    "rock" to Color(0xFFA5D6A7),
    "ghost" to Color(0xFF7E57C2),
    "bug" to Color(0xFFAED581),
    "steel" to Color(0xFFB0BEC5),
    "flying" to Color(0xFF90CAF9),
    "normal" to Color(0xFFEEEEEE)
)

fun typeColor(type: String) = typeColors[type.lowercase()] ?: Color(0xFFEEEEEE)

@Composable
fun ClashPokemonCard(
    pokemon: ClashPokemon,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageLoader = koinInject<ImageLoader>()
    val primaryColor = typeColor(pokemon.types.firstOrNull() ?: "normal")
    val borderColor = when {
        isLocked -> Color(0xFF4CAF50)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> primaryColor.copy(alpha = 0.4f)
    }
    val borderWidth = if (isSelected || isLocked) 2.5.dp else 1.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.18f), MaterialTheme.colorScheme.surface)
                )
            )
            .clickable(enabled = !isLocked, onClick = onClick)
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = pokemon.spriteUrl,
                contentDescription = pokemon.name,
                imageLoader = imageLoader,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = pokemon.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                pokemon.types.take(2).forEach { type ->
                    TypeChip(type)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "BST ${pokemon.bst}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CardBack(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("?", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TypeChip(type: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(typeColor(type).copy(alpha = 0.8f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = type.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontSize = 9.sp
        )
    }
}
