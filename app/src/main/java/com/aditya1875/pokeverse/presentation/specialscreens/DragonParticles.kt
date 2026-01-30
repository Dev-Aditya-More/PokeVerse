package com.aditya1875.pokeverse.presentation.specialscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Preview
@Composable
fun DragonParticles(
    modifier: Modifier = Modifier,
    maxParticles: Int = 35,
    spawnRateMillis: Long = 120L
) {
    data class Particle(
        val baseX: Float,
        val baseY: Float,
        val radius: Float,
        val color: Color,
        val phaseX: Float,
        val phaseY: Float,
        val lifetime: Long,
        val createdAt: Long,
        val pulseSpeed: Float
    )

    val density = LocalDensity.current
    val particles = remember { mutableStateListOf<Particle>() }
    val rnd = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < maxParticles) {
                particles += Particle(
                    baseX = rnd.nextFloat(),
                    baseY = 0.4f + rnd.nextFloat() * 0.6f,
                    radius = rnd.nextFloat() * 16.dp.toPxCustom(density)
                            + 10.dp.toPxCustom(density),
                    color = listOf(
                        Color(0xFFFF4500),
                        Color(0xFFFF6347),
                        Color(0xFFFFD700),
                        Color(0xFFDC143C)
                    ).random(rnd),
                    phaseX = rnd.nextFloat() * 10f,
                    phaseY = rnd.nextFloat() * 10f,
                    lifetime = rnd.nextLong(1400L, 2200L),
                    createdAt = System.currentTimeMillis(),
                    pulseSpeed = rnd.nextFloat() * 0.002f + 0.0006f
                )
            }
            delay(spawnRateMillis)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val now = System.currentTimeMillis()

        particles.removeAll { now - it.createdAt > it.lifetime }

        particles.forEach { p ->
            val t = ((now - p.createdAt) / p.lifetime.toFloat()).coerceIn(0f, 1f)

            val x =
                p.baseX +
                        0.02f * sin(t * 2 * Math.PI + p.phaseX).toFloat()

            val y =
                p.baseY -
                        t * 0.4f +
                        0.02f * cos(t * 2 * Math.PI + p.phaseY).toFloat()

            val fade = (t * (1f - t) * 4f).coerceIn(0f, 1f)
            val pulse = sin(now * p.pulseSpeed)
            val radius = p.radius * (1f + pulse * 0.05f)

            val center = Offset(x * w, y * h)

            drawCircle(
                color = p.color.copy(alpha = fade * 0.35f),
                radius = radius * 1.5f,
                center = center,
                blendMode = BlendMode.Plus
            )

            drawCircle(
                color = p.color.copy(alpha = fade),
                radius = radius,
                center = center,
                blendMode = BlendMode.Plus
            )
        }
    }
}

