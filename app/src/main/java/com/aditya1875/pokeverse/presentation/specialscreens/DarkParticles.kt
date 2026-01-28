package com.aditya1875.pokeverse.presentation.specialscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Preview
@Composable
fun DarkParticles(
    modifier: Modifier = Modifier,
    maxParticles: Int = 45,
    spawnRateMillis: Long = 70L
) {
    data class Particle(
        var x: Float,
        var y: Float,
        val radius: Float,
        val color: Color,
        val lifetime: Long,
        val createdAt: Long,
        val jitter: Float
    )

    val particles = remember { mutableStateListOf<Particle>() }
    val rnd = remember { Random(System.currentTimeMillis()) }
    val density = LocalDensity.current

    // Spawn particles near center
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < maxParticles) {
                particles += Particle(
                    x = 0.3f + (rnd.nextFloat() - 0.5f) * 0.9f,
                    y = 0.5f + (rnd.nextFloat() - 0.5f) * 0.9f,

                    // bigger base sizes — gentle, wave-like orbs
                    radius = rnd.nextFloat() * 20.dp.toPxCustom(density) + 10.dp.toPxCustom(density),
                    color = listOf(
                        Color(0xFF111111),
                        Color(0xFF222222),
                        Color(0xFF2E2E2E),
                    ).random(rnd),
                    lifetime = rnd.nextLong(600L, 1000L),
                    createdAt = System.currentTimeMillis(),
                    jitter = rnd.nextFloat() * 0.003f
                )
            }
            delay(spawnRateMillis)
        }
    }

    // Update particle positions
    LaunchedEffect(Unit) {
        var lastTime = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            val dt = (now - lastTime) / 1000f
            lastTime = now

            val it = particles.listIterator()
            while (it.hasNext()) {
                val p = it.next()
                val age = now - p.createdAt
                if (age > p.lifetime) {
                    it.remove()
                    continue
                }

                // Slight downward drift
                p.y += 0.0004f * dt

                // Sideways jitter for creepiness
                p.x += (rnd.nextFloat() - 0.5f) * p.jitter
            }

            delay(16L)
        }
    }

    // Draw particles
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val now = System.currentTimeMillis()

        particles.forEach { p ->
            val t = ((now - p.createdAt) / p.lifetime.toFloat()).coerceIn(0f, 1f)
            val alpha = (1f - t).coerceIn(0f, 1f)

            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.radius * (1f - t),
                center = Offset(p.x * w, p.y * h)
            )
        }
    }
}
