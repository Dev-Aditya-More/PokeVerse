package com.aditya1875.pokeverse.specialscreens

import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
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
fun PsychicParticles(
    modifier: Modifier = Modifier,
    maxParticles: Int = 40,
    spawnRateMillis: Long = 80L
) {
    data class Particle(
        var x: Float,
        var y: Float,
        val radius: Float,
        val color: Color,
        val angleOffset: Float,
        val lifetime: Long,
        val createdAt: Long,
        val horizontalDrift: Float
    )

    val density = LocalDensity.current
    val particles = remember { mutableStateListOf<Particle>() }
    val rnd = remember { Random(System.currentTimeMillis()) }

    // Spawn particles gently across the screen
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < maxParticles) {
                val angleOffset = rnd.nextFloat() * 360f
                particles += Particle(
                    x = 0.5f + (rnd.nextFloat() - 0.5f) * 0.6f, // slightly centered
                    y = 0.5f + (rnd.nextFloat() - 0.5f) * 0.8f,

                    // smaller and subtler radii
                    radius = rnd.nextFloat() * 8.dp.toPxCustom(density) + 4.dp.toPxCustom(density),

                    // mix in soft violet to pink tones
                    color = listOf(
                        Color(0xFF9B59B6).copy(alpha = 0.25f),
                        Color(0xFFBB86FC).copy(alpha = 0.2f),
                        Color(0xFFCE93D8).copy(alpha = 0.18f)
                    ).random(),

                    angleOffset = angleOffset,
                    lifetime = rnd.nextLong(3000L, 5000L),
                    createdAt = System.currentTimeMillis(),
                    horizontalDrift = (rnd.nextFloat() - 0.5f) * 0.0004f // calmer drift
                )
            }
            delay(spawnRateMillis)
        }
    }

    // Animate the particles’ movement
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

                // Gentle oscillation and upward drift
                val waveX = sin((age + p.angleOffset) / 1000.0) * 0.0012f
                val waveY = cos((age + p.angleOffset) / 900.0) * 0.001f

                p.x += (waveX + p.horizontalDrift).toFloat()
                p.y -= (0.00025f + waveY).toFloat()
            }

            delay(16L)
        }
    }

    // Draw them softly and ethereally
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val now = System.currentTimeMillis()

        particles.forEach { p ->
            val t = ((now - p.createdAt) / p.lifetime.toFloat()).coerceIn(0f, 1f)
            val alpha = (1f - t).coerceIn(0f, 1f)

            drawCircle(
                color = p.color.copy(alpha = alpha * 0.8f),
                radius = p.radius * (1f - 0.2f * t), // shrink slowly, subtle fade
                center = Offset(p.x * w, p.y * h),
                blendMode = BlendMode.Plus // additive glow feel
            )
        }
    }
}

