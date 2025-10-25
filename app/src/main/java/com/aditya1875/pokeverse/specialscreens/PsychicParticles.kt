package com.aditya1875.pokeverse.specialscreens

import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
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

@Preview
@Composable
fun PsychicParticles(
    modifier: Modifier = Modifier,
    maxParticles: Int = 30,                 // fewer particles = sparse, elegant
    spawnRateMillis: Long = 70L            // slower spawn = calmer vibe
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
    val rnd = remember { kotlin.random.Random(System.currentTimeMillis()) }

    // Spawn logic
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < maxParticles) {
                val angleOffset = rnd.nextFloat() * 360f
                particles += Particle(
                    // slightly spread out, not just in the center
                    x = 0.3f + (rnd.nextFloat() - 0.5f) * 0.9f,
                    y = 0.5f + (rnd.nextFloat() - 0.5f) * 0.9f,

                    // bigger base sizes — gentle, wave-like orbs
                    radius = rnd.nextFloat() * 20.dp.toPxCustom(density) + 10.dp.toPxCustom(density),

                    // soft psychic violet
                    color = Color(0xFF9B59B6).copy(alpha = 0.4f),

                    angleOffset = angleOffset,

                    // live a little longer
                    lifetime = rnd.nextLong(2500L, 4000L),
                    createdAt = System.currentTimeMillis(),

                    // each particle gets a unique horizontal drift speed
                    horizontalDrift = (rnd.nextFloat() - 0.5f) * 0.0008f
                )
            }
            delay(spawnRateMillis)
        }
    }

    // Update positions
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

                // Subtle oscillation and floating motion
                val waveX = kotlin.math.sin((age + p.angleOffset) / 800.0) * 0.002f
                val waveY = kotlin.math.cos((age + p.angleOffset) / 600.0) * 0.001f

                p.x += (waveX + p.horizontalDrift).toFloat()
                p.y -= 0.0003f + waveY.toFloat() // slow gentle rise
            }

            delay(16L)
        }
    }

    // Draw
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val now = System.currentTimeMillis()

        particles.forEach { p ->
            val t = ((now - p.createdAt) / p.lifetime.toFloat()).coerceIn(0f, 1f)
            val alpha = (1f - t).coerceIn(0f, 1f)

            // use smooth fade and size shrink for dreamy feel
            drawCircle(
                color = p.color.copy(alpha = alpha * 0.9f),
                radius = p.radius * (1f - 0.3f * t),
                center = Offset(p.x * w, p.y * h)
            )
        }
    }
}

