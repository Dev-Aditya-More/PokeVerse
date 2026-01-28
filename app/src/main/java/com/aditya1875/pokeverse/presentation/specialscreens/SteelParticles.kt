package com.aditya1875.pokeverse.presentation.specialscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Preview
@Composable
fun SteelParticles(
    modifier: Modifier = Modifier,
    maxParticles: Int = 45,
    spawnRateMillis: Long = 70L
) {
    data class MetalFlake(
        var x: Float,
        var y: Float,
        val width: Float,
        val height: Float,
        val rotationSpeed: Float,
        val color: Color,
        val lifetime: Long,
        val createdAt: Long,
        val shimmerPhase: Float
    )

    val flakes = remember { mutableStateListOf<MetalFlake>() }
    val rnd = remember { Random(System.currentTimeMillis()) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        while (true) {
            if (flakes.size < maxParticles) {
                flakes += MetalFlake(
                    x = 0.3f + (rnd.nextFloat() - 0.5f) * 0.9f,
                    y = 0.5f + (rnd.nextFloat() - 0.5f) * 0.9f,
                    width = rnd.nextFloat() * 8.dp.toPxCustom(density)
                            + 6.dp.toPxCustom(density),

                    height = rnd.nextFloat() * 5.dp.toPxCustom(density)
                            + 3.dp.toPxCustom(density),
                    rotationSpeed = (rnd.nextFloat() - 0.5f) * 120f,
                    color = listOf(
                        Color(0xFFB0B0B0), // steel gray
                        Color(0xFFC0C0C0), // silver
                        Color(0xFF708090), // slate gray
                        Color(0xFFD4AF37)  // gold glint
                    ).random(rnd),
                    lifetime = rnd.nextLong(800L, 1200L),
                    createdAt = System.currentTimeMillis(),
                    shimmerPhase = rnd.nextFloat() * 2 * Math.PI.toFloat()
                )
            }
            delay(spawnRateMillis)
        }
    }

    // Update motion
    LaunchedEffect(Unit) {
        var lastTime = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            val dt = (now - lastTime) / 1000f
            lastTime = now

            val it = flakes.listIterator()
            while (it.hasNext()) {
                val f = it.next()
                val age = now - f.createdAt
                if (age > f.lifetime) {
                    it.remove()
                    continue
                }

                // Slow downward drift + slight horizontal jitter
                f.y += 0.02f * dt
                f.x += (rnd.nextFloat() - 0.5f) * 0.002f
            }

            delay(16L)
        }
    }

    // Draw
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val now = System.currentTimeMillis()

        flakes.forEach { f ->
            val t = ((now - f.createdAt) / f.lifetime.toFloat()).coerceIn(0f, 1f)
            val alpha = (1f - t).coerceIn(0f, 1f)
            val shimmer = 0.5f + 0.5f * sin((t * 8f * Math.PI + f.shimmerPhase)).toFloat()
            val finalColor = f.color.copy(alpha = alpha * (0.6f + 0.4f * shimmer))

            val rotation = t * f.rotationSpeed

            withTransform({
                translate(left = f.x * w, top = f.y * h)
                rotate(rotation)
            }) {
                drawRect(
                    color = finalColor,
                    size = Size(f.width, f.height),
                    topLeft = Offset(-f.width / 2, -f.height / 2)
                )
            }
        }
    }
}
