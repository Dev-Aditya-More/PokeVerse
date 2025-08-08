package com.example.pokeverse.specialscreens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PoisonParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 30,
    color: Color = Color(0xFF6F42C1).copy(alpha = 0.4f) // purple-ish
) {
    val density = LocalDensity.current
    val baseSizePx = with(density) { 8.dp.toPx() }

    val particles = remember {
        List(particleCount) {
            PoisonParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                sizeFactor = 0.5f + Random.nextFloat(),
                alpha = 0.3f + Random.nextFloat() * 0.5f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing)
        )
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val widthPx = size.width
        val heightPx = size.height

        val bufferPx = baseSizePx * 3

        particles.forEach { p ->
            var yPos = (p.y * heightPx) - (time * heightPx)

            // Wrap early: if above -bufferPx, wrap to bottom + bufferPx
            if (yPos < -bufferPx) yPos += heightPx + bufferPx

            val xPos = p.x * widthPx + 5 * sin((time * 2 * Math.PI + p.x * 10).toDouble()).toFloat()
            val size = baseSizePx * p.sizeFactor

            drawCircle(
                color = color.copy(alpha = p.alpha),
                radius = size / 2,
                center = Offset(xPos, yPos)
            )
        }
    }

}

private data class PoisonParticle(val x: Float, val y: Float, val sizeFactor: Float, val alpha: Float)
