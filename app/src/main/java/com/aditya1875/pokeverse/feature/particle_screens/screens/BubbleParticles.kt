package com.aditya1875.pokeverse.feature.particle_screens.screens

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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Preview(showBackground = true)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HydroPumpParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 44,
    spawnRateMs: Long = 70L,
    colors: List<Color> = listOf(
        Color(0xFF244B6C),
        Color(0xFF64B5F6),
        Color(0xFFB3E5FC),
        Color(0xFF81D4FA),
        Color(0xFFE1F5FE)
    )
) {
    val particles = remember { mutableStateListOf<BubbleParticle>() }
    val density = LocalDensity.current.density

    val hydroShader = remember {
        RuntimeShader(
            """
            uniform float2 iResolution;
            uniform float iTime;

            half4 main(float2 fragCoord) {
                float2 uv = fragCoord / iResolution;
                float t = iTime * 0.15;

                float wave = sin(uv.x * 3.0 + t) * 0.012
                           + cos(uv.y * 2.5 - t * 0.5) * 0.008;

                float dist = length(uv - float2(0.5 + wave, 0.5));
                float vignette = 1.0 - smoothstep(0.3, 0.9, dist);

                float shimmer = 0.5 + 0.5 * sin(t * 0.8 + uv.y * 8.0 + uv.x * 4.0);

                return half4(0.1, 0.3, 0.55, vignette * shimmer * 0.04);
            }
            """.trimIndent()
        )
    }

    val time = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            time.floatValue += 0.016f
            delay(16)
        }
    }

    // Spawn particles
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < particleCount) {
                particles += BubbleParticle.generate(colors)
            }
            delay(spawnRateMs)
        }
    }

    // Particle physics
    LaunchedEffect(Unit) {
        while (true) {

            val iterator = particles.listIterator()

            while (iterator.hasNext()) {

                val p = iterator.next()
                val age = p.age + 1
                val progress = age.toFloat() / p.lifetime

                // Spiral wobble
                val swirl =
                    sin(progress * PI * 2.4 + p.phaseOffset) * 0.03 +
                            cos(progress * PI * 1.6 + p.phaseOffset * 1.3) * 0.015

                val sway = swirl.toFloat()

                val newX = p.x + sway * p.driftSign

                val speed = p.velocityY * (1f + progress * 0.4f)

                // vertical bounce
                val bounce =
                    sin(progress * PI * 6 + p.phaseOffset) * 0.003f

                val newY = p.y - speed + bounce

                val fadeIn = (progress / 0.15f).coerceIn(0f, 1f)
                val fadeOut = ((1f - progress) / 0.25f).coerceIn(0f, 1f)

                val newAlpha = (fadeIn * fadeOut * p.peakAlpha).coerceIn(0f, 1f)

                val newParticle = p.copy(
                    x = newX,
                    y = newY.toFloat(),
                    alpha = newAlpha,
                    age = age
                )

                if (newParticle.isAlive()) iterator.set(newParticle)
                else iterator.remove()
            }

            delay(16)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {

        val w = size.width
        val h = size.height

        hydroShader.setFloatUniform("iResolution", w, h)
        hydroShader.setFloatUniform("iTime", time.floatValue)

        drawRect(
            brush = ShaderBrush(hydroShader),
            size = size,
            alpha = 1f,
            blendMode = BlendMode.Screen
        )

        particles.forEach { p ->

            val center = Offset(p.x * w, p.y * h)

            // size pulsing
            val pulse =
                1f + (sin(p.age * 0.15 + p.phaseOffset) * 0.12f).toFloat()

            val r = p.baseRadius * density * pulse
            val a = p.alpha

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        p.color.copy(alpha = a * 0.06f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = r * 3.2f
                ),
                radius = r * 3.2f,
                center = center,
                blendMode = BlendMode.Screen
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        p.color.copy(alpha = a * 0.18f),
                        p.color.copy(alpha = a * 0.06f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = r * 1.8f
                ),
                radius = r * 1.8f,
                center = center,
                blendMode = BlendMode.Screen
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        p.color.copy(alpha = a * 0.22f),
                        Color.White.copy(alpha = a * 0.08f)
                    ),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center,
                blendMode = BlendMode.Screen
            )

            drawCircle(
                color = Color.White.copy(alpha = a * 0.15f),
                radius = r,
                center = center,
                style = Stroke(width = 0.8f),
                blendMode = BlendMode.Screen
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = a * 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(
                        center.x - r * 0.28f,
                        center.y - r * 0.28f
                    ),
                    radius = r * 0.22f
                ),
                radius = r * 0.22f,
                center = Offset(
                    center.x - r * 0.28f,
                    center.y - r * 0.28f
                ),
                blendMode = BlendMode.Screen
            )

            drawCircle(
                color = Color.White.copy(alpha = a * 0.12f),
                radius = r * 0.1f,
                center = Offset(
                    center.x + r * 0.3f,
                    center.y + r * 0.3f
                ),
                blendMode = BlendMode.Screen
            )
        }
    }
}

data class BubbleParticle(
    val x: Float,
    val y: Float,
    val baseRadius: Float,     // Immutable — prevents compounding mutation bug
    val velocityY: Float,
    val driftSign: Float,      // +1 or -1 — determines sway direction
    val phaseOffset: Double,   // Unique wave phase per particle
    val alpha: Float,
    val peakAlpha: Float,      // Max opacity ceiling, varies per particle
    val lifetime: Int,
    val color: Color,
    val age: Int = 0
) {
    fun isAlive(): Boolean = age < lifetime

    companion object {

        fun generate(colors: List<Color>): BubbleParticle {

            val r = Random.nextFloat()

            val isLarge = r < 0.20f
            val isMicro = r > 0.80f

            val radius = when {
                isLarge -> Random.nextFloat() * 5f + 8f
                isMicro -> Random.nextFloat() * 1.2f + 0.8f
                else -> Random.nextFloat() * 4f + 2.5f
            }

            val peak =
                if (isLarge)
                    Random.nextFloat() * 0.12f + 0.08f
                else
                    Random.nextFloat() * 0.20f + 0.20f

            return BubbleParticle(
                x = Random.nextFloat() * 0.90f + 0.05f,
                y = 0.75f + Random.nextFloat() * 0.30f,
                baseRadius = radius,
                velocityY = Random.nextFloat() * 0.0010f + 0.0008f,
                driftSign = if (Random.nextBoolean()) 1f else -1f,
                phaseOffset = Random.nextDouble() * PI * 2.0,
                alpha = 0f,
                peakAlpha = peak,
                lifetime = 220 + Random.nextInt(120),
                color = colors.random()
            )
        }
    }
}