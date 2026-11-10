package com.aditya1875.pokeverse.feature.pokemon.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.delay
import kotlin.random.Random

/** How Professor Dex feels about the current line. Each mood is a small set of face parameters. */
enum class ProfessorMood(
    val smile: Float,
    val openMouth: Float,
    val eyeScale: Float,
    val browLift: Float,
    val browTilt: Float,
    val lookX: Float,
    val lookY: Float,
    val tilt: Float,
    val blush: Boolean,
) {
    NEUTRAL(0.55f, 0f, 1f, 0f, 0f, 0f, 0.15f, 0f, false),
    HAPPY(1f, 0f, 1.05f, 0.01f, -4f, 0f, 0f, 0f, true),
    EXCITED(1f, 0.55f, 1.2f, 0.04f, -8f, 0f, -0.1f, 0f, true),
    SURPRISED(0f, 0.7f, 1.3f, 0.06f, -6f, 0f, 0f, 0f, false),
    THINKING(0.15f, 0f, 0.9f, 0.02f, 10f, 0.7f, -0.6f, -7f, false),
}

private val BallRed = Color(0xFFEE3B33)
private val BallWhite = Color(0xFFF4F6FA)
private val Ink = Color(0xFF14171F)
private val Blush = Color(0xFFFF8FA3)

/**
 * Professor Dex: a Poké Ball with a face. He bobs, blinks on his own schedule, moves his mouth
 * while [talking], pushes his brows around with [mood], and hops whenever [nudge] changes
 * (bump it once per new line of dialogue). Drawn entirely in code, so it needs no franchise
 * artwork and is unaffected by the Original Assets setting.
 */
