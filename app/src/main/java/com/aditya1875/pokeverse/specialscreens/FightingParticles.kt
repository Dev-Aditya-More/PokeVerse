package com.aditya1875.pokeverse.specialscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Preview(showBackground = true)
@Composable
fun FightingParticles(
    modifier: Modifier = Modifier,
    maxParticles: Int = 45,
    spawnRateMillis: Long = 40L
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
        val gravity: Float,
        var rotation: Float
    )

    val particles = remember { mutableStateListOf<Particle>() }
    val rnd = remember { kotlin.random.Random(System.currentTimeMillis()) }

    // --- Spawn phase ---
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < maxParticles) {
                val angle = rnd.nextFloat() * (Math.PI.toFloat() * 2f)
                val baseColor = listOf(
                    Color(0xFFFF6D00),
                ).random()

                particles += Particle(
                    x = 0.5f + (rnd.nextFloat() - 0.5f) * 0.3f,
                    y = (rnd.nextFloat() - 0.5f),
                    angle = angle,
                    speed = rnd.nextFloat(),
                    size = rnd.nextFloat() * 8.dp.toPxCustom(density) + 8.dp.toPxCustom(density),
                    color = baseColor.copy(alpha = 0.9f),
                    lifetime = rnd.nextLong(900L, 1500L),
                    createdAt = System.currentTimeMillis(),
                    gravity = 0.7f + rnd.nextFloat() * 0.3f,
                    rotation = rnd.nextFloat() * 360f
                )
            }
            delay(spawnRateMillis)
        }
    }

    // --- Motion phase ---
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

                // swirl + impact movement
                val swirl = sin((now + p.x * 1000) * 0.005f) * 0.0025f
                p.x += (cos(p.angle) + swirl) * p.speed * dt * 2.8f
                p.y += sin(p.angle) * p.speed * dt * 2.8f + p.gravity * dt * 0.18f
                p.rotation += 90f * dt // spin subtly
                p.speed *= 0.985f
            }

            delay(16L)
        }
    }

    // --- Drawing phase ---
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val now = System.currentTimeMillis()

        particles.forEach { p ->
            val age = now - p.createdAt
            val t = (age / p.lifetime.toFloat()).coerceIn(0f, 1f)
            val fade = (1f - t * t).coerceIn(0f, 1f)

            // inner core glow — brighter like impact sparks
            drawCircle(
                color = p.color.copy(alpha = fade * 0.9f),
                radius = p.size * (0.6f - 0.3f * t),
                center = Offset(p.x * w, p.y * h)
            )

            // outer glowing aura — like shockwave ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        p.color.copy(alpha = fade * 0.7f),
                        Color.Transparent
                    ),
                    center = Offset(p.x * w, p.y * h),
                    radius = p.size * (1.8f - t)
                ),
                radius = p.size * (1.6f - 0.5f * t),
                center = Offset(p.x * w, p.y * h)
            )
        }
    }
}


fun Dp.toPxCustom(density: Density): Float = this.value * density.density

