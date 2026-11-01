package com.aditya1875.pokeverse.feature.pokemon.detail.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.aditya1875.pokeverse.utils.rememberDecorativeMotionEnabled
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/**
 * Premium "Living Aura": an ambient, type-driven backdrop behind the whole detail page.
 *
 * - Aurora: soft glows in the Pokémon's colours drift slowly and lag behind the scroll (parallax).
 * - Motes: each primary type has its own motion (embers rise, snow falls, sparks flicker...).
 * - Shiny adds golden glints; Legendary/Mythical get a slow halo framing the sprite.
 *
 * Readability: motes are dimmed over the centre column where the cards sit, and motes
 * stay under ~0.6 alpha. Cost: one Canvas; the clock is read only inside the draw lambda, so
 * a tick redraws this layer without recomposing the page. The clock runs only while the
 * screen is RESUMED and is off entirely with system animations off or Battery Saver on.
 */
@Composable
fun DetailAura(
    types: List<String>,
    baseColor: Color,
    isShiny: Boolean,
    isLegendary: Boolean,
    focusY: Dp,
    scrollPx: () -> Float,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = rememberDecorativeMotionEnabled()

    // Colours glide when moving along an evolution chain instead of snapping.
    val primary by animateColorAsState(baseColor, tween(800), label = "auraPrimary")
    val secondary by animateColorAsState(
        types.getOrNull(1)?.let(::pokemonTypeColor) ?: baseColor, tween(800), label = "auraSecondary"
    )
    val profile = remember(types.firstOrNull()) { profileFor(types.firstOrNull()) }

    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }

    val clock = remember { mutableFloatStateOf(0f) }
    if (motionEnabled) {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var last = 0L
                while (true) {
                    withFrameNanos { now ->
                        if (last != 0L) clock.floatValue += (now - last) / 1_000_000_000f
                        last = now
                    }
                }
            }
        }
    }

    Canvas(modifier.fillMaxSize().graphicsLayer { alpha = reveal.value }) {
        val t = clock.floatValue
        val scroll = scrollPx()
        val par = scroll / size.height

        drawAurora(t, par, primary, secondary)
        if (isLegendary) drawHalo(t, focusY.toPx() - scroll, 1f - scroll / ScrollCap.toPx())
        drawMotes(profile, t, par)
        if (isShiny) drawShinyGlints(t, par)
    }
}

/** Parallax and the halo only need the first stretch of scrolling; see [detailAuraScroll]. */
val ScrollCap: Dp = 240.dp

/**
 * Scroll distance for [DetailAura]. Exact offsets are only known while the header (item 0) is
 * first; capping below the header's height (~screen width) keeps the value continuous when
 * the header leaves and the list index changes. Read it inside the draw lambda.
 */
fun LazyListState.detailAuraScroll(capPx: Float): Float =
    if (firstVisibleItemIndex == 0) firstVisibleItemScrollOffset.toFloat().coerceAtMost(capPx) else capPx

// ─── Type motion profiles ─────────────────────────────────────────────────────

private enum class Drift { RISE, FALL, FLOAT, FLICKER }
private enum class Shape { DOT, RING, SPARKLE, STREAK, LEAF, FLAKE }

private class Profile(
    val drift: Drift,
    val shape: Shape,
    val color: Color,
    val count: Int,
    val speed: Float,
    val sizeDp: Float,
    val alpha: Float,
)

