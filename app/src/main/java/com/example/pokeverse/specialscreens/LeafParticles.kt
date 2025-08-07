package com.example.pokeverse.specialscreens

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun LeafParticles(modifier: Modifier = Modifier) {
    val leafCount = 20
    val infiniteTransition = rememberInfiniteTransition()

    // Animate a "time" value to simulate passage of time
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f, // Just cycles every second
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearOutSlowInEasing)
        )
    )

    val leaves = remember {
        List(leafCount) {
            LeafParticle(
                startX = Random.nextFloat(),
                speed = Random.nextFloat() * 0.5f + 0.3f,
                sway = Random.nextFloat() * 30f + 10f,
                size = Random.nextFloat() * 20f + 20f,
                angleSpeed = Random.nextFloat() * 2f + 0.5f
            )
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenHeightPx = with(LocalDensity.current) { screenHeight.toPx() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val elapsed = time * 60f // Simulated time multiplier

        leaves.forEach { leaf ->
            val y = (elapsed * 100 * leaf.speed) % screenHeightPx
            val x = size.width * leaf.startX + kotlin.math.sin(y / 50f) * leaf.sway
            val angle = (elapsed * 60 * leaf.angleSpeed) % 360f

            rotate(angle, Offset(x, y)) {
                drawOval(
                    color = Color(0xFF66BB6A).copy(alpha = 0.8f),
                    topLeft = Offset(x - leaf.size / 2, y - leaf.size / 2),
                    size = Size(leaf.size, leaf.size / 2)
                )
            }
        }
    }
}

data class LeafParticle(
    val startX: Float,
    val speed: Float,
    val sway: Float,
    val size: Float,
    val angleSpeed: Float
)
