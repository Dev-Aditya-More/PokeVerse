package com.aditya1875.pokeverse.specialscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun BubbleParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 30,
    colors: List<Color> = listOf(Color(0xFFB3E5FC), Color(0xFF81D4FA), Color(0xFF4FC3F7))
) {
    val particles = remember { mutableStateListOf<BubbleParticle>() }
    val density = LocalDensity.current.density

    // Add new bubbles
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < particleCount) {
                particles += BubbleParticle.generate(colors)
            }
            delay(60L)
        }
    }

    // Update bubbles
    LaunchedEffect(Unit) {
        while (true) {
            val iterator = particles.listIterator()
            while (iterator.hasNext()) {
                val p = iterator.next().update()
                if (p.isAlive()) {
                    iterator.set(p)
                } else {
                    iterator.remove()
                }
            }
            delay(16L)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val center = Offset(particle.x * size.width, particle.y * size.height)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        particle.color.copy(alpha = 0.3f), // center
                        Color.White.copy(alpha = 0.05f)    // edge
                    ),
                    center = center,
                    radius = particle.radius * density
                ),
                radius = particle.radius * density,
                center = center
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = particle.radius * density,
                center = center,
                style = Stroke(width = 1.2f * density)
            )

            drawOval(
                color = Color.White.copy(alpha = 0.4f),
                topLeft = Offset(
                    center.x - particle.radius * density * 0.5f,
                    center.y - particle.radius * density * 0.7f
                ),
                size = Size(
                    particle.radius * density * 0.6f,
                    particle.radius * density * 0.3f
                )
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

