package com.aditya1875.pokeverse.feature.pokemon.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.utils.Haptics
import com.aditya1875.pokeverse.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.koinInject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private val Void = Color(0xFF020509)
private val BgTop = Color(0xFF060D1F)
private val BgBottom = Color(0xFF0A1628)
private val CyanGlow = Color(0xFF29C6E0)
private val RedGlow = Color(0xFFE05A29)
private val Gold = Color(0xFFFFD66B)
private val White = Color(0xFFEEF4FF)
private val TrackColor = Color(0xFF16233E)
private val BallRed = Color(0xFFEE3B33)
private val BallWhite = Color(0xFFF4F6FA)
private val BallInk = Color(0xFF14171F)

// ── Timeline (ms on the master clock) ────────────────────────────────────────────────────────
// Every visual below is a pure function of this one clock, so the beats can't drift apart and
// retiming the sequence is a matter of editing these numbers.
private const val RING_IN_END = 700f
private const val BALL_IN_START = 250f
private const val BALL_IN_END = 800f
private const val ENERGY_START = 300f
private const val CHARGE_START = 1000f
private const val POP = 1240f
private const val OPEN_END = 1520f
private const val WORD_START = 1380f
private const val WORD_STAGGER = 60f
private const val WORD_LETTER_MS = 420f
private const val SHIMMER_START = 2300f
private const val SHIMMER_END = 2900f
private const val LOADER_START = 2400f
private const val CINEMATIC_MS = 3000

/** Tap-to-skip unlocks once the pop has landed, so nobody skips past the best moment by accident. */
private const val SKIP_UNLOCK = 1800f

private const val MAX_HOLD_MS = 4500
private const val FALLBACK_HOLD_MS = 3000

private const val WORD = "DEXVERSE"

private fun seg(t: Float, from: Float, to: Float, easing: Easing = LinearEasing): Float =
    easing.transform(((t - from) / (to - from)).coerceIn(0f, 1f))

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

/** A speck of energy that streams in toward the ball while it charges. */
private data class Mote(val angle: Float, val dist: Float, val delay: Float, val size: Float, val warm: Boolean)

/** A streak thrown outward when the ball pops. */
private data class Spark(val angle: Float, val reach: Float, val width: Float, val delay: Float, val tint: Int)

private fun generateMotes(count: Int): List<Mote> {
    val rnd = Random(11)
    return List(count) {
        Mote(
            angle = rnd.nextFloat() * 2f * PI.toFloat(),
            dist = 0.55f + rnd.nextFloat() * 0.5f,
            delay = rnd.nextFloat() * 260f,
            size = 1.6f + rnd.nextFloat() * 2.2f,
            warm = it % 2 == 0
        )
    }
}

private fun generateSparks(count: Int): List<Spark> {
    val rnd = Random(29)
    return List(count) {
        Spark(
            angle = (it / count.toFloat()) * 2f * PI.toFloat() + rnd.nextFloat() * 0.18f,
            reach = 0.28f + rnd.nextFloat() * 0.34f,
            width = 1.5f + rnd.nextFloat() * 2.5f,
            delay = rnd.nextFloat() * 90f,
            tint = it % 4
        )
    }
}

/**
 * The launch sequence. A Poké Ball powers up inside a ring of light, bursts open, and the wordmark
 * rises out of the flash.
 *
 * The sequence always plays in full, but it only ends once [ready] is true, so slow startup work
 * shows up as the loader holding near the end instead of the app popping in half-built. Tapping
 * after the burst skips the rest of the hold.
 */
