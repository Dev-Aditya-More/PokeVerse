package com.aditya1875.pokeverse.specialscreens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun IceParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    baseSize: Dp = 4.dp,
    color: Color = Color(0xFFBEE6FF).copy(alpha = 0.7f)
) {
    val density = LocalDensity.current
    val baseSizePx = with(density) { baseSize.toPx() }

    // Particle data with randomized starting positions and phases
    val particles = remember {
        List(particleCount) {
            IceParticle(
                x = Random.nextFloat(), // normalized horizontal [0..1]
                y = Random.nextFloat(), // normalized vertical [0..1]
                sizeFactor = 0.5f + Random.nextFloat(), // scale size 0.5 to 1.5
                swayPhase = Random.nextFloat() * 2 * Math.PI.toFloat(),
                alpha = 0.4f + Random.nextFloat() * 0.6f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing)
        )
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val widthPx = size.width
        val heightPx = size.height

        val fallSpeed = 0.05f // fraction of screen per animation cycle (slow)

        particles.forEach { p ->
            // Vertical position moves downward over time with wrap-around
            val yPos = ((p.y + time * fallSpeed) % 1f) * heightPx
            // Horizontal sway using sine wave, amplitude ~10px
            val swayAmplitude = 10f
            val xPos = (p.x * widthPx) + swayAmplitude * sin((time * 2 * Math.PI + p.swayPhase)).toFloat()

            val size = baseSizePx * p.sizeFactor
            drawCircle(
                color = color.copy(alpha = p.alpha),
                radius = size / 2,
                center = Offset(xPos, yPos)
            )
        }
    }
}

private data class IceParticle(
    val x: Float,
    val y: Float,
    val sizeFactor: Float,
    val swayPhase: Float,
    val alpha: Float
)
