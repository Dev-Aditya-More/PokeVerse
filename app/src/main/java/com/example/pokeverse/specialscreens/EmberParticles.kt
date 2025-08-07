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
fun EmberParticles(modifier: Modifier = Modifier) {
    val emberCount = 25
    val infiniteTransition = rememberInfiniteTransition(label = "ember_transition")

    val emberParticles = remember {
        List(emberCount) {
            EmberParticle(
                startX = Random.nextFloat(),
                delay = Random.nextInt(0, 2000),
                speed = Random.nextFloat() * 0.5f + 0.5f, // 0.5x to 1.0x
                color = listOf(
                    Color(0xFFFF6B00),
                    Color(0xFFFFB300),
                    Color(0xFFDD2C00)
                ).random(),
                size = Random.nextFloat() * 6f + 4f // 4 to 10 dp
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        emberParticles.forEach { particle ->
            val yAnim by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (4000 / particle.speed).toInt(),
                        delayMillis = particle.delay,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "y_anim"
            )

            val alphaAnim by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 800,
                        delayMillis = particle.delay,
                        easing = FastOutLinearInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha_anim"
            )

            val xOffset = sin((1f - yAnim) * 6f * PI).toFloat() * 0.02f // slight left-right flicker

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = alphaAnim
                        translationX = (particle.startX + xOffset - 0.5f) * size.width
                        translationY = yAnim * size.height
                    }
                    .size(particle.size.dp)
                    .background(
                        color = particle.color,
                        shape = CircleShape
                    )
            )
        }
    }
}

data class EmberParticle(
    val startX: Float,
    val delay: Int,
    val speed: Float,
    val color: Color,
    val size: Float
)


