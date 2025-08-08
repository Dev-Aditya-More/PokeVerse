package com.example.pokeverse.specialscreens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FlyingParticles(
    modifier: Modifier = Modifier,
    streakColor: Color = Color.White.copy(alpha = 0.3f),
    streakWidth: Dp = 0.9.dp,  // thinner lines
    streakLength: Dp = 30.dp,
    gustSpeed: Float = 300f, // px per full cycle
    streakCount: Int = 10
) {
    val density = LocalDensity.current
    val streakLengthPx = with(density) { streakLength.toPx() }
    val streakWidthPx = with(density) { streakWidth.toPx() }

    // Initial offsets for streaks
    val streaks = remember {
        List(streakCount) {
            GustParticle(
                initialXOffset = Random.nextFloat(), // between 0 and 1
                y = Random.nextFloat() * 2000f,
                controlOffset = Random.nextFloat() * 80f - 40f,
                alpha = Random.nextFloat(),
                speed = gustSpeed
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "gustMovement")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "timeTick"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val widthPx = size.width

        streaks.forEach { streak ->
            // Calculate x based on time and initial offset, wrap around when > 1f
            val xProgress = (streak.initialXOffset + time) % 1f
            val xPos = xProgress * (widthPx + streakLengthPx) - streakLengthPx

            val start = Offset(xPos, streak.y % size.height)  // wrap y inside canvas height
            val end = Offset(xPos + streakLengthPx, streak.y % size.height)
            val control = Offset(
                xPos + streakLengthPx / 2,
                (streak.y % size.height) + streak.controlOffset
            )

            drawPath(
                path = Path().apply {
                    moveTo(start.x, start.y)
                    lineTo(end.x, end.y)  // straight line instead of curve
                },
                color = streakColor.copy(alpha = 0.2f + 0.5f * streak.alpha),
                style = Stroke(width = streakWidthPx, cap = StrokeCap.Round)
            )

        }
    }
}

private data class GustParticle(
    val initialXOffset: Float, // 0..1 normalized initial horizontal offset
    val y: Float,
    val controlOffset: Float,
    val alpha: Float,
    val speed: Float
)

