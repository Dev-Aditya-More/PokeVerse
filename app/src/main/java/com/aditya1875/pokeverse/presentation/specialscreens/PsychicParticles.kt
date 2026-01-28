package com.aditya1875.pokeverse.presentation.specialscreens

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
    maxParticles: Int = 45,
    spawnRateMillis: Long = 90L
) {
    data class Particle(
        var x: Float,
        var y: Float,
        val radius: Float,
        val color: Color,
        val angleOffset: Float,
        val lifetime: Long,
        val createdAt: Long,
        val horizontalDrift: Float,
        val orbitalSpeed: Float,
        val orbitalRadius: Float
    )

    val density = LocalDensity.current
    val particles = remember { mutableStateListOf<Particle>() }
    val rnd = remember { Random(System.currentTimeMillis()) }

    // Spawn particles around center where Pokemon is
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < maxParticles) {
                val angleOffset = rnd.nextFloat() * 360f
                particles += Particle(
                    // Spawn around center in circular pattern
                    x = 0.5f + (rnd.nextFloat() - 0.5f) * 0.5f,
                    y = 0.5f + (rnd.nextFloat() - 0.5f) * 0.5f,

                    // Varied sizes for depth
                    radius = rnd.nextFloat() * 10.dp.toPxCustom(density) + 5.dp.toPxCustom(density),

                    // Ethereal psychic colors with varied opacity
                    color = listOf(
                        Color(0xFF9B59B6), // amethyst
                        Color(0xFFBB86FC), // light purple
                        Color(0xFFCE93D8), // lavender
                        Color(0xFFBA68C8), // medium purple
                        Color(0xFFAB47BC), // deep purple
                        Color(0xFFE1BEE7)  // pale lavender
                    ).random(),

                    angleOffset = angleOffset,
                    lifetime = rnd.nextLong(2500L, 4000L),
                    createdAt = System.currentTimeMillis(),
                    horizontalDrift = (rnd.nextFloat() - 0.5f) * 0.0003f,
                    orbitalSpeed = (rnd.nextFloat() - 0.5f) * 0.002f,
                    orbitalRadius = rnd.nextFloat() * 0.08f + 0.02f
                )
            }
            delay(spawnRateMillis)
        }
    }

    // Animate with ethereal orbital motion
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

                // Orbital motion around spawn point - creates swirling psychic energy
                val orbitalAngle = age * p.orbitalSpeed + p.angleOffset
                val orbitalX = cos(orbitalAngle) * p.orbitalRadius
                val orbitalY = sin(orbitalAngle) * p.orbitalRadius

                // Gentle wave motion
                val waveX = sin((age + p.angleOffset) / 1200.0) * 0.001f
                val waveY = cos((age + p.angleOffset) / 1000.0) * 0.0008f

                p.x += (orbitalX + waveX + p.horizontalDrift).toFloat()
                p.y += (orbitalY + waveY - 0.0002f).toFloat() // slight upward drift
            }

            delay(16L)
        }
    }

    // Draw with layered ethereal glow
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val now = System.currentTimeMillis()

        particles.forEach { p ->
            val t = ((now - p.createdAt) / p.lifetime.toFloat()).coerceIn(0f, 1f)

            // Fade in smoothly, fade out gradually
            val fadeIn = (t * 6f).coerceIn(0f, 1f)
            val fadeOut = (1f - t)
            val alpha = (fadeIn * fadeOut).coerceIn(0f, 1f)

            // Pulsing effect for psychic energy
            val pulse = (sin(now * 0.002 + p.angleOffset) * 0.5 + 0.5).toFloat()
            val radiusMultiplier = 1f + pulse * 0.2f

            val centerPos = Offset(p.x * w, p.y * h)
            val currentRadius = p.radius * (0.9f + t * 0.1f) * radiusMultiplier

            // Large outer aura - very soft
            drawCircle(
                color = p.color.copy(alpha = alpha * 0.08f),
                radius = currentRadius * 3.5f,
                center = centerPos,
                blendMode = BlendMode.Plus
            )

            // Medium aura
            drawCircle(
                color = p.color.copy(alpha = alpha * 0.15f),
                radius = currentRadius * 2.2f,
                center = centerPos,
                blendMode = BlendMode.Plus
            )

            // Inner glow
            drawCircle(
                color = p.color.copy(alpha = alpha * 0.35f),
                radius = currentRadius * 1.3f,
                center = centerPos,
                blendMode = BlendMode.Plus
            )

            // Core particle
            drawCircle(
                color = p.color.copy(alpha = alpha * 0.6f),
                radius = currentRadius,
                center = centerPos,
                blendMode = BlendMode.Plus
            )

            // Bright psychic center
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.3f),
                radius = currentRadius * 0.4f,
                center = centerPos,
                blendMode = BlendMode.Plus
            )
        }
    }
}

