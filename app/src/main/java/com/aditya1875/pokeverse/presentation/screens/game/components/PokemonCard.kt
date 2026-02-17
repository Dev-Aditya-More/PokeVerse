package com.aditya1875.pokeverse.presentation.screens.game.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aditya1875.pokeverse.utils.CardState

// PokemonCard.kt
@Composable
fun PokemonCard(
    card: CardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "cardFlip"
    )

    val matchScale by animateFloatAsState(
        targetValue = if (card.isMatched) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "matchScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = matchScale
                scaleY = matchScale
            }
            .clickable(
                enabled = !card.isFlipped && !card.isMatched,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Show back when rotation < 90 degrees
        if (rotation <= 90f) {
            CardBack(
                modifier = Modifier.graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
            )
        } else {
            // Show front when rotation > 90 degrees (mirrored)
            CardFront(
                card = card,
                modifier = Modifier.graphicsLayer {
                    rotationY = rotation - 180f
                    cameraDistance = 12f * density
                }
            )
        }
    }
}

@Composable
private fun CardBack(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Pokeball design
            Canvas(modifier = Modifier.size(40.dp)) {
                val strokeWidth = 3.dp.toPx()
                val radius = size.minDimension / 2

                // Top half
                drawArc(
                    color = Color(0xFFCC0000),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Fill
                )
                // Bottom half
                drawArc(
                    color = Color.White,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Fill
                )
                // Middle line
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = strokeWidth
                )
                // Center circle
                drawCircle(
                    color = Color.Black,
                    radius = radius * 0.28f,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
                drawCircle(
                    color = Color.White,
                    radius = radius * 0.18f,
                    center = center
                )
            }
        }
    }
}

@Composable
private fun CardFront(
    card: CardState,
    modifier: Modifier = Modifier
) {
    val glowColor = if (card.isMatched)
        MaterialTheme.colorScheme.primary
    else
        Color.Transparent

    Card(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (card.isMatched) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (card.isMatched)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(card.spriteUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = card.pokemonName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            )

            // Match overlay
            if (card.isMatched) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                )
            }
        }
    }
}