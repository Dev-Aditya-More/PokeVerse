package com.aditya1875.pokeverse.presentation.specialscreens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


@Composable
fun BugParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 40,
    color: Color = Color(0xFFBFFF00).copy(alpha = 0.6f) // yellow-green glow
) {
    val density = LocalDensity.current
    val baseSizePx = with(density) { 6.dp.toPx() }

    val particles = remember {
        List(particleCount) {
            BugParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                baseAlpha = 0.3f + Random.nextFloat() * 0.5f,
                sizeFactor = 0.5f + Random.nextFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val alphaAnim = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing)
        )
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val widthPx = size.width
        val heightPx = size.height

        particles.forEach { p ->
            val xPos = p.x * widthPx + 5 * sin((alphaAnim.value * 2 * Math.PI + p.x * 10)).toFloat()
            val yPos = p.y * heightPx + 5 * cos((alphaAnim.value * 3 * Math.PI + p.y * 10)).toFloat()
            val alpha = p.baseAlpha * (0.5f + 0.5f * sin(alphaAnim.value * 2 * Math.PI * 2 + p.x * 20))

            val size = baseSizePx * p.sizeFactor

            drawCircle(
                color = color.copy(alpha = alpha.coerceIn(0.0, 1.0).toFloat()),
                radius = size / 2,
                center = Offset(xPos, yPos)
            )
        }
    }
}

private data class BugParticle(val x: Float, val y: Float, val baseAlpha: Float, val sizeFactor: Float)