@Composable
fun SplashScreen(
    ready: Boolean = true,
    onFinish: () -> Unit = {},
) {
    val soundManager: SoundManager = koinInject()
    val haptics: Haptics = koinInject()
    val readyState = rememberUpdatedState(ready)

    val transition = rememberInfiniteTransition(label = "splash")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val clock = remember { Animatable(0f) }
    val barProgress = remember { Animatable(0f) }
    var skipRequested by remember { mutableStateOf(false) }

    val stars = remember { generateStars(90) }
    val motes = remember { generateMotes(28) }
    val sparks = remember { generateSparks(34) }

    DisposableEffect(Unit) {
        onDispose { soundManager.stopIntroMusic() }
    }

    LaunchedEffect(Unit) {
        val musicMs = soundManager.playIntroMusic()
        val holdMs = (if (musicMs > 0) musicMs else FALLBACK_HOLD_MS).coerceIn(CINEMATIC_MS, MAX_HOLD_MS)

        launch { clock.animateTo(CINEMATIC_MS.toFloat(), tween(CINEMATIC_MS, easing = LinearEasing)) }
        launch { barProgress.animateTo(0.92f, tween((holdMs * 0.92f).toInt(), easing = LinearEasing)) }

        // Haptics follow the picture: a rising hum as the ball charges, two ticks as it squeezes,
        // a pop at the burst, and a soft click as the wordmark lands.
        launch {
            delay(ENERGY_START.toLong())
            haptics.play(Haptics.Haptic.CHARGE)
            delay((CHARGE_START - ENERGY_START).toLong())
            haptics.play(Haptics.Haptic.TICK)
            delay(120)
            haptics.play(Haptics.Haptic.TICK)
            delay((POP - CHARGE_START - 120).toLong())
            haptics.play(Haptics.Haptic.POP)
            delay(380)
            haptics.play(Haptics.Haptic.CLICK)
        }

        // Hold for the full length (or until the user taps through), then wait for the app.
        withTimeoutOrNull(holdMs.toLong()) { snapshotFlow { skipRequested }.first { it } }
        snapshotFlow { readyState.value }.first { it }
        barProgress.animateTo(1f, tween(180))
        onFinish()
    }

    val tapSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            .clickable(interactionSource = tapSource, indication = null) {
                if (clock.value >= SKIP_UNLOCK) skipRequested = true
            },
        contentAlignment = Alignment.Center
    ) {

        // Corner ambience + stars + vignette share a single canvas pass.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fadeIn = seg(clock.value, 0f, 600f)

            // Centres sit outside the frame so the gradient has faded out before the edge.
            val cyanCenter = Offset(-size.width * 0.35f, size.height * 0.05f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(CyanGlow.copy(alpha = 0.16f * fadeIn), Color.Transparent),
                    center = cyanCenter, radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f, center = cyanCenter, blendMode = BlendMode.Screen
            )
            val redCenter = Offset(size.width * 1.35f, size.height * 0.95f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(RedGlow.copy(alpha = 0.14f * fadeIn), Color.Transparent),
                    center = redCenter, radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f, center = redCenter, blendMode = BlendMode.Screen
            )

            stars.forEach { star ->
                val twinkle = 0.55f + 0.45f * sin(((spin + star.phaseDeg) * PI.toFloat()) / 180f)
                drawCircle(
                    color = White,
                    radius = star.radius,
                    center = Offset(star.xFrac * size.width, star.yFrac * size.height),
                    alpha = (star.baseAlpha * twinkle * fadeIn).coerceIn(0f, 1f)
                )
            }

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Void.copy(alpha = 0.6f)),
                    center = Offset(size.width / 2f, size.height * 0.5f),
                    radius = size.maxDimension * 0.72f
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, BgBottom.copy(alpha = 0.85f), Void)))
        )

        // The stage: glow, ring, energy, ball, flash, shockwaves and sparks.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = clock.value
            val c = Offset(size.width / 2f, size.height / 2f)
            val base = 220.dp.toPx()

            // 0 → 1 while the ball squeezes, back to 0 after it opens.
            val charge = if (t < POP) seg(t, CHARGE_START, POP) else 1f - seg(t, POP, POP + 260f)
            val kick = if (t >= POP) (1f - seg(t, POP, 1800f)).pow(2) else 0f

            // Two-tone glow behind everything; swells while charging, settles once the name is up.
            val glow = (0.25f + 0.3f * seg(t, ENERGY_START, 1100f) + 0.35f * charge + 0.4f * kick -
                0.22f * seg(t, 1500f, 2300f)).coerceIn(0f, 1f) * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(CyanGlow.copy(alpha = 0.35f * glow), Color.Transparent),
                    center = Offset(c.x - base * 0.35f, c.y), radius = base * 0.9f
                ),
                radius = base * 0.9f, center = Offset(c.x - base * 0.35f, c.y), blendMode = BlendMode.Screen
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(RedGlow.copy(alpha = 0.35f * glow), Color.Transparent),
                    center = Offset(c.x + base * 0.35f, c.y), radius = base * 0.9f
                ),
                radius = base * 0.9f, center = Offset(c.x + base * 0.35f, c.y), blendMode = BlendMode.Screen
            )

            // Ring of light: dots light up one after another, then it tightens with the charge
            // and kicks outward at the burst.
            val ringR = 120.dp.toPx() - 6f
            // After the burst the ring opens out into a halo around the wordmark, clear of the letters.
            val halo = seg(t, 1400f, 2200f, EaseOut)
            val ringScale = 1f - 0.06f * charge + 0.1f * kick + 0.5f * halo
            rotate(spin, c) {
                for (i in 0 until 24) {
                    val reveal = seg(t, i * 12f, i * 12f + 260f, FastOutSlowInEasing)
                    if (reveal <= 0f) continue
                    val angle = (i / 24f) * 2f * PI.toFloat()
                    val strong = i % 3 == 0
                    drawCircle(
                        color = if (i < 12) CyanGlow else RedGlow,
                        radius = (if (strong) 3.8f else 3.2f) * (0.3f + 0.7f * reveal) * (1f + 0.5f * kick),
                        center = Offset(
                            c.x + cos(angle) * ringR * ringScale,
                            c.y + sin(angle) * ringR * ringScale
                        ),
                        alpha = (if (strong) 0.9f else 0.28f) * reveal * (1f - 0.4f * halo)
                    )
                }
            }

            // Energy streams in toward the ball while it powers up.
            motes.forEach { m ->
                val p = seg(t, ENERGY_START + m.delay, CHARGE_START + 120f, FastOutSlowInEasing)
                if (p <= 0f || p >= 1f) return@forEach
                val d = m.dist * base * (1f - p)
                val dir = Offset(cos(m.angle), sin(m.angle))
                val head = c + dir * d
                val tail = c + dir * (d + 26.dp.toPx() * (1f - p))
                val tint = if (m.warm) RedGlow else CyanGlow
                drawLine(
                    brush = Brush.linearGradient(listOf(tint.copy(alpha = 0f), tint.copy(alpha = 0.85f)), tail, head),
                    start = tail, end = head, strokeWidth = m.size.dp.toPx() * 0.7f, cap = StrokeCap.Round
                )
                drawCircle(White, radius = m.size.dp.toPx() * 0.45f, center = head, alpha = p)
            }

            // The ball itself: rises in, squeezes, then splits around a beam of light.
            val appear = seg(t, BALL_IN_START, BALL_IN_END, EaseOutBack)
            val ballAlpha = seg(t, BALL_IN_START, 550f) * (1f - seg(t, 1440f, 1620f))
            if (ballAlpha > 0f) {
                val open = seg(t, POP, OPEN_END, EaseOut)
                val squeeze = 1f - 0.08f * charge
                val tremble = if (t in CHARGE_START..POP) sin(t * 0.09f) * 1.6f.dp.toPx() * charge else 0f
                scale(scale = (0.4f + 0.6f * appear) * squeeze * (1f + 0.05f * open), pivot = c) {
                    drawPokeBall(Offset(c.x + tremble, c.y), 84.dp.toPx(), open, ballAlpha)
                }
            }

            // The burst: white flash, two shockwaves, and a spray of sparks.
            if (t >= POP) {
                val flash = 0.95f * (1f - seg(t, POP, POP + 320f))
                if (flash > 0f) {
                    val r = size.maxDimension * (0.12f + 0.3f * seg(t, POP, POP + 460f, EaseOut))
                    drawCircle(
                        brush = Brush.radialGradient(listOf(White.copy(alpha = flash), Color.Transparent), c, r),
                        radius = r, center = c
                    )
                }
                for (k in 0..1) {
                    val p = seg(t, POP + k * 140f, POP + 760f + k * 140f, EaseOut)
                    if (p <= 0f || p >= 1f) continue
                    drawCircle(
                        color = if (k == 0) CyanGlow else RedGlow,
                        radius = p * size.maxDimension * 0.55f,
                        center = c,
                        alpha = (1f - p) * 0.7f,
                        style = Stroke(width = (7f - 5f * p).dp.toPx())
                    )
                }
                sparks.forEach { s ->
                    val p = seg(t, POP + s.delay, 2100f)
                    if (p <= 0f || p >= 1f) return@forEach
                    val d = s.reach * size.maxDimension * EaseOut.transform(p)
                    val dir = Offset(cos(s.angle), sin(s.angle))
                    val tint = when (s.tint) { 0 -> CyanGlow; 1 -> White; 2 -> RedGlow; else -> Gold }
                    drawLine(
                        color = tint,
                        start = c + dir * (d * 0.72f),
                        end = c + dir * d,
                        strokeWidth = s.width.dp.toPx() * (1f - 0.5f * p),
                        cap = StrokeCap.Round,
                        alpha = (1f - p).pow(1.4f)
                    )
                }
            }
        }

        // Wordmark: letters rise out of the flash one by one, then a highlight sweeps across.
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val p = seg(clock.value, SHIMMER_START, SHIMMER_END, FastOutSlowInEasing)
                    if (p > 0f && p < 1f) {
                        val x = -size.width * 0.25f + size.width * 1.5f * p
                        drawRect(
                            brush = Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent),
                                start = Offset(x - 70f, 0f),
                                end = Offset(x + 70f, size.height * 0.4f)
                            ),
                            blendMode = BlendMode.SrcAtop
                        )
                    }
                }
        ) {
            WORD.forEachIndexed { i, ch ->
                val start = WORD_START + i * WORD_STAGGER
                // Cyan on the left, white in the middle, red on the right.
                val frac = i / (WORD.length - 1f)
                val color = if (frac < 0.5f) lerp(CyanGlow, White, frac * 2f) else lerp(White, RedGlow, (frac - 0.5f) * 2f)
                Text(
                    text = ch.toString(),
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 36.sp,
                    modifier = Modifier.graphicsLayer {
                        val rise = seg(clock.value, start, start + WORD_LETTER_MS, EaseOutBack)
                        alpha = seg(clock.value, start, start + WORD_LETTER_MS * 0.6f)
                        translationY = (1f - rise) * 22.dp.toPx()
                        val s = 0.6f + 0.4f * rise
                        scaleX = s
                        scaleY = s
                    }
                )
            }
        }

        // Loader, tied to how long the hold actually is and to real readiness at the very end.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .graphicsLayer { alpha = seg(clock.value, LOADER_START, LOADER_START + 360f) }
        ) {
            Canvas(modifier = Modifier.width(200.dp).height(6.dp)) {
                val cornerRadius = CornerRadius(size.height / 2f)
                drawRoundRect(color = TrackColor, cornerRadius = cornerRadius)

                val fillWidth = size.width * barProgress.value.coerceIn(0f, 1f)
                if (fillWidth > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(CyanGlow, White, RedGlow), startX = 0f, endX = fillWidth
                        ),
                        size = Size(fillWidth, size.height),
                        cornerRadius = cornerRadius
                    )
                    val headX = fillWidth.coerceIn(size.height / 2f, size.width - size.height / 2f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(White.copy(alpha = 0.85f), Color.Transparent),
                            center = Offset(headX, size.height / 2f), radius = size.height * 2.4f
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

/** A Poké Ball that can split: [open] 0 is closed, 1 has the halves apart around a beam of light. */
private fun DrawScope.drawPokeBall(center: Offset, d: Float, open: Float, alpha: Float) {
    val r = d / 2f
    val band = d * 0.1f
    val gap = d * 0.34f * open
    val topLeft = Offset(center.x - r, center.y - r)
    val box = Size(d, d)

    // Light spilling out of the seam.
    if (open > 0f) {
        val beamH = d * (0.15f + 0.9f * open)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, White.copy(alpha = 0.95f * alpha), Color.Transparent),
                startY = center.y - beamH / 2f, endY = center.y + beamH / 2f
            ),
            topLeft = Offset(center.x - r * 0.9f, center.y - beamH / 2f),
            size = Size(d * 0.9f, beamH),
            blendMode = BlendMode.Screen
        )
    }

    // Top half (red) rises, bottom half (white) drops.
    drawArc(BallRed, 180f, 180f, true, Offset(topLeft.x, topLeft.y - gap), box, alpha = alpha)
    drawArc(BallWhite, 0f, 180f, true, Offset(topLeft.x, topLeft.y + gap), box, alpha = alpha)
    drawRect(BallInk, Offset(topLeft.x, center.y - gap - band / 2f), Size(d, band / 2f + 0.5f), alpha = alpha)
    drawRect(BallInk, Offset(topLeft.x, center.y + gap), Size(d, band / 2f), alpha = alpha)

    val buttonAlpha = alpha * (1f - (open * 5f).coerceAtMost(1f))
    if (buttonAlpha > 0f) {
        drawCircle(BallInk, d * 0.2f, center, alpha = buttonAlpha)
        drawCircle(BallWhite, d * 0.11f, center, alpha = buttonAlpha)
    }
    // Rim, drawn per half so it follows them apart.
    val rim = Stroke(d * 0.07f)
    drawArc(BallInk, 180f, 180f, false, Offset(topLeft.x + d * 0.03f, topLeft.y - gap + d * 0.03f), Size(d * 0.94f, d * 0.94f), alpha = alpha, style = rim)
    drawArc(BallInk, 0f, 180f, false, Offset(topLeft.x + d * 0.03f, topLeft.y + gap + d * 0.03f), Size(d * 0.94f, d * 0.94f), alpha = alpha, style = rim)
}
