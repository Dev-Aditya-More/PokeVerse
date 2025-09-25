package com.aditya1875.pokeverse.specialscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun ElectricParticles(
    modifier: Modifier = Modifier,
    maxSparks: Int = 80,
    spawnRateMillis: Long = 50L
) {
    val sparks = remember { mutableStateListOf<Spark>() }
    val rnd = remember { kotlin.random.Random(System.currentTimeMillis()) }
    val density = LocalDensity.current

    // Spawner: keep a pool of sparks up to maxSparks
    LaunchedEffect(Unit) {
        while (true) {
            if (sparks.size < maxSparks) {
                sparks += Spark(
                    x = rnd.nextFloat(),
                    y = rnd.nextFloat(),
                    vx = (rnd.nextFloat() - 0.5f) * 1.2f, // initial velocity
                    vy = (rnd.nextFloat() - 0.5f) * 1.2f,
                    lifetime = rnd.nextLong(220L, 600L),
                    shape = if (rnd.nextFloat() < 0.3f) SparkShape.ZIGZAG else SparkShape.STREAK,
                    colorIsWhite = rnd.nextFloat() < 0.15f,
                    createdAt = System.currentTimeMillis()
                )
            }
            delay(spawnRateMillis)
        }
    }

    // Physics updater: moves sparks using real dt, applies jitter & damping, removes expired
    LaunchedEffect(Unit) {
        var last = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            val dt = (now - last) / 1000f // seconds
            last = now

            // Iterate safely and update
            val it = sparks.listIterator()
            while (it.hasNext()) {
                val s = it.next()
                val age = now - s.createdAt
                if (age > s.lifetime) {
                    it.remove()
                    continue
                }

                // Add random jitter acceleration (chaotic electricity behavior)
                s.vx += (rnd.nextFloat() - 0.5f) * 6f * dt
                s.vy += (rnd.nextFloat() - 0.5f) * 6f * dt

                // Damping so they don't fly off too violently
                s.vx *= 0.95f
                s.vy *= 0.95f

                // Integrate
                s.x += s.vx * dt
                s.y += s.vy * dt

                // Optional: keep within bounds (bounce a little)
                if (s.x < 0f) { s.x = 0f; s.vx = -s.vx * 0.4f }
                if (s.x > 1f) { s.x = 1f; s.vx = -s.vx * 0.4f }
                if (s.y < 0f) { s.y = 0f; s.vy = -s.vy * 0.4f }
                if (s.y > 1f) { s.y = 1f; s.vy = -s.vy * 0.4f }
            }

            // Target ~60fps updates
            delay(16L)
        }
    }

    // Draw
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val now = System.currentTimeMillis()

        sparks.forEach { s ->
            val age = now - s.createdAt
            val t = (age / s.lifetime.toFloat()).coerceIn(0f, 1f)
            val alpha = (1f - t).coerceIn(0f, 1f)

            val center = Offset(s.x * w, s.y * h)
            val mainColor = if (s.colorIsWhite) Color.White else Color(0xFFFFD700)

            when (s.shape) {
                SparkShape.STREAK -> {
                    // short streak in the direction of velocity
                    val vxPx = s.vx * 60f
                    val vyPx = s.vy * 60f
                    val end = center + Offset(vxPx, vyPx)
                    drawLine(
                        color = mainColor.copy(alpha = alpha),
                        start = center,
                        end = end,
                        strokeWidth = (2.dp.toPx() * (1f - t)).coerceAtLeast(0.8f),
                        cap = StrokeCap.Round
                    )
                    // little glow
                    drawCircle(mainColor.copy(alpha = alpha * 0.12f), radius = 6.dp.toPx() * (1f - t), center = center)
                }

                SparkShape.ZIGZAG -> {
                    // create a small zig-zag path (lightning-like)
                    val len = (8.dp.toPx() + 12.dp.toPx() * (1f - t))
                    val p1 = center + Offset(-len * 0.4f, 0f)
                    val p2 = center + Offset(0f, -len * 0.5f)
                    val p3 = center + Offset(len * 0.35f, len * 0.25f)

                    val path = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                    }

                    drawPath(
                        path = path,
                        color = mainColor.copy(alpha = alpha),
                        style = Stroke(width = (2.4.dp.toPx() * (1f - t)).coerceAtLeast(0.9f), cap = StrokeCap.Round)
                    )

                    // one random tiny branch for realism
                    val branchAngle = (s.vx * 50f).coerceIn(-20f, 20f)
                    val branchEnd = center + Offset(len * 0.35f, -len * 0.35f).rotate(branchAngle)
                    drawLine(
                        color = mainColor.copy(alpha = alpha * 0.6f),
                        start = center,
                        end = branchEnd,
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // faint glow for depth
                    drawCircle(mainColor.copy(alpha = alpha * 0.12f), radius = 5.dp.toPx() * (1f - t), center = center)
                }
            }
        }
    }
}

/** Helpers and model **/
private data class Spark(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var lifetime: Long,
    var shape: SparkShape,
    var colorIsWhite: Boolean,
    var createdAt: Long
)

private enum class SparkShape { STREAK, ZIGZAG }

/** Utility to rotate an Offset by degrees (used above) */
private fun Offset.rotate(degrees: Float): Offset {
    val rad = Math.toRadians(degrees.toDouble())
    val sin = kotlin.math.sin(rad).toFloat()
    val cos = kotlin.math.cos(rad).toFloat()
    return Offset(x * cos - y * sin, x * sin + y * cos)
}
