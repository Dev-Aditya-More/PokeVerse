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
fun FairyParticles(
    modifier: Modifier = Modifier,
    maxParticles: Int = 50,
    spawnRateMillis: Long = 60L
) {
    data class Particle(
        var x: Float,
        var y: Float,
        val radius: Float,
        val color: Color,
        val angleOffset: Float, // for oscillation
        val lifetime: Long,
        val createdAt: Long
    )

    val particles = remember { mutableStateListOf<Particle>() }
    val rnd = remember { kotlin.random.Random(System.currentTimeMillis()) }
    val density = LocalDensity.current

    // Particle spawner
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < maxParticles) {
                val angleOffset = rnd.nextFloat() * 360f
                particles += Particle(
                    x = 0.3f + (rnd.nextFloat() - 0.5f) * 0.9f,
                    y = 0.5f + (rnd.nextFloat() - 0.5f) * 0.9f,
                    // bigger base sizes — gentle, wave-like orbs
                    radius = rnd.nextFloat() * 20.dp.toPxCustom(density) + 10.dp.toPxCustom(density),
                    color = listOf(
                        Color(0xFFFFC0CB), // pink
                        Color(0xFFDA70D6), // orchid
                        Color(0xFFFFFFB0)  // pale yellow
                    ).random(rnd),
                    angleOffset = angleOffset,
                    lifetime = rnd.nextLong(600L, 1000L),
                    createdAt = System.currentTimeMillis()
                )
            }
            delay(spawnRateMillis)
        }
    }

    // Physics updater: gentle floating and oscillation
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

                // Gentle floating upwards
                p.y -= 0.0008f

                // Side-to-side oscillation
                val wave = kotlin.math.sin((age + p.angleOffset) / 250.0) * 0.0015f
                p.x += wave.toFloat()
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
