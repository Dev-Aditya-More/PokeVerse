package com.aditya1875.pokeverse.specialscreens

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

@Preview
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun GhostParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 25,
    baseColor: Color = Color(0xFFAAAADD).copy(alpha = 0.3f)
) {
    val density = LocalDensity.current
    val baseSizePx = with(density) { 40.dp.toPx() }

    data class GhostParticle(
        val x: Float,
        val startTime: Float,
        val swayPhase: Float,
        val alphaPhase: Float,
        val sizeFactor: Float,
        val verticalSpeed: Float,
        val rotationSpeed: Float
    )

    val particles = remember {
        List(particleCount) {
            GhostParticle(
                x = Random.nextFloat(),
                startTime = Random.nextFloat(),
                swayPhase = Random.nextFloat() * 2 * Math.PI.toFloat(),
                alphaPhase = Random.nextFloat() * 2 * Math.PI.toFloat(),
                sizeFactor = 0.7f + Random.nextFloat() * 0.6f,
                verticalSpeed = 0.03f + Random.nextFloat() * 0.02f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 20f // degrees per second
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val globalTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing)
        )
    )
    val ghostShader = remember {
        RuntimeShader(
            """
            uniform float2 iResolution;
            uniform float iTime;
            uniform float2 uCenter;
            uniform float uSize;

            half4 main(float2 fragCoord) {
                float2 uv = (fragCoord - uCenter) / uSize;

                // radial distance
                float dist = length(uv);

                // spectral flow distortion
                float wave = sin(dist * 10.0 - iTime * 4.0) * 0.05;
                float alpha = smoothstep(0.6, 0.1, dist + wave);

                // ghostly color gradient
                float3 color = mix(
                    float3(0.6, 0.7, 1.0),  // inner bluish core
                    float3(0.4, 0.2, 0.6),  // outer purple aura
                    dist + wave
                );

                return half4(color, alpha * 0.7);
            }
            """
        )
    }


    Canvas(modifier = modifier.fillMaxSize()) {
        val widthPx = size.width
        val heightPx = size.height

        val timeSec = globalTime * 10f
        ghostShader.setFloatUniform("iResolution", widthPx, heightPx)
        ghostShader.setFloatUniform("iTime", timeSec)

        drawRect(
            brush = ShaderBrush(ghostShader),
            size = Size(widthPx * 2f, heightPx * 2f),
            alpha = 0.3f
        )

        particles.forEach { p ->
            // Life progress looping 0..1
            var lifeProgress = (globalTime - p.startTime)
            if (lifeProgress < 0f) lifeProgress += 1f

            // Vertical position (rises up)
            val yPos = heightPx * (1f - lifeProgress * p.verticalSpeed * 33f) // scale to screen height

            // Horizontal sway
            val swayAmplitude = 20f
            val xPos = p.x * widthPx + swayAmplitude * sin((lifeProgress * 2 * Math.PI + p.swayPhase)).toFloat()

            // Size oscillates gently
            val sizeOscillation = 1f + 0.2f * sin(lifeProgress * 6 * Math.PI + p.alphaPhase)
            val size: Float = (baseSizePx * p.sizeFactor * sizeOscillation).toFloat()

            // Alpha flickers softly
            val alpha: Float = baseColor.alpha * (0.5f + 0.5f * sin(lifeProgress * 10 * Math.PI + p.alphaPhase)).toFloat()

            // Rotation angle in degrees
            val rotation = lifeProgress * 360f * p.rotationSpeed

            withTransform({
                translate(left = xPos, top = yPos)
                rotate(rotation)
            }) {
                // Draw multiple overlapping ellipses for wispy shape
                drawOval(
                    color = baseColor.copy(alpha = alpha),
                    size = androidx.compose.ui.geometry.Size(size, size * 0.5f),
                    topLeft = Offset((-size / 2).toFloat(), (-size * 0.25f).toFloat())
                )
                drawOval(
                    color = baseColor.copy(alpha = alpha * 0.7f),
                    size = androidx.compose.ui.geometry.Size(size * 0.7f, size * 0.35f),
                    topLeft = Offset(-size * 0.35f, -size * 0.175f)
                )
                drawOval(
                    color = baseColor.copy(alpha = alpha * 0.4f),
                    size = androidx.compose.ui.geometry.Size(size * 0.5f, size * 0.25f),
                    topLeft = Offset(-size * 0.25f, -size * 0.125f)
                )
            }
        }
    }
}