private fun profileFor(type: String?): Profile = when (type?.lowercase()) {
    "fire" -> Profile(Drift.RISE, Shape.DOT, Color(0xFFFF8A3D), 34, 1.6f, 1.6f, 0.32f)
    "water" -> Profile(Drift.RISE, Shape.RING, Color(0xFF9EE7FF), 24, 0.9f, 3.2f, 0.26f)
    "grass" -> Profile(Drift.FLOAT, Shape.LEAF, Color(0xFF7BD86A), 18, 0.8f, 3.4f, 0.26f)
    "bug" -> Profile(Drift.FLOAT, Shape.LEAF, Color(0xFFB5D96B), 18, 0.9f, 3f, 0.24f)
    "electric" -> Profile(Drift.FLICKER, Shape.STREAK, Color(0xFFFFE45C), 26, 1f, 5f, 0.4f)
    "ice" -> Profile(Drift.FALL, Shape.FLAKE, Color(0xFFDDF4FF), 28, 0.7f, 3.2f, 0.3f)
    "ground" -> Profile(Drift.FLOAT, Shape.DOT, Color(0xFFD9B98A), 30, 1.1f, 1.3f, 0.24f)
    "rock" -> Profile(Drift.FALL, Shape.DOT, Color(0xFFC9BBA4), 22, 0.6f, 1.6f, 0.22f)
    "flying" -> Profile(Drift.FLOAT, Shape.STREAK, Color(0xFFF2F6FF), 18, 2.2f, 7f, 0.2f)
    "poison" -> Profile(Drift.RISE, Shape.RING, Color(0xFFC77DFF), 22, 0.7f, 2.8f, 0.26f)
    "ghost" -> Profile(Drift.FLOAT, Shape.DOT, Color(0xFF9C7BFF), 16, 0.5f, 3.4f, 0.22f)
    "psychic" -> Profile(Drift.FLOAT, Shape.RING, Color(0xFFFF7EB6), 18, 0.6f, 3.6f, 0.22f)
    "fairy" -> Profile(Drift.FLOAT, Shape.SPARKLE, Color(0xFFFFB3E6), 26, 0.5f, 3.4f, 0.38f)
    "dragon" -> Profile(Drift.RISE, Shape.SPARKLE, Color(0xFF8C9EFF), 24, 0.8f, 3.4f, 0.34f)
    "dark" -> Profile(Drift.FLOAT, Shape.DOT, Color(0xFF8E7CC3), 20, 0.4f, 2.6f, 0.18f)
    "steel" -> Profile(Drift.FLOAT, Shape.SPARKLE, Color(0xFFDDE6EE), 22, 0.4f, 3f, 0.34f)
    "fighting" -> Profile(Drift.RISE, Shape.STREAK, Color(0xFFFF7043), 20, 2f, 5f, 0.22f)
    else -> Profile(Drift.FLOAT, Shape.DOT, Color(0xFFF5F0E6), 22, 0.6f, 1.6f, 0.2f)
}

// ─── Particles ────────────────────────────────────────────────────────────────

private class Mote(val x: Float, val y: Float, val speed: Float, val size: Float, val phase: Float)

private fun motes(count: Int, seed: Int): List<Mote> {
    val r = Random(seed)
    return List(count) {
        Mote(r.nextFloat(), r.nextFloat(), 0.01f + r.nextFloat() * 0.03f, 0.6f + r.nextFloat() * 0.8f, r.nextFloat() * 2f * PI.toFloat())
    }
}

// Stable layouts: moving along an evolution chain recolours/reshapes motes, it doesn't reshuffle them.
private val TypeMotes = motes(48, 7)
private val Glints = motes(16, 41)

private fun frac(v: Float) = v - floor(v)

private fun twinkle(t: Float, m: Mote, rate: Float = 1.4f) = 0.35f + 0.65f * ((sin(t * rate + m.phase) + 1f) / 2f)

/** Full strength at the screen edges, half over the centre column where the cards are. */
private fun edge(x: Float) = 0.5f + 0.5f * (abs(x - 0.5f) * 2f).coerceAtMost(1f)

private fun DrawScope.glow(color: Color, alpha: Float, cx: Float, cy: Float, radius: Float) {
    val c = Offset(size.width * cx, size.height * cy)
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), c, radius), radius, c)
}

private fun DrawScope.drawAurora(t: Float, par: Float, primary: Color, secondary: Color) {
    val w = size.width
    // Glows trail the content as it scrolls (slower than 1:1) — the depth cue.
    glow(primary, 0.32f, 0.22f + 0.14f * sin(t * 0.13f), 0.16f + 0.06f * cos(t * 0.17f) - par * 0.6f, w * 1.0f)
    glow(secondary, 0.22f, 0.82f + 0.1f * cos(t * 0.11f), 0.48f + 0.08f * sin(t * 0.09f) - par * 0.35f, w * 0.9f)
    glow(primary, 0.15f, 0.35f + 0.18f * sin(t * 0.07f + 2f), 0.92f + 0.04f * cos(t * 0.12f) - par * 0.15f, w * 0.95f)
}

