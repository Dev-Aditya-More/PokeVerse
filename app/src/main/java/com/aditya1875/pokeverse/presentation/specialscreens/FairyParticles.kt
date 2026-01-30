package com.aditya1875.pokeverse.presentation.specialscreens

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@RequiresApi(Build.VERSION_CODES.TIRAMISU) // RuntimeShader needs Android 13+
@Preview
@Composable
fun FairyParticles(
    modifier: Modifier = Modifier,
    maxParticles: Int = 45,
    spawnRateMillis: Long = 40L
) {
    // --- Particle data model ---
    data class Particle(
        var x: Float,
        var y: Float,
        val radius: Float,
        val color: Color,
        val angleOffset: Float,
        val lifetime: Long,
        val createdAt: Long
    )

    val particles = remember { mutableStateListOf<Particle>() }
    val rnd = remember { Random(System.currentTimeMillis()) }
    val density = LocalDensity.current

    // --- RuntimeShader for shimmer glow background ---
    val shimmerShader = remember {
        RuntimeShader(
            """
            uniform float2 iResolution;
            uniform float iTime;

            half4 main(float2 fragCoord) {
                float2 uv = fragCoord / iResolution;
                float glow = 0.0;

                // Moving waves of pink/purple light
                glow += 0.4 + 0.4 * sin((uv.x + uv.y) * 10.0 + iTime * 1.2);
                glow += 0.2 * cos((uv.x - uv.y) * 15.0 - iTime * 0.9);

                // soft vignette fade at edges
                float dist = distance(uv, float2(0.5, 0.5));
                glow *= smoothstep(0.9, 0.3, dist);

                // pinkish glow color
                return half4(1.0, 0.7 + 0.3 * sin(iTime * 0.3), 0.9, glow * 0.6);
            }
            """.trimIndent()
        )
    }

    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            time.floatValue += 0.016f
            delay(16L)
        }
    }

    // --- Particle spawning ---
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < maxParticles) {
                val angleOffset = rnd.nextFloat() * 360f
                particles += Particle(
                    x = 0.5f + (rnd.nextFloat() - 0.5f) * 1.0f,
                    y = 0.5f + (rnd.nextFloat() - 0.5f) * 1.0f,
                    radius = (rnd.nextFloat() * 6.dp.toPx(density) + 3.dp.toPx(density)),
                    color = listOf(
                        Color(0xFFFFC0CB),
                        Color(0xFFE1BEE7),
                        Color(0xFFF8BBD0)
                    ).random(rnd),
                    angleOffset = angleOffset,
                    lifetime = rnd.nextLong(1500L, 2500L),
                    createdAt = System.currentTimeMillis()
                )
            }
            delay(spawnRateMillis)
        }
    }

    // --- Physics updater ---
    LaunchedEffect(Unit) {
        var lastTime = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            lastTime = now

            val it = particles.listIterator()
            while (it.hasNext()) {
                val p = it.next()
                val age = now - p.createdAt
                if (age > p.lifetime) {
                    it.remove()
                    continue
                }

                // Smooth floating upwards
                p.y -= 0.0005f

                // Side sway using sine
                val wave = sin((age + p.angleOffset) / 250.0) * 0.001f
                p.x += wave.toFloat()
            }
            delay(16L)
        }
    }

    // --- Draw ---
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        shimmerShader.setFloatUniform("iResolution", w, h)
        shimmerShader.setFloatUniform("iTime", time.floatValue)

        // Draw shimmer background softly
        drawRect(
            brush = ShaderBrush(shimmerShader),
            size = size,
            alpha = 0.6f
        )

        val now = System.currentTimeMillis()
        particles.forEach { p ->
            val t = ((now - p.createdAt) / p.lifetime.toFloat()).coerceIn(0f, 1f)
            val alpha = (1f - t).coerceIn(0f, 1f)

            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.radius * (1f - t * 0.4f),
                center = Offset(p.x * w, p.y * h),
                blendMode = BlendMode.Plus // additive blending for glow
            )
        }
    }
}

// Helper extension
fun Dp.toPx(density: Density) = with(density) { this@toPx.toPx() }