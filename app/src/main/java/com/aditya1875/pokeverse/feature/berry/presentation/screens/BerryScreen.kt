package com.aditya1875.pokeverse.feature.berry.presentation.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.feature.berry.data.source.remote.model.BerryUiModel
import com.aditya1875.pokeverse.utils.pokemonTypeColor

@Composable
fun BerryGridCard(berry: BerryUiModel, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val typeColor = pokemonTypeColor(berry.naturalGiftType)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .graphicsLayer {
                shadowElevation = 8f
                shape = RoundedCornerShape(20.dp)
                clip = true
            }
            .background(
                Brush.verticalGradient(
                    listOf(typeColor.copy(alpha = 0.25f), Color.Transparent)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = berry.spriteUrl,
                    contentDescription = berry.displayName,
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    berry.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = typeColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        berry.naturalGiftType.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BerryGridSkeleton() {
    val shimmer by rememberInfiniteTransition(label = "s").animateFloat(
        0.3f, 0.7f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a"
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        items(18) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmer))
            )
        }
    }
}

@Composable
fun BerryListError(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text("Failed to load berries", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
