package com.aditya1875.pokeverse.presentation.screens.splash

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val x: Float,         // 0..1 normalized
    val y: Float,
    val radius: Float,
    val speed: Float,     // twinkle speed multiplier
    val phase: Float      // initial phase offset
)

private val stars: List<Star> = List(120) {
    Star(
        x = Random.nextFloat(),
        y = Random.nextFloat(),
        radius = Random.nextFloat() * 1.8f + 0.4f,
        speed = Random.nextFloat() * 0.6f + 0.4f,
        phase = Random.nextFloat() * 2f * PI.toFloat()
    )
}

private data class OrbRing(val radiusFactor: Float, val alpha: Float, val blur: Float)

private val orbRings = listOf(
    OrbRing(1.45f, 0.08f, 24f),
    OrbRing(1.20f, 0.14f, 16f),
    OrbRing(1.00f, 0.22f, 8f),
)

private val BgTop = Color(0xFF060D1F)
private val BgBottom = Color(0xFF0A1628)
private val CyanGlow = Color(0xFF29C6E0)
private val RedGlow = Color(0xFFE05A29)
private val White = Color(0xFFEEF4FF)

@Preview(showBackground = true)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SplashScreen() {

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "twinkle")
    val twinkleClock by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "clock"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )

    val p = progress.value

    val glowAlpha = ((p - 0.25f) * 2.5f).coerceIn(0f, 1f)

    val titleAlpha = ((p - 0.55f) * 3.5f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgTop, BgBottom)
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // ── star field ────────────────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawStars(stars, twinkleClock)
        }

        // ── glow aura behind logo ─────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .size(340.dp)
                .alpha(glowAlpha)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val base = size.minDimension / 2f

            // cyan left arc glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyanGlow.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(cx - base * 0.35f, cy),
                    radius = base * 0.9f
                ),
                radius = base * 0.9f,
                center = Offset(cx - base * 0.35f, cy),
                blendMode = BlendMode.Screen
            )
            // red right arc glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(RedGlow.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(cx + base * 0.35f, cy),
                    radius = base * 0.9f
                ),
                radius = base * 0.9f,
                center = Offset(cx + base * 0.35f, cy),
                blendMode = BlendMode.Screen
            )
        }

        // ── rotating accent ring ──────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .alpha(glowAlpha * 0.55f)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f - 6f
            rotate(ringRotation, Offset(cx, cy)) {
                // dashed-look: draw small arcs as dots around ring
                for (i in 0 until 36) {
                    val angle = (i / 36f) * 2f * PI.toFloat()
                    val dotAlpha = if (i % 3 == 0) 0.9f else 0.25f
                    drawCircle(
                        color = if (i < 18) CyanGlow else RedGlow,
                        radius = 2.5f,
                        center = Offset(cx + cos(angle) * r, cy + sin(angle) * r),
                        alpha = dotAlpha
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .alpha(titleAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "DEXVERSE",
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Black,
                fontSize = 38.sp,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(CyanGlow, White, RedGlow)
                    )
                )
            )
        }
    }
}

// ─── Draw helpers ─────────────────────────────────────────────────────────────
private fun DrawScope.drawStars(stars: List<Star>, clock: Float) {
    val w = size.width
    val h = size.height
    stars.forEach { star ->
        val twinkle = (sin(clock * star.speed + star.phase) * 0.5f + 0.5f)
        val alpha = twinkle * 0.75f + 0.15f
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = star.radius * (0.7f + twinkle * 0.6f),
            center = Offset(star.x * w, star.y * h)
        )
    }
}