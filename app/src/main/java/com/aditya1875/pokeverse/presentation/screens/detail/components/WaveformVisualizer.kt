package com.aditya1875.pokeverse.presentation.screens.detail.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    modifier: Modifier = Modifier,
    color: Color,
    isSpeaking: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")

    // Wave motion
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing)
        ),
        label = "phase"
    )

    // Smooth amplitude animation (0 → 1 when speaking, 1 → 0 when done)
    val amplitude by animateFloatAsState(
        targetValue = if (isSpeaking) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "amplitude"
    )

    // Smooth visibility fade
    val alpha by animateFloatAsState(
        targetValue = if (isSpeaking) 1f else 0f,
        animationSpec = tween(500, easing = LinearEasing),
        label = "alpha"
    )

    if (alpha > 0f) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(60.dp)
                .alpha(alpha)
        ) {
            val width = size.width
            val centerY = size.height / 2
            val path = Path()
            val wavelength = width / 1.3f
            val step = 6f

            path.moveTo(0f, centerY)

            for (x in 0..width.toInt() step step.toInt()) {
                val radians = (x / wavelength) * (2 * Math.PI).toFloat() + phase
                val y = centerY + sin(radians) * amplitude * centerY * 0.7f
                path.lineTo(x.toFloat(), y)
            }

            val gradientBrush = Brush.verticalGradient(
                listOf(
                    color.copy(alpha = 0.8f),
                    color.copy(alpha = 0.25f),
                    Color.Transparent
                )
            )

            drawPath(
                path = path,
                brush = gradientBrush,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun LayeredWaveformVisualizer(
    modifier: Modifier = Modifier,
    color: Color,
    isSpeaking: Boolean
) {
    Box(modifier = modifier) {
        if (isSpeaking) {
            // Base layer
            WaveformVisualizer(
                modifier = Modifier.alpha(0.4f),
                color = color.copy(alpha = 0.5f),
                isSpeaking = true
            )

            // Foreground layer
            WaveformVisualizer(
                modifier = Modifier.alpha(0.8f),
                color = color,
                isSpeaking = true
            )
        }
    }
}

