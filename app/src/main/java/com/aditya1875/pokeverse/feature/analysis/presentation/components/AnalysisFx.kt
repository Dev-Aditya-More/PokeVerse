package com.aditya1875.pokeverse.feature.analysis.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Springs a section up from below the first time it appears (staggered by [index]), then leaves it
 * alone: [seen] remembers what already played so scrolling back never replays the entrance.
 */
@Composable
fun Reveal(
    index: Int,
    seen: MutableMap<Int, Boolean>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val already = remember { seen[index] == true }
    val progress = remember { Animatable(if (already) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!already) {
            // Cap the stagger so something scrolled into view later doesn't sit invisible for a second.
            delay(index.coerceAtMost(4) * 90L)
            progress.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
            seen[index] = true
        }
    }
    Box(
        modifier.graphicsLayer {
            val p = progress.value
            alpha = p.coerceIn(0f, 1f)
            translationY = (1f - p) * 56.dp.toPx()
            val s = 0.94f + 0.06f * p
            scaleX = s
            scaleY = s
        }
    ) { content() }
}

/** Slowly drifting colour blobs behind the whole screen; the main one takes the team's score colour. */
@Composable
fun AmbientBackdrop(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ambient")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val a = phase * 2f * PI.toFloat()
        fun blob(cx: Float, cy: Float, r: Float, c: Color, alpha: Float) {
            val center = Offset(cx, cy)
            drawCircle(
                brush = Brush.radialGradient(listOf(c.copy(alpha = alpha), Color.Transparent), center = center, radius = r),
                radius = r,
                center = center,
            )
        }
        blob(w * (0.2f + 0.15f * cos(a)), h * (0.18f + 0.06f * sin(a)), w * 0.75f, color, 0.22f)
        blob(w * (0.85f + 0.10f * sin(a)), h * (0.50f + 0.08f * cos(a)), w * 0.60f, AnalysisColors.PURPLE, 0.13f)
        blob(w * (0.30f + 0.12f * sin(a)), h * (0.88f + 0.05f * cos(a)), w * 0.65f, AnalysisColors.BLUE, 0.11f)
    }
}

private class ConfettiBit(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val spin: Float,
)

private val ConfettiPalette = listOf(
    Color(0xFFFFD54F), Color(0xFF00E676), Color(0xFF40C4FF), Color(0xFFFF4C60), Color(0xFFCE93D8), Color(0xFFFFFFFF),
)

/** One burst of confetti from the upper middle of its box, fired whenever [fire] turns true. */
@Composable
fun Confetti(fire: Boolean, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(fire) {
        if (fire) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(2000, easing = LinearEasing))
        }
    }
    val bits = remember {
        val r = Random(11)
        List(46) {
            ConfettiBit(
                angle = (r.nextFloat() * 2f * PI).toFloat(),
                speed = 0.25f + r.nextFloat() * 0.65f,
                size = 6f + r.nextFloat() * 8f,
                color = ConfettiPalette[r.nextInt(ConfettiPalette.size)],
                spin = r.nextFloat() * 2f - 1f,
            )
        }
    }
    val t = progress.value
    if (t > 0.001f && t < 0.999f) {
        Canvas(modifier) {
            val origin = Offset(size.width / 2f, size.height * 0.38f)
            bits.forEach { b ->
                val dist = b.speed * t * size.width * 0.9f
                val x = origin.x + cos(b.angle) * dist
                val y = origin.y + sin(b.angle) * dist + size.height * 0.9f * t * t
                val s = b.size * density
                rotate(b.spin * t * 540f, pivot = Offset(x, y)) {
                    drawRect(
                        color = b.color.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                        topLeft = Offset(x - s / 2f, y - s * 0.3f),
                        size = Size(s, s * 0.6f),
                    )
                }
            }
        }
    }
}

/** Counts from 0 up to [target] the first time it is shown, then follows any new value. */
@Composable
fun rememberCountUp(target: Int, durationMs: Int = 1100): Int {
    var goal by remember { mutableIntStateOf(0) }
    LaunchedEffect(target) { goal = target }
    val value by animateIntAsState(goal, tween(durationMs, easing = FastOutSlowInEasing), label = "countUp")
    return value
}
