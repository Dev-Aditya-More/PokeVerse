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
import kotlin.math.sin
import kotlin.random.Random

@Preview
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HydroPumpParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 35,
    spawnRateMs: Long = 110L,
    colors: List<Color> = listOf(
        Color(0xFF64B5F6),
        Color(0xFF42A5F5),
        Color(0xFF90CAF9),
        Color(0xFF81D4FA)
    )
) {
    val particles = remember { mutableStateListOf<BubbleParticle>() }
    val density = LocalDensity.current.density

    // Subtle background water flow shader
    val hydroShader = remember {
        RuntimeShader(
            """
            uniform float2 iResolution;
            uniform float iTime;

            half4 main(float2 fragCoord) {
                float2 uv = fragCoord / iResolution;
                float t = iTime * 0.25;

                // Very gentle flowing waves
                float wave1 = sin(uv.y * 6.0 + t) * 0.02;
                float wave2 = cos(uv.y * 4.0 - t * 0.6) * 0.015;
                float wave = wave1 + wave2;
                
                // Soft vertical gradient
                float dist = abs(uv.x - 0.5 + wave);
                float intensity = smoothstep(0.7, 0.0, dist);

                // Subtle shimmer
                float shimmer = 0.4 + 0.3 * sin(t * 1.5 + uv.y * 12.0);
                
                // Very soft blue tones
                float blue = 0.5 + 0.15 * sin(t + uv.y * 3.0);
                float cyan = 0.4 + 0.2 * cos(t * 0.4 + uv.y * 5.0);

                // TWEAK THIS: Change 0.08 to 0.05-0.15 (lower = more subtle, higher = more visible)
                return half4(cyan * 0.3, blue * 0.6, 0.9, intensity * shimmer * 0.08);
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

    // Spawn particles
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < particleCount) {
                particles += BubbleParticle.generate(colors)
            }
            delay(spawnRateMs)
        }
    }

    // Update particles
    LaunchedEffect(Unit) {
        while (true) {
            val iterator = particles.listIterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                val age = p.age + 1
                val progress = age.toFloat() / p.lifetime

                // Gentle S-curve rise with subtle sway
                val sway = sin(progress * PI * 2.5f + p.driftX * 80f) * 0.03f
                val newX = p.x + sway.toFloat()
                val newY = p.y - (p.velocityY * 1.0f)

                // Smooth fade - stays transparent
                val fadeIn = (progress * 10f).coerceIn(0f, 1f)
                val fadeOut = 1f - progress
                val newAlpha = (fadeIn * fadeOut).coerceAtMost(0.65f) // TWEAK: 0.5-0.8 (lower = more subtle)

                // Gentle size pulsing
                val pulse = 1f + sin(progress * PI * 2f) * 0.12f

                val newParticle = p.copy(
                    x = newX,
                    y = newY,
                    alpha = newAlpha,
                    age = age,
                    radius = (p.radius * pulse).toFloat()
                )

                if (newParticle.isAlive()) iterator.set(newParticle)
                else iterator.remove()
            }
            delay(16L)
        }
    }

    Canvas(modifier = modifier.fillMaxSize().clipToBounds()) {
        val w = size.width
        val h = size.height

        hydroShader.setFloatUniform("iResolution", w, h)
        hydroShader.setFloatUniform("iTime", time.floatValue)

        drawRect(
            brush = ShaderBrush(hydroShader),
            size = size,
            alpha = 0.25f,
            blendMode = BlendMode.Plus
        )

        particles.forEach { particle ->
            val center = Offset(particle.x * w, particle.y * h)
            val radiusPx = particle.radius * density
            val alpha = particle.alpha

            // Large soft outer glow - very transparent
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        particle.color.copy(alpha = alpha * 0.08f), // TWEAK: 0.05-0.12
                        Color.Transparent
                    ),
                    center = center,
                    radius = radiusPx * 2.5f
                ),
                radius = radiusPx * 2.5f,
                center = center,
                blendMode = BlendMode.Plus
            )

            // Medium glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        particle.color.copy(alpha = alpha * 0.2f), // TWEAK: 0.15-0.3
                        Color.Transparent
                    ),
                    center = center,
                    radius = radiusPx * 1.5f
                ),
                radius = radiusPx * 1.5f,
                center = center,
                blendMode = BlendMode.Plus
            )

            // Bubble core with gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha * 0.15f), // TWEAK: 0.1-0.25
                        particle.color.copy(alpha = alpha * 0.35f), // TWEAK: 0.25-0.45
                        particle.color.copy(alpha = alpha * 0.2f)
                    ),
                    center = center,
                    radius = radiusPx
                ),
                radius = radiusPx,
                center = center,
                blendMode = BlendMode.Plus
            )

            // Subtle bubble outline
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.2f), // TWEAK: 0.15-0.3
                radius = radiusPx,
                center = center,
                style = Stroke(width = 1f),
                blendMode = BlendMode.Plus
            )

            // Highlight on top
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.25f), // TWEAK: 0.2-0.35
                radius = radiusPx * 0.3f,
                center = Offset(
                    center.x - radiusPx * 0.25f,
                    center.y - radiusPx * 0.25f
                ),
                blendMode = BlendMode.Plus
            )
        }
    }
}

data class BubbleParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val velocityY: Float,
    val driftX: Float,
    val alpha: Float,
    val lifetime: Int,
    val color: Color,
    val age: Int = 0
) {
    fun isAlive(): Boolean = age < lifetime

    companion object {
        fun generate(colors: List<Color>): BubbleParticle {
            return BubbleParticle(

                x = 0.35f + Random.nextFloat() * 0.3f,

                y = 0.6f + Random.nextFloat() * 0.4f,

                radius = Random.nextFloat() * 4f + 3f,

                velocityY = Random.nextFloat() * 0.0015f + 0.001f,

                driftX = Random.nextFloat() * 0.001f - 0.0005f,
                alpha = 0.65f,

                lifetime = 120 + Random.nextInt(60),

                color = colors.random()
            )
        }
    }
}