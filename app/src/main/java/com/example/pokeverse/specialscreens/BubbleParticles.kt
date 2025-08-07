package com.example.pokeverse.specialscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha),
                radius = particle.radius * density,
                center = Offset(particle.x * size.width, particle.y * size.height),
                style = Stroke(width = 1.5f * density) // outline for more bubble look
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

