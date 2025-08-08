package com.example.pokeverse.specialscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun RockParticleBackground(modifier: Modifier = Modifier) {
    val particles = remember { mutableStateListOf<RockParticle>() }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        while (true) {
            particles.add(RockParticle.createRandom(density))
            delay(100L) // control spawn rate
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.update(size)
            if (particle.isOffScreen(size)) iterator.remove()
            else particle.draw(this)
        }
    }
}

private data class RockParticle(
    var x: Float,
    var y: Float,
    var rockSize: Float,
    var velocityY: Float,
    var velocityX: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    val color: Color
) {
    fun update(canvasSize: Size) {
        y += velocityY
        x += velocityX
        rotation += rotationSpeed

        // Optional bounce at bottom
        if (y + rockSize > canvasSize.height) {
            y = canvasSize.height - rockSize
            velocityY = -velocityY * 0.3f // dampened bounce
        }

        // Gravity pull
        velocityY += 0.15f
    }

    fun isOffScreen(canvasSize: Size) = y > canvasSize.height + rockSize

    fun draw(scope: DrawScope) {
        scope.rotate(rotation, Offset(x, y)) {
            scope.drawRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(rockSize, rockSize)
            )
        }
    }

    companion object {
        fun createRandom(density: Density): RockParticle {
            val sizePx = Random.nextInt(from = 6, until = 14).dpToPx(density)
            return RockParticle(
                x = Random.nextFloat() * 1080f,
                y = -sizePx,
                rockSize = sizePx,
                velocityY = Random.nextFloat() * 12f + 8f ,
                velocityX = Random.nextFloat() * 1f - 0.5f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 1.5f - 0.75f,
                color = listOf(
                    Color(0xFF8D8B88),
                    Color(0xFF6D6A68),
                    Color(0xFF9C9B94),
                    Color(0xFFB6A999)
                ).random()
            )
        }

        private fun Int.dpToPx(density: Density): Float =
            with(density) { this@dpToPx.dp.toPx() }


    }
}
