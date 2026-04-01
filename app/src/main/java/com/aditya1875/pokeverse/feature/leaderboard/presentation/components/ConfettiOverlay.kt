package com.aditya1875.pokeverse.feature.leaderboard.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ConfettiOverlay(rank: Int) {
    val message = when (rank) {
        1 -> "👑 You're #1! Legendary Trainer!"
        2 -> "🥈 Amazing! You're in 2nd place!"
        else -> "🥉 Great job! You're in 3rd place!"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val offset by infiniteTransition.animateFloat(
        0f, 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "fall"
    )

    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {}) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = listOf(
                Color(0xFFFFD700), Color(0xFFFF6B6B),
                Color(0xFF4ECDC4), Color(0xFF45B7D1),
                Color(0xFF96CEB4), Color(0xFFDDA0DD)
            )
            repeat(60) { i ->
                val x = (i * 137.5f % size.width)
                val y = (offset * size.height + i * 40f) % size.height
                drawCircle(
                    color = colors[i % colors.size],
                    radius = 6f,
                    center = Offset(x, y),
                    alpha = 0.8f
                )
            }
        }

        // Message banner
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 12.dp
            ) {
                Text(
                    message,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}