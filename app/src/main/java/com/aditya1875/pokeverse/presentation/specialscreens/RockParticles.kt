package com.aditya1875.pokeverse.presentation.specialscreens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.presentation.ui.RockShape
import kotlinx.coroutines.delay
import kotlin.random.Random

@Preview
@Composable
fun RockParticleBackground(modifier: Modifier = Modifier) {
    data class RockParticle(
        var x: Float,
        var y: Float,
        var size: Float,
        var velocityY: Float,
        var velocityX: Float,
        var rotation: Float,
        var rotationSpeed: Float,
        val color: Color,
        val shape: RockShape,
        val createdAt: Long,
        val lifetime: Long
    )

    val particles = remember { mutableStateListOf<RockParticle>() }
    val density = LocalDensity.current
    val rnd = remember { Random(System.currentTimeMillis()) }

    // Spawn rocks from top/sides
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < 25) { // limit max particles
                val sizePx = (rnd.nextInt(8, 20)).dp.toPx(density)
                val now = System.currentTimeMillis()

                particles.add(
                    RockParticle(
                        x = 0.2f + rnd.nextFloat() * 0.6f, // spawn in center area (normalized)
                        y = -0.1f, // start above screen (normalized)
                        size = sizePx,
                        velocityY = rnd.nextFloat() * 0.003f + 0.002f, // slower, more natural fall
                        velocityX = (rnd.nextFloat() - 0.5f) * 0.0008f, // subtle horizontal drift
                        rotation = rnd.nextFloat() * 360f,
                        rotationSpeed = (rnd.nextFloat() - 0.5f) * 2.5f,
                        color = listOf(
                            Color(0xFF8B7355), // warm brown
                            Color(0xFF6B5D52), // dark brown
                            Color(0xFF9C8B7A), // light brown
                            Color(0xFF7A6A5C), // medium brown
                            Color(0xFFA89988), // tan
                            Color(0xFF5C4F44)  // deep brown
                        ).random(rnd),
                        shape = RockShape.entries.toTypedArray().random(rnd),
                        createdAt = now,
                        lifetime = rnd.nextLong(3000L, 5000L)
                    )
                )
            }
            delay(150L) // spawn rate
        }
    }

    // Physics update
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            val iterator = particles.iterator()

            while (iterator.hasNext()) {
                val p = iterator.next()
                val age = now - p.createdAt

                // Remove if lifetime exceeded or off screen
                if (age > p.lifetime || p.y > 1.2f) {
                    iterator.remove()
                    continue
                }

                // Apply physics
                p.y += p.velocityY
                p.x += p.velocityX
                p.rotation += p.rotationSpeed

                // Gravity acceleration (normalized)
                p.velocityY += 0.00008f
            }

            delay(16L)
        }
    }

    // Draw rocks with irregular shapes
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val now = System.currentTimeMillis()

        particles.forEach { p ->
            val centerX = p.x * w
            val centerY = p.y * h
            val age = now - p.createdAt
            val t = (age / p.lifetime.toFloat()).coerceIn(0f, 1f)

            // Fade in and out
            val fadeIn = (t * 5f).coerceIn(0f, 1f)
            val fadeOut = 1f - t
            val alpha = (fadeIn * fadeOut).coerceIn(0f, 1f)

            rotate(p.rotation, Offset(centerX, centerY)) {
                when (p.shape) {
                    RockShape.IRREGULAR_1 -> {
                        // Irregular pentagon-ish rock
                        val path = Path().apply {
                            moveTo(centerX, centerY - p.size * 0.5f)
                            lineTo(centerX + p.size * 0.4f, centerY - p.size * 0.2f)
                            lineTo(centerX + p.size * 0.5f, centerY + p.size * 0.3f)
                            lineTo(centerX - p.size * 0.3f, centerY + p.size * 0.5f)
                            lineTo(centerX - p.size * 0.5f, centerY)
                            close()
                        }

                        // Shadow
                        drawPath(
                            path = path,
                            color = Color.Black.copy(alpha = alpha * 0.3f),
                            style = Stroke(width = 2f)
                        )

                        // Main rock
                        drawPath(
                            path = path,
                            color = p.color.copy(alpha = alpha * 0.8f)
                        )

                        // Highlight
                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = alpha * 0.15f),
                            style = Stroke(width = 1.5f)
                        )
                    }

                    RockShape.IRREGULAR_2 -> {
                        // Angular hexagon rock
                        val path = Path().apply {
                            moveTo(centerX, centerY - p.size * 0.6f)
                            lineTo(centerX + p.size * 0.5f, centerY - p.size * 0.3f)
                            lineTo(centerX + p.size * 0.5f, centerY + p.size * 0.2f)
                            lineTo(centerX + p.size * 0.2f, centerY + p.size * 0.5f)
                            lineTo(centerX - p.size * 0.4f, centerY + p.size * 0.4f)
                            lineTo(centerX - p.size * 0.5f, centerY - p.size * 0.1f)
                            close()
                        }

                        drawPath(path, color = Color.Black.copy(alpha = alpha * 0.3f), style = Stroke(width = 2f))
                        drawPath(path, color = p.color.copy(alpha = alpha * 0.8f))
                        drawPath(path, color = Color.White.copy(alpha = alpha * 0.15f), style = Stroke(width = 1.5f))
                    }

                    RockShape.IRREGULAR_3 -> {
                        // Chunky quadrilateral rock
                        val path = Path().apply {
                            moveTo(centerX - p.size * 0.3f, centerY - p.size * 0.5f)
                            lineTo(centerX + p.size * 0.5f, centerY - p.size * 0.4f)
                            lineTo(centerX + p.size * 0.4f, centerY + p.size * 0.5f)
                            lineTo(centerX - p.size * 0.5f, centerY + p.size * 0.3f)
                            close()
                        }

                        drawPath(path, color = Color.Black.copy(alpha = alpha * 0.3f), style = Stroke(width = 2f))
                        drawPath(path, color = p.color.copy(alpha = alpha * 0.8f))
                        drawPath(path, color = Color.White.copy(alpha = alpha * 0.15f), style = Stroke(width = 1.5f))
                    }

                    RockShape.JAGGED -> {
                        // Jagged rock with sharp edges
                        val path = Path().apply {
                            moveTo(centerX, centerY - p.size * 0.6f)
                            lineTo(centerX + p.size * 0.3f, centerY - p.size * 0.4f)
                            lineTo(centerX + p.size * 0.6f, centerY - p.size * 0.1f)
                            lineTo(centerX + p.size * 0.4f, centerY + p.size * 0.3f)
                            lineTo(centerX + p.size * 0.1f, centerY + p.size * 0.6f)
                            lineTo(centerX - p.size * 0.3f, centerY + p.size * 0.4f)
                            lineTo(centerX - p.size * 0.6f, centerY + p.size * 0.1f)
                            lineTo(centerX - p.size * 0.4f, centerY - p.size * 0.2f)
                            close()
                        }

                        drawPath(path, color = Color.Black.copy(alpha = alpha * 0.3f), style = Stroke(width = 2f))
                        drawPath(path, color = p.color.copy(alpha = alpha * 0.8f))
                        drawPath(path, color = Color.White.copy(alpha = alpha * 0.15f), style = Stroke(width = 1.5f))
                    }
                }
            }
        }
    }
}

private fun Int.toPx(density: Density): Float = with(density) { this@toPx.dp.toPx() }

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
