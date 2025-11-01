package com.aditya1875.pokeverse.specialscreens

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Preview
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HydroPumpParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 80,
    colors: List<Color> = listOf(
        Color(0xFFB3E5FC),
        Color(0xFF81D4FA),
        Color(0xFF4FC3F7)
    )
) {
    val particles = remember { mutableStateListOf<BubbleParticle>() }
    val density = LocalDensity.current.density

    // --- Shader for shimmer and depth glow ---
    val hydroShader = remember {
        RuntimeShader(
            """
            uniform float2 iResolution;
            uniform float iTime;

            half4 main(float2 fragCoord) {
                float2 uv = fragCoord / iResolution;
                float t = iTime * 2.0;

                // swirl waves
                float wave = sin(uv.y * 25.0 + t) * 0.05;
                float intensity = smoothstep(0.5, 0.2, abs(uv.x - 0.5 + wave));

                // shimmer color variation
                float blue = 0.7 + 0.3 * sin(t + uv.y * 8.0);
                float cyan = 0.7 + 0.3 * cos(t * 0.5 + uv.y * 10.0);

                return half4(0.3 * cyan, 0.7 * blue, 1.0, intensity * 0.7);
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

    // --- Spawn particles ---
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < particleCount) {
                particles += BubbleParticle.generate(colors)
            }
            delay(40L)
        }
    }

    // --- Update particles (twisting + rising) ---
    LaunchedEffect(Unit) {
        while (true) {
            val iterator = particles.listIterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                val age = p.age + 1
                val progress = age.toFloat() / p.lifetime

                // spiral movement
                val angle = (progress * 8f * PI).toFloat()
                val spiralRadius = 0.05f + 0.05f * sin(progress * PI)
                val newX = 0.5f + spiralRadius * sin(angle) + p.driftX
                val newY = p.y - (p.velocityY * 1.5f)

                val newAlpha = 1f - progress
                val newParticle = p.copy(
                    x = newX.toFloat(),
                    y = newY,
                    alpha = newAlpha,
                    age = age
                )

                if (newParticle.isAlive()) iterator.set(newParticle)
                else iterator.remove()
            }
            delay(16L)
        }
    }

    // --- Draw ---
    Canvas(modifier = modifier.fillMaxSize().clipToBounds()) {
        val w = size.width
        val h = size.height

        hydroShader.setFloatUniform("iResolution", w, h)
        hydroShader.setFloatUniform("iTime", time.floatValue)

        // glowing water beam background
        drawRect(
            brush = ShaderBrush(hydroShader),
            size = size,
            alpha = 0.8f
        )

        particles.forEach { particle ->
            val center = Offset(particle.x * w, particle.y * h)
            val radiusPx = particle.radius * density

            // glow core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        particle.color.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.05f)
                    ),
                    center = center,
                    radius = radiusPx
                ),
                radius = radiusPx,
                center = center,
                blendMode = BlendMode.Plus
            )

            // bubble outline
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = radiusPx,
                center = center,
                style = Stroke(width = 0.8f * density),
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
    fun update(): BubbleParticle {
        val newAge = age + 1
        val progress = newAge.toFloat() / lifetime
        val newAlpha = 1f - progress
        val newY = y - velocityY
        val newX = x + sin(age / 10f) * driftX // drift left-right

        return this.copy(
            x = newX,
            y = newY,
            alpha = newAlpha,
            age = newAge
        )
    }

    fun isAlive(): Boolean = age < lifetime

    companion object {
        fun generate(colors: List<Color>): BubbleParticle {
            return BubbleParticle(
                x = Random.nextFloat(),
                y = 1f + Random.nextFloat() * 0.1f,
                radius = Random.nextFloat() * 6f + 3f,
                velocityY = Random.nextFloat() * 0.005f + 0.002f,
                driftX = Random.nextFloat() * 0.002f - 0.001f,
                alpha = 1f,
                lifetime = 90 + Random.nextInt(60),
                color = colors.random()
            )
        }
    }
}