@Composable
fun ProfessorBall(
    mood: ProfessorMood,
    talking: Boolean,
    accent: Color,
    nudge: Int,
    modifier: Modifier = Modifier,
) {
    val idle = rememberInfiniteTransition(label = "professor")
    val bob by idle.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val glow by idle.animateFloat(
        initialValue = 0.16f, targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val chatter by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(150, easing = LinearEasing), RepeatMode.Reverse),
        label = "chatter",
    )

    val blink = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2200, 4600))
            blink.animateTo(0.05f, tween(70))
            blink.animateTo(1f, tween(110))
        }
    }

    // A quick hop-and-squash on every new line, so he reads as reacting rather than just sitting.
    val hop = remember { Animatable(0f) }
    LaunchedEffect(nudge) {
        if (nudge > 0) {
            hop.animateTo(1f, tween(110, easing = EaseOut))
            hop.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
    }

    val ease = tween<Float>(260)
    val smile by animateFloatAsState(mood.smile, ease, label = "smile")
    val openBase by animateFloatAsState(mood.openMouth, ease, label = "open")
    val eyeScale by animateFloatAsState(mood.eyeScale, ease, label = "eyeScale")
    val browLift by animateFloatAsState(mood.browLift, ease, label = "browLift")
    val browTilt by animateFloatAsState(mood.browTilt, ease, label = "browTilt")
    val lookX by animateFloatAsState(mood.lookX, ease, label = "lookX")
    val lookY by animateFloatAsState(mood.lookY, ease, label = "lookY")
    val tilt by animateFloatAsState(mood.tilt, ease, label = "tilt")
    val blush by animateFloatAsState(if (mood.blush) 0.55f else 0f, ease, label = "blush")

    Canvas(modifier) {
        val s = size.minDimension
        val d = s * 0.74f
        val r = d / 2f
        val cx = s / 2f
        val restY = s * 0.47f
        val lift = bob * s * 0.022f + hop.value * s * 0.1f
        val cy = restY - lift

        // Soft glow in the accent colour, and a shadow that shrinks as he rises.
        drawCircle(
            brush = Brush.radialGradient(listOf(accent.copy(alpha = glow), Color.Transparent), Offset(cx, restY), s * 0.5f),
            radius = s * 0.5f, center = Offset(cx, restY),
        )
        val shadow = (1f - lift / s * 2.2f).coerceIn(0.4f, 1f)
        drawOval(
            Color.Black.copy(alpha = 0.32f * shadow),
            Offset(cx - d * 0.34f * shadow, restY + r + s * 0.03f),
            Size(d * 0.68f * shadow, d * 0.09f * shadow),
        )

        withTransform({
            rotate(tilt, Offset(cx, cy))
            scale(1f + hop.value * 0.05f, 1f - hop.value * 0.07f, Offset(cx, cy + r))
        }) {
            val c = Offset(cx, cy)
            val band = d * 0.1f
            drawArc(BallRed, 180f, 180f, true, Offset(cx - r, cy - r), Size(d, d))
            drawArc(BallWhite, 0f, 180f, true, Offset(cx - r, cy - r), Size(d, d))
            drawRect(Ink, Offset(cx - r, cy - band / 2f), Size(d, band))
            drawCircle(Ink, d * 0.2f, c)
            drawCircle(BallWhite, d * 0.11f, c)
            drawCircle(Ink, r - d * 0.03f, c, style = Stroke(d * 0.07f))

            // Cheeks.
            if (blush > 0.01f) {
                for (sgn in intArrayOf(-1, 1)) {
                    drawCircle(Blush.copy(alpha = blush), d * 0.055f, Offset(cx + sgn * d * 0.29f, cy + d * 0.3f))
                }
            }

            // Eyes behind round professor glasses, on the red half.
            val er = d * 0.085f * eyeScale
            val ey = cy - d * 0.2f
            val lens = d * 0.125f
            for (sgn in intArrayOf(-1, 1)) {
                val ex = cx + sgn * d * 0.17f
                drawCircle(Color.White.copy(alpha = 0.12f), lens, Offset(ex, ey))
                val h = er * 2f * blink.value
                drawOval(BallWhite, Offset(ex - er, ey - h / 2f), Size(er * 2f, h))
                val px = ex + lookX * er * 0.45f
                val py = ey + lookY * er * 0.45f * blink.value
                drawOval(Ink, Offset(px - er * 0.55f, py - er * 0.55f * blink.value), Size(er * 1.1f, er * 1.1f * blink.value))
                if (blink.value > 0.5f) {
                    drawCircle(Color.White, er * 0.22f, Offset(px - er * 0.18f, py - er * 0.2f))
                    // A second glint on the cheerful moods makes the eyes sparkle.
                    if (mood == ProfessorMood.HAPPY || mood == ProfessorMood.EXCITED) {
                        drawCircle(Color.White, er * 0.1f, Offset(px + er * 0.2f, py + er * 0.18f))
                    }
                }
                drawCircle(Ink.copy(alpha = 0.9f), lens, Offset(ex, ey), style = Stroke(d * 0.028f))

                // Brow: the right one rides a little higher while he's thinking.
                val extra = if (mood == ProfessorMood.THINKING && sgn == 1) d * 0.03f else 0f
                val by = ey - lens - d * 0.045f - (browLift * d) - extra
                withTransform({ rotate(sgn * browTilt, Offset(ex, by)) }) {
                    drawLine(
                        Ink, Offset(ex - d * 0.075f, by), Offset(ex + d * 0.075f, by),
                        strokeWidth = d * 0.04f, cap = StrokeCap.Round,
                    )
                }
            }
            drawLine(
                Ink.copy(alpha = 0.9f),
                Offset(cx - d * 0.17f + lens, ey), Offset(cx + d * 0.17f - lens, ey),
                strokeWidth = d * 0.028f,
            )

            // Mouth: a smile at rest, opening and closing while he talks.
            val open = maxOf(openBase, if (talking) chatter * 0.75f else 0f)
            val my = cy + d * 0.33f
            val w = d * 0.24f
            if (open < 0.1f) {
                val sh = w * (0.15f + 0.65f * smile)
                drawArc(
                    Ink, 15f, 150f, false,
                    Offset(cx - w / 2f, my - sh / 2f), Size(w, sh),
                    style = Stroke(d * 0.04f, cap = StrokeCap.Round),
                )
            } else {
                val mh = d * 0.13f * open + 2f
                val mw = w * (if (mood == ProfessorMood.SURPRISED) 0.6f else 0.85f)
                drawOval(Ink, Offset(cx - mw / 2f, my - mh / 2f), Size(mw, mh))
                if (open > 0.4f) {
                    drawOval(
                        Color(0xFFFF7A8C),
                        Offset(cx - mw * 0.28f, my + mh * 0.08f), Size(mw * 0.56f, mh * 0.38f),
                    )
                }
            }
        }
    }
}
