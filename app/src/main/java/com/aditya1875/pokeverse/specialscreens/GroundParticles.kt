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
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GroundParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 20,
    baseColor: Color = Color(0xFFB97A56).copy(alpha = 0.3f)
) {
    val density = LocalDensity.current
    val baseSizePx = with(density) { 12.dp.toPx() }

    // Particle with spawn time to manage lifetime
    data class DustParticle(
        val x: Float,
        val startTime: Float, // 0..1 normalized spawn time
        val sizeFactor: Float,
        val swayPhase: Float,
        val alphaPhase: Float
    )

    val particles = remember {
        List(particleCount) {
            DustParticle(
                x = Random.nextFloat(),
                startTime = Random.nextFloat(),
                sizeFactor = 0.7f + Random.nextFloat(),
                swayPhase = Random.nextFloat() * 2 * Math.PI.toFloat(),
                alphaPhase = Random.nextFloat() * 2 * Math.PI.toFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val globalTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing)
        )
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val widthPx = size.width
        val heightPx = size.height

        particles.forEach { p ->
            // Calculate normalized particle life progress 0..1
            var lifeProgress = (globalTime - p.startTime)
            if (lifeProgress < 0f) lifeProgress += 1f

            // Vertical position moves up over life, starting from bottom
            val yPos = heightPx * (1f - lifeProgress)

            // Horizontal sway with swayPhase for randomness
            val swayAmplitude = 10f
            val xPos = p.x * widthPx + swayAmplitude * sin((lifeProgress * 2 * Math.PI + p.swayPhase)).toFloat()

            // Size grows with life progress (dust expands)
            val size = baseSizePx * p.sizeFactor * (0.5f + lifeProgress)

            // Alpha fades out near end of life, with small pulsing
            val alpha = baseColor.alpha * (1f - lifeProgress) * (0.7f + 0.3f * sin(lifeProgress * 10 * Math.PI + p.alphaPhase))

            // Draw layered circles to simulate a dust puff (fuzzy cloud)
            val layers = 3
            for (i in 0 until layers) {
                val layerSize = size * (1f + i * 0.3f)
                drawCircle(
                    color = baseColor.copy(alpha = (alpha * (1f - i * 0.3f)).toFloat()),
                    radius = layerSize / 2,
                    center = Offset(xPos, yPos)
                )
            }
        }
    }
}

private data class GroundParticle(val x: Float, val y: Float, val sizeFactor: Float, val alpha: Float)
