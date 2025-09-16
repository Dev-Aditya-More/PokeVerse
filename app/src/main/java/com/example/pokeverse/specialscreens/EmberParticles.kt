package com.example.pokeverse.specialscreens

import android.R.attr.alpha
import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun EmberParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 60,
    colors: List<Color> = listOf(Color(0xFFFF6D00), Color(0xFFFFA726), Color(0xFFFFC107))
) {
    val particles = remember { mutableStateListOf<EmberParticle>() }
    val density = LocalDensity.current.density

    // Add new particles
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < particleCount) {
                particles += EmberParticle.generate(colors)
            }
            delay(50L)
        }
    }

    // Update particles
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
            delay(16L) // ~60 FPS
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
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
                    maskFilter = BlurMaskFilter(particle.size * density, BlurMaskFilter.Blur.NORMAL)
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
    val alpha: Float,
    val lifetime: Int,
    val color: Color,
    val age: Int = 0
) {
    fun update(): EmberParticle {
        val newAge = age + 1
        val progress = newAge.toFloat() / lifetime
        val newAlpha = 1f - progress
        val newY = y - velocityY

        return this.copy(
            y = newY,
            alpha = newAlpha,
            age = newAge
        )
    }

    fun isAlive(): Boolean = age < lifetime

    companion object {
        fun generate(colors: List<Color>): EmberParticle {
            return EmberParticle(
                x = Random.nextFloat(),
                y = 1f + Random.nextFloat() * 0.1f,
                size = Random.nextFloat() * 6f + 4f,
                velocityY = Random.nextFloat() * 0.008f + 0.004f,
                alpha = 1f,
                lifetime = 60 + Random.nextInt(60),
                color = colors.random()
            )
        }
    }
}


