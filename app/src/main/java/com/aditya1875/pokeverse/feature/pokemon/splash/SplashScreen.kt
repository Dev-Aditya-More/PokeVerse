package com.aditya1875.pokeverse.feature.pokemon.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private val Void = Color(0xFF020509)
private val BgTop = Color(0xFF060D1F)
private val BgBottom = Color(0xFF0A1628)
private val CyanGlow = Color(0xFF29C6E0)
private val RedGlow = Color(0xFFE05A29)
private val White = Color(0xFFEEF4FF)
private val Muted = Color(0xFF8FA3C4)
private val TrackColor = Color(0xFF16233E)

private data class Star(
    val xFrac: Float,
    val yFrac: Float,
    val radius: Float,
    val baseAlpha: Float,
    val phaseDeg: Float
)

private fun generateStars(count: Int): List<Star> {
    val rnd = Random(20260910)
    return List(count) {
        Star(
            xFrac = rnd.nextFloat(),
            yFrac = rnd.nextFloat(),
            radius = rnd.nextFloat() * 1.4f + 0.6f,
            baseAlpha = rnd.nextFloat() * 0.35f + 0.15f,
            phaseDeg = rnd.nextFloat() * 360f
        )
    }
}

private const val MIN_HOLD_MS = 2400
private const val MAX_HOLD_MS = 6000
private const val FALLBACK_HOLD_MS = 2600

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun SplashScreen(onFinish: () -> Unit = {}) {

    val soundManager: SoundManager = koinInject()
    val haptic = LocalHapticFeedback.current

    // Ambient loop — runs for the whole life of the screen.
    val infiniteTransition = rememberInfiniteRing()
    val ringRotation = infiniteTransition.first
    val glowPulse = infiniteTransition.second

    val ballScale = remember { Animatable(0.6f) }
    val ballAlpha = remember { Animatable(0f) }
    val flashScale = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkScale = remember { Animatable(0.88f) }
    val taglineAlpha = remember { Animatable(0f) }
    val loadingAlpha = remember { Animatable(0f) }
    val loadingProgress = remember { Animatable(0f) }

    val stars = remember { generateStars(90) }

    DisposableEffect(Unit) {
        onDispose { soundManager.stopIntroMusic() }
    }

    LaunchedEffect(onFinish) {
        val musicDurationMs = soundManager.playIntroMusic()
        val holdMs = (if (musicDurationMs > 0) musicDurationMs else FALLBACK_HOLD_MS)
            .coerceIn(MIN_HOLD_MS, MAX_HOLD_MS)

        launch { loadingProgress.animateTo(1f, tween(holdMs, easing = LinearEasing)) }

        launch {
            ballAlpha.animateTo(1f, tween(320, easing = LinearEasing))
        }
        ballScale.animateTo(1f, tween(420, easing = EaseOutBack))

        // brief anticipation "charge" before it pops open
        ballScale.animateTo(0.9f, tween(120))
        ballScale.animateTo(1.05f, tween(120))

        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        launch { ballAlpha.animateTo(0f, tween(260)) }
        launch {
            flashAlpha.animateTo(0.9f, tween(140))
            flashAlpha.animateTo(0f, tween(340))
        }
        launch { flashScale.animateTo(2.6f, tween(460, easing = LinearEasing)) }

        delay(120.milliseconds)
        launch { wordmarkAlpha.animateTo(1f, tween(420)) }
        wordmarkScale.animateTo(1f, tween(420, easing = EaseOutBack))

        delay(160.milliseconds)
        launch { taglineAlpha.animateTo(1f, tween(360)) }
        loadingAlpha.animateTo(1f, tween(360))

        val elapsed = 320 + 420 + 120 + 120 + 260 + 120 + 160 + 360
        delay((holdMs - elapsed).coerceAtLeast(0).toLong().milliseconds)
        onFinish()
    }

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

        // Large soft ambient blobs anchored to the corners — gives the whole
        // frame depth instead of a flat void around the centered mark.
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Centers sit well outside the frame so the gradient has already
            // faded most of the way to transparent by the time it reaches the
            // screen edge — centering it near the corner instead left a hard,
            // uniformly-colored wall that got sliced off by the canvas bounds.
            val cyanCenter = Offset(-size.width * 0.35f, size.height * 0.05f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyanGlow.copy(alpha = 0.16f), Color.Transparent),
                    center = cyanCenter,
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = cyanCenter,
                blendMode = BlendMode.Screen
            )
            val redCenter = Offset(size.width * 1.35f, size.height * 0.95f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(RedGlow.copy(alpha = 0.14f), Color.Transparent),
                    center = redCenter,
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = redCenter,
                blendMode = BlendMode.Screen
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            stars.forEach { star ->
                val twinkle = 0.55f + 0.45f * sin(
                    ((ringRotation + star.phaseDeg) * PI.toFloat()) / 180f
                )
                drawCircle(
                    color = White,
                    radius = star.radius,
                    center = Offset(star.xFrac * size.width, star.yFrac * size.height),
                    alpha = (star.baseAlpha * twinkle).coerceIn(0f, 1f)
                )
            }
        }

        // Vignette to pull focus back to center and deepen the corners.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Void.copy(alpha = 0.6f)),
                    center = Offset(size.width / 2f, size.height * 0.42f),
                    radius = size.maxDimension * 0.72f
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, BgBottom.copy(alpha = 0.85f), Void)
                    )
                )
        )

        Canvas(
            modifier = Modifier
                .size(440.dp)
                .graphicsLayer { alpha = glowPulse }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val base = size.minDimension / 2f

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

        Canvas(modifier = Modifier.size(240.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f - 6f
            rotate(ringRotation, Offset(cx, cy)) {
                for (i in 0 until 24) {
                    val angle = (i / 24f) * 2f * PI.toFloat()
                    val dotAlpha = if (i % 3 == 0) 0.9f else 0.25f
                    drawCircle(
                        color = if (i < 12) CyanGlow else RedGlow,
                        radius = 3.5f,
                        center = Offset(cx + cos(angle) * r, cy + sin(angle) * r),
                        alpha = dotAlpha
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = flashScale.value
                    scaleY = flashScale.value
                }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(White.copy(alpha = flashAlpha.value), Color.Transparent)
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = ballAlpha.value > 0f,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.iconpokeball),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            alpha = ballAlpha.value
                            scaleX = ballScale.value
                            scaleY = ballScale.value
                        }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                ),
                modifier = Modifier.graphicsLayer {
                    alpha = wordmarkAlpha.value
                    scaleX = wordmarkScale.value
                    scaleY = wordmarkScale.value
                }
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .graphicsLayer { alpha = loadingAlpha.value }
        ) {
            Canvas(
                modifier = Modifier
                    .width(200.dp)
                    .height(6.dp)
            ) {
                val cornerRadius = CornerRadius(size.height / 2f)

                drawRoundRect(
                    color = TrackColor,
                    cornerRadius = cornerRadius
                )

                val fillWidth = size.width * loadingProgress.value.coerceIn(0f, 1f)
                if (fillWidth > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(CyanGlow, White, RedGlow),
                            startX = 0f,
                            endX = fillWidth
                        ),
                        size = Size(fillWidth, size.height),
                        cornerRadius = cornerRadius
                    )

                    // Bright glowing head riding the leading edge of the fill.
                    val headX = fillWidth.coerceIn(size.height / 2f, size.width - size.height / 2f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(White.copy(alpha = 0.85f), Color.Transparent),
                            center = Offset(headX, size.height / 2f),
                            radius = size.height * 2.4f
                        ),
                        radius = size.height * 2.4f,
                        center = Offset(headX, size.height / 2f),
                        blendMode = BlendMode.Screen
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberInfiniteRing(): Pair<Float, Float> {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "splash")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    return ringRotation to glowPulse
}
