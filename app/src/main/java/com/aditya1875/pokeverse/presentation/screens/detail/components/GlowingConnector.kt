package com.aditya1875.pokeverse.presentation.screens.detail.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

@Composable
fun EvolutionConnector(
    modifier: Modifier = Modifier,
    direction: EvolutionConnector.Direction,
    onClick: () -> Unit = {}
) {
    val infinite = rememberInfiniteTransition()
    val offset by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    Box(modifier = modifier.clickable { onClick() }) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2
            val start = if (direction == EvolutionConnector.Direction.LEFT) Offset(size.width, centerY) else Offset(0f, centerY)
            val end = if (direction == EvolutionConnector.Direction.LEFT) Offset(0f, centerY) else Offset(size.width, centerY)

            // glowing back layer
            drawLine(
                color = Color.White.copy(alpha = 0.06f),
                start = start,
                end = end,
                strokeWidth = 18f,
                cap = StrokeCap.Round
            )

            val bright = Color.White.copy(alpha = 0.9f)
            val dim = Color.White.copy(alpha = 0.25f)
            val brush = Brush.horizontalGradient(
                colors = if (direction == EvolutionConnector.Direction.LEFT)
                    listOf(bright, dim)
                else
                    listOf(dim, bright),
                startX = size.width * offset,
                endX = size.width * (offset + 0.4f)
            )

            drawLine(
                brush = brush,
                start = start,
                end = end,
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
    }
}

object EvolutionConnector {
    enum class Direction { LEFT, RIGHT }
}

