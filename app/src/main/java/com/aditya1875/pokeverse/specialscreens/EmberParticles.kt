package com.aditya1875.pokeverse.specialscreens

import android.graphics.BlurMaskFilter
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview
@Composable
fun EmberParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 60,
    colors: List<Color> = listOf(Color(0xFFFF6D00), Color(0xFFFFA726), Color(0xFFFFC107)),
    flameType: String = "orange" // "blue" for Charizard X flames
) {
    val particles = remember { mutableStateListOf<EmberParticle>() }
    val density = LocalDensity.current.density

    val runtimeShader = remember {
        RuntimeShader(
            """
            uniform float2 resolution;
            uniform float  time;
            uniform float  flameType; // 0 = orange, 1 = blue

            half4 main(float2 fragCoord) {
                float2 uv = fragCoord / resolution.xy;
                uv -= 0.5;
                uv.x *= resolution.x / resolution.y;

                float angle = atan(uv.y, uv.x);
                float radius = length(uv);
                angle += sin(time * 0.8 + radius * 8.0) * 0.5;
                uv = float2(cos(angle), sin(angle)) * radius;

                uv.y *= 1.8;
                uv.x *= 0.6;

                float glow = exp(-8.0 * dot(uv, uv));

                float3 deepOrange = float3(1.0, 0.42, 0.0);
                float3 brightOrange = float3(1.0, 0.65, 0.15);
                float3 golden = float3(1.0, 0.76, 0.03);

                float3 deepBlue = float3(0.05, 0.2, 0.65);
                float3 strongBlue = float3(0.10, 0.46, 0.82);
                float3 cyanGlow = float3(0.0, 0.75, 0.9);

                float3 color;
                if (flameType > 0.5) {
                    // Blue flame mode
                    color = mix(deepBlue, strongBlue, pow(glow, 0.6));
                    color = mix(color, cyanGlow, pow(glow, 2.5));
                } else {
                    // Orange flame mode
                    color = mix(deepOrange, brightOrange, pow(glow, 0.6));
                    color = mix(color, golden, pow(glow, 2.5));
                }

                float flicker = 0.85 + 0.15 * sin(time * 5.0 + uv.y * 12.0);
                color *= flicker;

                float swirl = sin(uv.y * 10.0 + time * 2.0) * 0.02;
                uv.x += swirl;

                return half4(color * glow, glow);
            }
            """
        )
    }

    // 🧩 Generate & animate particles
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < particleCount)
                particles += EmberParticle.generate(colors)
            delay(50L)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val iterator = particles.listIterator()
            while (iterator.hasNext()) {
                val updated = iterator.next().update()
                if (updated.isAlive()) iterator.set(updated)
                else iterator.remove()
            }
            delay(16L)
        }
    }

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            time += 0.02f
            delay(16L)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        runtimeShader.setFloatUniform("resolution", size.width, size.height)
        runtimeShader.setFloatUniform("time", time)
        runtimeShader.setFloatUniform("flameType", if (flameType == "blue") 1f else 0f)
        drawRect(ShaderBrush(runtimeShader))

        particles.forEach { particle ->
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.argb(
                        (particle.alpha * 255).toInt().coerceIn(0, 255),
                        (particle.color.red * 255).toInt(),
                        (particle.color.green * 255).toInt(),
                        (particle.color.blue * 255).toInt()
                    )
                    maskFilter = BlurMaskFilter(
                        (particle.size * (0.8f + 0.4f * particle.alpha)) * density,
                        BlurMaskFilter.Blur.NORMAL
                    )
                }
                canvas.nativeCanvas.drawCircle(
                    particle.x * size.width,
                    particle.y * size.height,
                    particle.size * density,
                    paint
                )
            }
        }
    }
}

data class EmberParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val velocityY: Float,
    val velocityX: Float,
    val alpha: Float,
    val lifetime: Int,
    val color: Color,
    val age: Int = 0
) {
    fun update(): EmberParticle {
        val newAge = age + 1
        val progress = newAge.toFloat() / lifetime

        // Flickering alpha with small randomness (simulates glowing)
        val flicker = 0.85f + Random.nextFloat() * 0.15f
        val newAlpha = (1f - progress) * flicker

        // Gentle sway left-right motion (sinusoidal)
        val sway = sin(newAge * 0.15f) * 0.003f

        return copy(
            x = (x + velocityX + sway).coerceIn(0f, 1f),
            y = y - velocityY,
            alpha = newAlpha,
            age = newAge
        )
    }

    fun isAlive() = age < lifetime && alpha > 0.05f

    companion object {
        fun generate(colors: List<Color>): EmberParticle {
            return EmberParticle(
                x = Random.nextFloat(),
                y = 1f + Random.nextFloat() * 0.05f,
                size = Random.nextFloat() * 5f + 2f,
                velocityY = Random.nextFloat() * 0.007f + 0.004f,
                velocityX = (Random.nextFloat() - 0.5f) * 0.002f, // small sideways drift
                alpha = 1f,
                lifetime = 60 + Random.nextInt(80),
                color = colors.random()
            )
        }
    }
}

