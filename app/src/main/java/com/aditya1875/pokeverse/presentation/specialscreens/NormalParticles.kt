package com.aditya1875.pokeverse.presentation.specialscreens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun NormalParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 20,
    baseColor: Color = Color(0xFFC4C3A5).copy(alpha = 0.5f)
) {
    val density = LocalDensity.current
    val baseSizePx = with(density) { 14.dp.toPx() }

    data class NormalParticle(
        val x: Float,
        val yStart: Float,
        val speed: Float,
        val sizeFactor: Float,
        val alphaPhase: Float,
        val driftPhase: Float
    )

    val particles = remember {
        List(particleCount) {
            NormalParticle(
                x = Random.nextFloat(),
                yStart = Random.nextFloat(),
                speed = 0.02f + Random.nextFloat() * 0.015f,
                sizeFactor = 0.8f + Random.nextFloat() * 0.5f,
                alphaPhase = Random.nextFloat() * (2 * Math.PI).toFloat(),
                driftPhase = Random.nextFloat() * (2 * Math.PI).toFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val globalTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing)
        )
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val widthPx = size.width
        val heightPx = size.height

        particles.forEach { p ->
            var t = (globalTime + p.alphaPhase / (2 * Math.PI).toFloat()) % 1f

            val yPos = heightPx * (1f - (t + p.yStart) % 1f)
            val drift = 25f * sin((t * 2 * Math.PI + p.driftPhase)).toFloat()
            val xPos = p.x * widthPx + drift

            val size = baseSizePx * p.sizeFactor
            val alpha = 0.4f + 0.3f * sin((t * 2 * Math.PI + p.alphaPhase)).toFloat()

            drawCircle(
                color = baseColor.copy(alpha = alpha),
                radius = size / 2f,
                center = Offset(xPos, yPos)
            )
        }
    }
}
