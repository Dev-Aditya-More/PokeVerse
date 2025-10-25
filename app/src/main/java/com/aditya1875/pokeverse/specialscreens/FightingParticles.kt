package com.aditya1875.pokeverse.specialscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Preview
@Composable
fun FightingParticles(
    modifier: Modifier = Modifier,
    maxParticles: Int = 45,            // fewer for clarity, looks more impactful
    spawnRateMillis: Long = 70L
) {
    val density = LocalDensity.current

    data class Particle(
        var x: Float,
        var y: Float,
        val angle: Float,
        var speed: Float,
        val size: Float,
        val color: Color,
        val lifetime: Long,
        val createdAt: Long,
        val gravity: Float
    )

    val particles = remember { mutableStateListOf<Particle>() }
    val rnd = remember { kotlin.random.Random(System.currentTimeMillis()) }

    // --- Spawner ---
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < maxParticles) {
                val angle = rnd.nextFloat() * (Math.PI.toFloat() * 2f)
                val baseColor = listOf(
                    Color(0xFFB87333), // bronze-like tone
                    Color(0xFFD2691E), // brownish-orange
                    Color(0xFFC68642)  // dusty tan
                ).random()

                particles += Particle(
                    x = 0.5f + (rnd.nextFloat() - 0.5f) * 0.3f, // around center
                    y = 0.6f + (rnd.nextFloat() - 0.5f) * 0.2f,
                    angle = angle,
                    speed = rnd.nextFloat() * 0.8f + 0.3f,       // moderate velocity
                    size = rnd.nextFloat() * 10.dp.toPxCustom(density) + 8.dp.toPxCustom(density),
                    color = baseColor.copy(alpha = 0.9f),
                    lifetime = rnd.nextLong(900L, 1400L),
                    createdAt = System.currentTimeMillis(),
                    gravity = 0.8f + rnd.nextFloat() * 0.4f      // slightly varying gravity
                )
            }
            delay(spawnRateMillis)
        }
    }

    // --- Motion & decay ---
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

                // Radial expansion — like impact debris
                p.x += kotlin.math.cos(p.angle) * p.speed * dt * 2.5f
                p.y += kotlin.math.sin(p.angle) * p.speed * dt * 2.5f

                // Gravity pulling down — makes them “fall”
                p.y += p.gravity * dt * 0.15f

                // Slight friction to slow over time
                p.speed *= 0.985f
            }

            delay(16L)
        }
    }

    // --- Draw phase ---
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val now = System.currentTimeMillis()

        particles.forEach { p ->
            val age = now - p.createdAt
            val t = (age / p.lifetime.toFloat()).coerceIn(0f, 1f)
            val fade = (1f - t * t).coerceIn(0f, 1f) // slower fade

            // Dusty blurred glow effect
            drawCircle(
                color = p.color.copy(alpha = fade * 0.8f),
                radius = p.size * (1.2f - 0.6f * t),
                center = Offset(p.x * w, p.y * h)
            )

            // inner solid core
            drawCircle(
                color = p.color.copy(alpha = fade),
                radius = p.size * (0.5f - 0.3f * t),
                center = Offset(p.x * w, p.y * h)
            )
        }
    }
}

fun Dp.toPxCustom(density: Density): Float = this.value * density.density

