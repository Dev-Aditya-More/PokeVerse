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
import kotlin.math.sin
import kotlin.random.Random

@Preview
@Composable
fun DragonParticles(
    modifier: Modifier = Modifier,
    maxParticles: Int = 35,
    spawnRateMillis: Long = 100L
) {
    data class Particle(
        var x: Float,
        var y: Float,
        val radius: Float,
        val color: Color,
        val angleOffset: Float,
        val curlSpeed: Float,
        val lifetime: Long,
        val createdAt: Long
    )

    val density = LocalDensity.current
    val particles = remember { mutableStateListOf<Particle>() }
    val rnd = remember { Random(System.currentTimeMillis()) }

    // Particle spawner
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < maxParticles) {
                particles += Particle(
                    x = 0.3f + (rnd.nextFloat() - 0.5f) * 0.9f,
                    y = 0.5f + (rnd.nextFloat() - 0.5f) * 0.9f,

                    // bigger base sizes — gentle, wave-like orbs
                    radius = rnd.nextFloat() * 20.dp.toPxCustom(density) + 10.dp.toPxCustom(density),
                    color = listOf(
                        Color(0xFFB22222), // firebrick red
                        Color(0xFFFFD700), // gold
                        Color(0xFF8A2BE2)  // blueviolet
                    ).random(rnd),
                    angleOffset = rnd.nextFloat() * 360f,
                    curlSpeed = rnd.nextFloat() * 0.03f + 0.01f,
                    lifetime = rnd.nextLong(800L, 1200L),
                    createdAt = System.currentTimeMillis()
                )
            }
            delay(spawnRateMillis)
        }
    }

    // Physics updater: curling + floating
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

                // Curling motion
                val curl = sin((age + p.angleOffset) / 300.0) * p.curlSpeed
                p.x += curl.toFloat()
                p.y -= 0.0006f // slow upward drift
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