private fun DrawScope.drawMotes(p: Profile, t: Float, par: Float) {
    val w = size.width
    val h = size.height
    // Tuned on-device: the cards are fairly opaque, so motes mostly show in the gutters and
    // around the sprite and can afford real presence there.
    val r = p.sizeDp.dp.toPx() * 1.4f
    val thin = 1.5.dp.toPx()
    val count = (p.count * 1.35f).toInt().coerceAtMost(TypeMotes.size)
    for (i in 0 until count) {
        val m = TypeMotes[i]
        var x: Float
        var y: Float
        when (p.drift) {
            Drift.RISE -> { y = m.y - t * m.speed * p.speed; x = m.x + sin(t * 0.6f + m.phase) * 0.02f }
            Drift.FALL -> { y = m.y + t * m.speed * p.speed; x = m.x + sin(t * 0.5f + m.phase) * 0.03f }
            Drift.FLOAT -> { x = frac(m.x + t * m.speed * p.speed * 0.5f); y = m.y + sin(t * 0.4f + m.phase) * 0.03f }
            Drift.FLICKER -> { x = m.x; y = m.y }
        }
        y = frac(y + par * 0.3f) // motes drift with the page a little too
        x = frac(x)

        val life = when {
            p.drift == Drift.FLICKER -> {
                // Sharp crackle: visible for a short slice of each cycle.
                val f = frac(t * (0.25f + m.speed * 10f) + m.phase / (2f * PI.toFloat()))
                if (f < 0.12f) 1f - f / 0.12f else 0f
            }
            p.shape == Shape.SPARKLE -> twinkle(t, m).let { it * it * it }
            else -> twinkle(t, m)
        }
        val a = (p.alpha * 1.7f).coerceAtMost(0.62f) * life * edge(x)
        if (a < 0.01f) continue

        val c = Offset(x * w, y * h)
        val size = r * m.size
        val color = p.color.copy(alpha = a)
        when (p.shape) {
            Shape.DOT -> {
                drawCircle(p.color.copy(alpha = a * 0.25f), size * 2.6f, c)
                drawCircle(color, size, c)
            }
            Shape.RING -> {
                drawCircle(color, size, c, style = Stroke(thin))
                drawCircle(p.color.copy(alpha = a * 0.8f), size * 0.18f, Offset(c.x - size * 0.4f, c.y - size * 0.4f))
            }
            Shape.SPARKLE -> sparkle(c, size, color, thin)
            Shape.STREAK -> {
                val angle = when (p.drift) {
                    Drift.FLICKER -> m.phase
                    Drift.RISE -> -PI.toFloat() / 2f
                    else -> 0f
                }
                val d = Offset(cos(angle) * size, sin(angle) * size)
                drawLine(color, c - d, c + d, thin * 1.4f, StrokeCap.Round)
            }
            Shape.LEAF -> rotate(t * 40f * m.speed * 20f + m.phase * 57f, c) {
                drawOval(color, Offset(c.x - size, c.y - size * 0.45f), Size(size * 2f, size * 0.9f))
            }
            Shape.FLAKE -> rotate(t * 20f + m.phase * 57f, c) {
                for (k in 0 until 3) {
                    val ang = k * PI.toFloat() / 3f
                    val d = Offset(cos(ang) * size, sin(ang) * size)
                    drawLine(color, c - d, c + d, thin, StrokeCap.Round)
                }
            }
        }
    }
}

private fun DrawScope.sparkle(c: Offset, radius: Float, color: Color, stroke: Float) {
    drawLine(color, Offset(c.x - radius, c.y), Offset(c.x + radius, c.y), stroke, StrokeCap.Round)
    drawLine(color, Offset(c.x, c.y - radius), Offset(c.x, c.y + radius), stroke, StrokeCap.Round)
    drawCircle(color, stroke * 1.1f, c)
}

private val Gold = Color(0xFFFFD54F)

private fun DrawScope.drawShinyGlints(t: Float, par: Float) {
    val w = size.width
    val h = size.height
    val thin = 1.3.dp.toPx()
    Glints.forEachIndexed { i, m ->
        val x = m.x
        val y = frac(m.y * 0.75f + par * 0.3f)
        val life = twinkle(t, m, 1.1f).let { it * it * it * it }
        val a = 0.55f * life * edge(x)
        if (a < 0.01f) return@forEachIndexed
        val color = (if (i % 3 == 0) Color.White else Gold).copy(alpha = a)
        sparkle(Offset(x * w, y * h), (4f + m.size * 3f).dp.toPx(), color, thin)
    }
}

/** Two counter-rotating gold arc rings framing the sprite; fades out as the header scrolls away. */
private fun DrawScope.drawHalo(t: Float, cy: Float, fade: Float) {
    if (fade <= 0f) return
    val c = Offset(size.width / 2f, cy)
    val r1 = 128.dp.toPx()
    val r2 = 146.dp.toPx()
    drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = 0.10f * fade), Color.Transparent), c, r2 * 1.25f), r2 * 1.25f, c)
    val stroke = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round)
    for (k in 0 until 3) {
        drawArc(
            Gold.copy(alpha = 0.26f * fade), t * 12f + k * 120f, 64f, false,
            Offset(c.x - r1, c.y - r1), Size(r1 * 2f, r1 * 2f), style = stroke
        )
        drawArc(
            Gold.copy(alpha = 0.14f * fade), -t * 8f + k * 120f + 60f, 38f, false,
            Offset(c.x - r2, c.y - r2), Size(r2 * 2f, r2 * 2f), style = stroke
        )
    }
}
