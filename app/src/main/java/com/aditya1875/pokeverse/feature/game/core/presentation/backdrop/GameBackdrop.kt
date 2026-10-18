package com.aditya1875.pokeverse.feature.game.core.presentation.backdrop

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ambient, code-drawn scenery for game screens.
 *
 * Readability rules every scene follows so the backdrop never fights the UI:
 * - Motifs live in the edges/corners; the centre 60% only gets soft glows.
 * - Motif alpha stays ≤ ~0.08, particles ≤ ~0.25, and are dimmed further mid-screen.
 * - A scrim darkens the top HUD band and bottom action band.
 * - Feedback ([pulseKey]) is a brief edge vignette, never a centre flash.
 * - Animation freezes while [paused], and is static when system animations are off.
 */
sealed interface GameScene {
    data object Hub : GameScene
    data object Lab : GameScene
    data class TypeArena(val typeColor: Color) : GameScene
    data object Spotlight : GameScene
    data object TrainerTable : GameScene
    data object DuelArena : GameScene
    data class Weather(val sky: SkyWeather) : GameScene
    data class Wild(val biome: WildBiome) : GameScene
    data object Stadium : GameScene
}

enum class SkyWeather { CLEAR, RAIN, SUN }

enum class WildBiome {
    FIELD, SEA, MOUNTAIN, NIGHT, VOLCANO;

    companion object {
        fun forTypes(types: List<String>): WildBiome = when (types.firstOrNull()?.lowercase()) {
            "water", "ice" -> SEA
            "fire" -> VOLCANO
            "rock", "ground", "steel", "fighting", "dragon" -> MOUNTAIN
            "ghost", "dark", "psychic", "poison", "fairy" -> NIGHT
            else -> FIELD
        }
    }
}

object BackdropPulse {
    val Correct = Color(0xFF4CAF50)
    val Wrong = Color(0xFFFF5252)
    val Gold = Color(0xFFFFD54F)
}

@Composable
fun GameBackdrop(
    scene: GameScene,
    modifier: Modifier = Modifier,
    paused: Boolean = false,
    pulseKey: Any? = null,
    pulseColor: Color = BackdropPulse.Correct,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val motionEnabled = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

    // Seconds of scene time. Read only inside draw lambdas, so ticking it redraws
    // the backdrop layer without recomposing anything.
    val clock = remember { mutableFloatStateOf(0f) }
    if (motionEnabled && !paused) {
        LaunchedEffect(Unit) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    if (last != 0L) clock.floatValue += (now - last) / 1_000_000_000f
                    last = now
                }
            }
        }
    }

    val pulse = remember { Animatable(0f) }
    LaunchedEffect(pulseKey) {
        if (pulseKey == null) return@LaunchedEffect
        pulse.snapTo(0f)
        pulse.animateTo(1f, tween(120))
        pulse.animateTo(0f, tween(520))
    }

    Box(modifier.fillMaxSize()) {
        Crossfade(targetState = scene, animationSpec = tween(600), label = "backdrop") { s ->
            Canvas(Modifier.fillMaxSize().graphicsLayer()) {
                drawScene(s, clock.floatValue, accent)
                drawScrim()
            }
        }
        Canvas(Modifier.fillMaxSize().graphicsLayer()) {
            val a = pulse.value
            if (a > 0f) drawEdgeGlow(pulseColor, a)
        }
        content()
    }
}

// ─── Dispatch ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawScene(scene: GameScene, t: Float, accent: Color) = when (scene) {
    GameScene.Hub -> drawHub(t, accent)
    GameScene.Lab -> drawLab(t, accent)
    is GameScene.TypeArena -> drawTypeArena(t, scene.typeColor)
    GameScene.Spotlight -> drawSpotlight(t, accent)
    GameScene.TrainerTable -> drawTrainerTable(t, accent)
    GameScene.DuelArena -> drawDuelArena(t, accent)
    is GameScene.Weather -> drawWeather(t, accent, scene.sky)
    is GameScene.Wild -> drawWild(t, scene.biome)
    GameScene.Stadium -> drawStadium(t, accent)
}

// ─── Shared particles & helpers ───────────────────────────────────────────────

private class Mote(val x: Float, val y: Float, val speed: Float, val size: Float, val phase: Float)

private fun motes(count: Int, seed: Int): List<Mote> {
    val r = Random(seed)
    return List(count) {
        Mote(r.nextFloat(), r.nextFloat(), 0.01f + r.nextFloat() * 0.03f, 0.5f + r.nextFloat(), r.nextFloat() * 2f * PI.toFloat())
    }
}

private val Sparks = motes(18, 7)
private val Drops = motes(56, 11)
private val Stars = motes(40, 23)
private val Bokeh = motes(24, 31)
private val Blades = motes(44, 47)

private fun frac(v: Float) = v - floor(v)

/** 1 at the edges, dimmed over the centre where gameplay content sits. */
private fun zone(x: Float, y: Float): Float =
    if (x in 0.2f..0.8f && y in 0.2f..0.8f) 0.35f else 1f

private fun tint(c: Color, accent: Color, f: Float = 0.12f) = lerp(c, accent, f)

private fun twinkle(t: Float, m: Mote, rate: Float = 1.5f) = 0.4f + 0.6f * ((sin(t * rate + m.phase) + 1f) / 2f)

private fun DrawScope.vertical(colors: List<Color>) = drawRect(Brush.verticalGradient(colors))

private fun DrawScope.glow(color: Color, alpha: Float, cx: Float, cy: Float, radius: Float) {
    val c = Offset(size.width * cx, size.height * cy)
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), c, radius), radius, c)
}

private fun DrawScope.risingMotes(list: List<Mote>, t: Float, color: Color, maxAlpha: Float, radiusDp: Float, speedScale: Float = 1f) {
    val r = radiusDp.dp.toPx()
    list.forEach { m ->
        val y = frac(m.y - t * m.speed * speedScale)
        val x = m.x + sin(t * 0.5f + m.phase) * 0.015f
        val a = maxAlpha * twinkle(t, m) * zone(x, y)
        drawCircle(color.copy(alpha = a), r * m.size, Offset(x * size.width, y * size.height))
    }
}

private fun DrawScope.pokeballOutline(center: Offset, radius: Float, rotationDeg: Float, color: Color) {
    val stroke = 2.dp.toPx()
    rotate(rotationDeg, center) {
        drawCircle(color, radius, center, style = Stroke(stroke))
        val inner = radius * 0.22f
        drawLine(color, Offset(center.x - radius, center.y), Offset(center.x - inner, center.y), stroke)
        drawLine(color, Offset(center.x + inner, center.y), Offset(center.x + radius, center.y), stroke)
        drawCircle(color, inner, center, style = Stroke(stroke))
    }
}

private fun DrawScope.hexagon(center: Offset, radius: Float, rotationDeg: Float, color: Color) {
    val path = Path().apply {
        for (i in 0 until 6) {
            val a = (rotationDeg + 60f * i) * PI.toFloat() / 180f
            val p = Offset(center.x + radius * cos(a), center.y + radius * sin(a))
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    drawPath(path, color, style = Stroke(1.5.dp.toPx()))
}

private fun DrawScope.sparkle(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
}

/** Silhouette ridge along the bottom; [peaks] are (x, heightFraction) pairs. */
private fun DrawScope.ridge(baseY: Float, peaks: List<Pair<Float, Float>>, color: Color, smooth: Boolean) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(0f, h)
        lineTo(0f, h * baseY)
        var prev = Offset(0f, h * baseY)
        peaks.forEach { (px, py) ->
            val p = Offset(w * px, h * py)
            if (smooth) quadraticTo((prev.x + p.x) / 2f, prev.y, p.x, p.y) else lineTo(p.x, p.y)
            prev = p
        }
        lineTo(w, h * baseY)
        lineTo(w, h)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawScrim() {
    drawRect(
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.22f),
            0.16f to Color.Transparent,
            0.84f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.28f)
        )
    )
}

private fun DrawScope.drawEdgeGlow(color: Color, alpha: Float) {
    val c = Offset(size.width / 2f, size.height / 2f)
    drawRect(
        Brush.radialGradient(
            0.55f to Color.Transparent,
            1f to color.copy(alpha = 0.38f * alpha),
            center = c,
            radius = hypot(size.width, size.height) / 2f
        )
    )
}

// ─── Scenes ───────────────────────────────────────────────────────────────────

private fun DrawScope.drawHub(t: Float, accent: Color) {
    val w = size.width
    val h = size.height
    vertical(listOf(tint(Color(0xFF101A2E), accent), tint(Color(0xFF0A0F1C), accent, 0.06f)))
    glow(accent, 0.12f, 0.85f, 0.08f, w * 0.9f)
    glow(accent, 0.06f, 0.1f, 0.95f, w * 0.7f)
    pokeballOutline(Offset(w * 1.0f, h * 0.14f), w * 0.42f, t * 4f, Color.White.copy(alpha = 0.05f))
    pokeballOutline(Offset(0f, h * 0.9f), w * 0.34f, -t * 3f, Color.White.copy(alpha = 0.04f))
    risingMotes(Sparks, t, accent, 0.22f, 1.6f)
}

private val LabHexes = listOf(
    Triple(0.08f, 0.24f, 26f), Triple(0.93f, 0.34f, 20f), Triple(0.05f, 0.62f, 18f),
    Triple(0.94f, 0.76f, 28f), Triple(0.84f, 0.1f, 16f), Triple(0.16f, 0.9f, 22f)
)

private fun DrawScope.drawLab(t: Float, accent: Color) {
    val w = size.width
    val h = size.height
    val cyan = Color(0xFF4FC3F7)
    vertical(listOf(tint(Color(0xFF0B1A2A), accent), tint(Color(0xFF07111C), accent, 0.06f)))
    glow(cyan, 0.08f, 0.5f, 0.3f, w * 0.8f)

    // Blueprint grid
    val step = 28.dp.toPx()
    val gridColor = cyan.copy(alpha = 0.035f)
    var x = 0f
    while (x < w) { drawLine(gridColor, Offset(x, 0f), Offset(x, h), 1f); x += step }
    var y = 0f
    while (y < h) { drawLine(gridColor, Offset(0f, y), Offset(w, y), 1f); y += step }

    LabHexes.forEachIndexed { i, (hx, hy, r) ->
        val bob = sin(t * 0.6f + i) * 8.dp.toPx()
        hexagon(Offset(w * hx, h * hy + bob), r.dp.toPx(), t * 6f + i * 17f, cyan.copy(alpha = 0.08f))
    }
    risingMotes(Sparks, t, cyan, 0.2f, 1.4f)
}

private fun DrawScope.drawTypeArena(t: Float, typeColor: Color) {
    val w = size.width
    val h = size.height
    vertical(listOf(lerp(Color(0xFF0E1220), typeColor, 0.10f), Color(0xFF080A12)))
    glow(typeColor, 0.18f, 0.5f + 0.22f * cos(t * 0.25f), 0.2f + 0.05f * sin(t * 0.3f), w * 0.8f)
    glow(typeColor, 0.10f, 0.5f + 0.3f * sin(t * 0.2f), 0.95f, w * 0.7f)

    // Speed streaks confined to the side gutters
    val len = h * 0.07f
    Sparks.forEach { m ->
        val sx = if (m.x < 0.5f) m.x * 0.24f else 0.88f + (m.x - 0.5f) * 0.24f
        val sy = frac(m.y + t * m.speed * 6f) * h
        drawLine(
            Brush.verticalGradient(listOf(Color.Transparent, typeColor.copy(alpha = 0.14f)), sy, sy + len),
            Offset(sx * w, sy), Offset(sx * w, sy + len), 2.dp.toPx(), StrokeCap.Round
        )
    }
    risingMotes(Stars, t, typeColor, 0.2f, 1.3f)
}

private fun DrawScope.drawSpotlight(t: Float, accent: Color) {
    val w = size.width
    val h = size.height
    vertical(listOf(tint(Color(0xFF140D1E), accent, 0.08f), Color(0xFF07050C)))

    val sway = sin(t * 0.4f) * 0.04f
    val floorY = h * 0.62f
    val cone = Path().apply {
        moveTo(w * (0.46f + sway * 0.3f), -h * 0.04f)
        lineTo(w * (0.54f + sway * 0.3f), -h * 0.04f)
        lineTo(w * (0.84f + sway), floorY)
        lineTo(w * (0.16f + sway), floorY)
        close()
    }
    drawPath(cone, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.03f)), 0f, floorY))
    drawOval(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent), Offset(w * (0.5f + sway), floorY), w * 0.36f),
        topLeft = Offset(w * (0.14f + sway), floorY - h * 0.035f),
        size = Size(w * 0.72f, h * 0.07f)
    )

    // Side curtains
    val curtain = tint(Color(0xFF7A1C30), accent, 0.1f)
    val cw = w * 0.08f
    for (side in 0..1) {
        val folds = 4
        for (i in 0 until folds) {
            val fx = i * cw / folds + sin(t * 0.5f + i) * 2.dp.toPx()
            val left = if (side == 0) fx else w - fx - cw / folds
            drawRect(
                Brush.horizontalGradient(
                    listOf(curtain.copy(alpha = 0.55f), curtain.copy(alpha = 0.25f)),
                    left, left + cw / folds
                ),
                Offset(left, 0f), Size(cw / folds, h)
            )
        }
    }

    // Dust caught in the beam
    val r = 1.2.dp.toPx()
    Sparks.forEach { m ->
        val y = frac(m.y + t * m.speed * 0.5f) * 0.6f
        val x = 0.3f + m.x * 0.4f + sway
        drawCircle(Color.White.copy(alpha = 0.16f * twinkle(t, m)), r * m.size, Offset(x * w, y * h))
    }
}

private val TableCards = listOf(
    Triple(0.1f, 0.18f, -18f), Triple(0.9f, 0.26f, 14f), Triple(0.06f, 0.7f, 24f),
    Triple(0.92f, 0.82f, -12f), Triple(0.78f, 0.06f, 30f)
)

private fun DrawScope.drawTrainerTable(t: Float, accent: Color) {
    val w = size.width
    val h = size.height
    vertical(listOf(tint(Color(0xFF0F2A27), accent), tint(Color(0xFF081615), accent, 0.06f)))
    glow(Color(0xFF26A69A), 0.10f, 0.5f, 0.45f, w * 0.85f)

    // Felt diamond weave
    val step = 40.dp.toPx()
    val weave = Color.White.copy(alpha = 0.022f)
    var d = -h
    while (d < w + h) {
        drawLine(weave, Offset(d, 0f), Offset(d + h, h), 1f)
        drawLine(weave, Offset(d + h, 0f), Offset(d, h), 1f)
        d += step
    }

    val cardW = 56.dp.toPx()
    val cardH = 78.dp.toPx()
    TableCards.forEachIndexed { i, (cx, cy, rot) ->
        val c = Offset(w * cx, h * cy + sin(t * 0.5f + i) * 5.dp.toPx())
        rotate(rot + sin(t * 0.3f + i) * 3f, c) {
            drawRoundRect(
                Color.White.copy(alpha = 0.06f), Offset(c.x - cardW / 2, c.y - cardH / 2), Size(cardW, cardH),
                CornerRadius(8.dp.toPx()), style = Stroke(1.5.dp.toPx())
            )
            drawCircle(Color.White.copy(alpha = 0.05f), cardW * 0.18f, c, style = Stroke(1.5.dp.toPx()))
        }
    }

    val gold = Color(0xFFFFD54F)
    Sparks.forEach { m ->
        val a = 0.3f * ((sin(t * 1.2f + m.phase) + 1f) / 2f) * zone(m.x, m.y)
        sparkle(Offset(m.x * w, m.y * h), 4.dp.toPx() * m.size, gold.copy(alpha = a))
    }
}

private fun DrawScope.drawDuelArena(t: Float, accent: Color) {
    val w = size.width
    val h = size.height
    val rival = Color(0xFFE53935)
    vertical(listOf(tint(Color(0xFF121A2B), accent), Color(0xFF0A0E18)))
    glow(rival, 0.14f, 1f, 0f, w * 1.0f)
    glow(accent, 0.14f, 0f, 1f, w * 1.0f)

    // Diagonal seam with a travelling glint
    val start = Offset(0f, h * 0.72f)
    val end = Offset(w, h * 0.28f)
    drawLine(Color.White.copy(alpha = 0.06f), start, end, 1.5.dp.toPx())
    val g = frac(t * 0.12f)
    val gp = Offset(start.x + (end.x - start.x) * g, start.y + (end.y - start.y) * g)
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), gp, 26.dp.toPx()), 26.dp.toPx(), gp)

    // Battle platforms
    val ring = Color.White.copy(alpha = 0.06f)
    drawOval(ring, Offset(w * 0.5f, h * 0.27f), Size(w * 0.48f, h * 0.06f), style = Stroke(1.5.dp.toPx()))
    drawOval(ring, Offset(w * 0.02f, h * 0.75f), Size(w * 0.52f, h * 0.065f), style = Stroke(1.5.dp.toPx()))
    risingMotes(Sparks, t, Color.White, 0.16f, 1.3f)
}

private fun DrawScope.drawWeather(t: Float, accent: Color, sky: SkyWeather) {
    val w = size.width
    val h = size.height
    when (sky) {
        SkyWeather.CLEAR -> {
            vertical(listOf(Color(0xFF1A1206), Color(0xFF130D1E), Color(0xFF0C0A14)))
            glow(Color(0xFFFFB300), 0.10f, 0.5f, 1.0f, w * 0.9f)
            ridge(0.9f, listOf(0.15f to 0.86f, 0.3f to 0.9f, 0.5f to 0.84f, 0.72f to 0.89f, 0.9f to 0.85f), Color(0xFF07050C).copy(alpha = 0.7f), false)
            risingMotes(Sparks, t, Color(0xFFFFB74D), 0.24f, 1.5f)
        }
        SkyWeather.RAIN -> {
            vertical(listOf(Color(0xFF0E1724), Color(0xFF0A1019), Color(0xFF06090F)))
            glow(Color(0xFF64B5F6), 0.07f, 0.5f, 0.1f, w)
            val len = h * 0.035f
            val slant = w * 0.012f
            val stroke = 1.2.dp.toPx()
            Drops.forEach { m ->
                val y = frac(m.y + t * (0.5f + m.speed * 10f))
                val a = 0.2f * zone(m.x, y)
                drawLine(Color(0xFF9EC9FF).copy(alpha = a), Offset(m.x * w, y * h), Offset(m.x * w - slant, y * h + len), stroke, StrokeCap.Round)
            }
            // Occasional distant lightning
            val f = frac(t / 9f)
            if (f < 0.025f) drawRect(Color.White.copy(alpha = 0.06f * (1f - f / 0.025f)))
        }
        SkyWeather.SUN -> {
            vertical(listOf(Color(0xFF2B1707), Color(0xFF1C0F08), Color(0xFF110A08)))
            val sun = Offset(w * 1.02f, -h * 0.03f)
            glow(Color(0xFFFFC107), 0.26f, 1.02f, -0.03f, w * 0.7f)
            val rayLen = hypot(w, h)
            rotate(t * 2f, sun) {
                for (i in 0 until 9) {
                    val a0 = (95f + i * 10f) * PI.toFloat() / 180f
                    val a1 = a0 + 3.5f * PI.toFloat() / 180f
                    val ray = Path().apply {
                        moveTo(sun.x, sun.y)
                        lineTo(sun.x + rayLen * cos(a0), sun.y + rayLen * sin(a0))
                        lineTo(sun.x + rayLen * cos(a1), sun.y + rayLen * sin(a1))
                        close()
                    }
                    drawPath(ray, Brush.radialGradient(listOf(Color(0xFFFFC107).copy(alpha = 0.07f), Color.Transparent), sun, rayLen * 0.8f))
                }
            }
            risingMotes(Sparks, t, Color(0xFFFF8A65), 0.22f, 1.5f, speedScale = 1.6f)
        }
    }
    glow(accent, 0.05f, 0.5f, 0.5f, w * 0.6f)
}

private fun DrawScope.drawWild(t: Float, biome: WildBiome) {
    val w = size.width
    val h = size.height
    when (biome) {
        WildBiome.FIELD -> {
            vertical(listOf(Color(0xFF0D1B2A), Color(0xFF162033), Color(0xFF1A2E1A), Color(0xFF0D2010)))
            ridge(0.84f, listOf(0.25f to 0.8f, 0.55f to 0.86f, 0.8f to 0.79f, 1f to 0.83f), Color(0xFF14301A).copy(alpha = 0.8f), true)
            ridge(0.9f, listOf(0.2f to 0.92f, 0.5f to 0.87f, 0.85f to 0.92f, 1f to 0.9f), Color(0xFF0B2010), true)
            // Tall grass swaying along the bottom edge
            val bladeColor = Color(0xFF2E7D32).copy(alpha = 0.55f)
            val stroke = 2.dp.toPx()
            Blades.forEachIndexed { i, m ->
                val bx = (i + m.x) / Blades.size * w
                val bh = h * (0.03f + m.size * 0.025f)
                val tip = Offset(bx + sin(t * 1.2f + m.phase) * 6.dp.toPx(), h - bh)
                val p = Path().apply { moveTo(bx, h); quadraticTo(bx, h - bh * 0.5f, tip.x, tip.y) }
                drawPath(p, bladeColor, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            val firefly = Color(0xFFDCE775)
            Sparks.forEach { m ->
                val x = m.x + sin(t * 0.4f + m.phase) * 0.03f
                val y = 0.35f + m.y * 0.5f + cos(t * 0.3f + m.phase) * 0.02f
                val a = 0.35f * ((sin(t * 2f + m.phase) + 1f) / 2f) * zone(x, y)
                glow(firefly, a, x, y, 5.dp.toPx() * m.size)
            }
        }
        WildBiome.SEA -> {
            vertical(listOf(Color(0xFF0A1A2E), Color(0xFF0B2338), Color(0xFF07192A)))
            glow(Color(0xFF4FC3F7), 0.08f, 0.5f, 0.05f, w)
            for (layer in 0..2) {
                val base = h * (0.84f + layer * 0.05f)
                val amp = (5 + layer * 2).dp.toPx()
                val path = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w) {
                        lineTo(x, base + sin(x / w * 2f * PI.toFloat() * 1.5f + t * (0.6f + layer * 0.2f) + layer) * amp)
                        x += 8.dp.toPx()
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(path, Color(0xFF1565C0).copy(alpha = 0.18f + layer * 0.1f))
            }
            risingMotes(Stars, t, Color(0xFFB3E5FC), 0.22f, 2f, speedScale = 0.8f)
        }
        WildBiome.MOUNTAIN -> {
            vertical(listOf(Color(0xFF1E1914), Color(0xFF15110E), Color(0xFF0E0B09)))
            glow(Color(0xFFFFCC80), 0.06f, 0.2f, 0.1f, w)
            ridge(0.82f, listOf(0.12f to 0.7f, 0.25f to 0.8f, 0.42f to 0.66f, 0.6f to 0.79f, 0.78f to 0.68f, 0.92f to 0.78f), Color(0xFF2A221B).copy(alpha = 0.75f), false)
            ridge(0.9f, listOf(0.18f to 0.84f, 0.35f to 0.9f, 0.55f to 0.82f, 0.75f to 0.9f, 0.9f to 0.85f), Color(0xFF110D0A), false)
            val r = 1.4.dp.toPx()
            Sparks.forEach { m ->
                val x = frac(m.x + t * m.speed * 0.8f)
                val y = m.y + sin(t * 0.5f + m.phase) * 0.01f
                drawCircle(Color(0xFFD7CCC8).copy(alpha = 0.16f * zone(x, y)), r * m.size, Offset(x * w, y * h))
            }
        }
        WildBiome.NIGHT -> {
            vertical(listOf(Color(0xFF0B0820), Color(0xFF120B26), Color(0xFF0A0614)))
            val r = 1.2.dp.toPx()
            Stars.forEach { m ->
                val y = m.y * 0.7f
                drawCircle(Color.White.copy(alpha = 0.45f * twinkle(t, m, 0.9f) * zone(m.x, y)), r * m.size, Offset(m.x * w, y * h))
            }
            val moon = Offset(w * 0.84f, h * 0.1f)
            val mr = 22.dp.toPx()
            glow(Color(0xFFE1BEE7), 0.14f, 0.84f, 0.1f, mr * 4f)
            drawCircle(Color(0xFFF3E5F5).copy(alpha = 0.55f), mr, moon)
            drawCircle(Color(0xFF0D0922), mr * 0.9f, Offset(moon.x - mr * 0.45f, moon.y - mr * 0.15f))
            val drift = sin(t * 0.15f) * w * 0.05f
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF7E57C2).copy(alpha = 0.12f)), h * 0.78f, h),
                Offset(drift - w * 0.05f, h * 0.78f), Size(w * 1.1f, h * 0.22f)
            )
        }
        WildBiome.VOLCANO -> {
            vertical(listOf(Color(0xFF1A0A0A), Color(0xFF210C08), Color(0xFF2B0F06)))
            glow(Color(0xFFFF5722), 0.22f + 0.05f * sin(t * 0.8f), 0.5f, 1.05f, w * 0.9f)
            ridge(0.88f, listOf(0.2f to 0.84f, 0.38f to 0.74f, 0.46f to 0.73f, 0.62f to 0.86f, 0.85f to 0.82f), Color(0xFF120605).copy(alpha = 0.85f), false)
            risingMotes(Stars, t, Color(0xFFFF7043), 0.3f, 1.5f, speedScale = 1.4f)
        }
    }
}

private val BokehColors = listOf(Color(0xFFE53935), Color(0xFF42A5F5), Color(0xFFFFD54F), Color.White)

private fun DrawScope.drawStadium(t: Float, accent: Color) {
    val w = size.width
    val h = size.height
    vertical(listOf(Color(0xFF0A0D16), tint(Color(0xFF0B101C), accent, 0.06f), Color(0xFF07090F)))
    glow(Color(0xFF1B3A9C), 0.24f, 0.2f, 0.18f, w * 0.75f)
    glow(Color(0xFF9C1B1B), 0.2f, 0.8f, 0.82f, w * 0.65f)

    // Crossing floodlights from the top corners
    val beamLen = h * 0.9f
    for (side in 0..1) {
        val origin = Offset(if (side == 0) 0f else w, 0f)
        val angle = (if (side == 0) 58f else 122f) + sin(t * 0.35f + side * 2f) * 6f
        rotate(angle - 90f, origin) {
            val beam = Path().apply {
                moveTo(origin.x, origin.y)
                lineTo(origin.x - w * 0.12f, origin.y + beamLen)
                lineTo(origin.x + w * 0.12f, origin.y + beamLen)
                close()
            }
            drawPath(beam, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent), origin.y, origin.y + beamLen))
        }
    }

    // Crowd bokeh along the top band and side edges
    Bokeh.forEachIndexed { i, m ->
        val onTop = i % 2 == 0
        val x = if (onTop) m.x else if (m.x < 0.5f) m.x * 0.12f else 0.88f + (m.x - 0.5f) * 0.24f
        val y = if (onTop) m.y * 0.12f else 0.15f + m.y * 0.7f
        val c = BokehColors[i % BokehColors.size]
        drawCircle(c.copy(alpha = 0.16f * twinkle(t, m, 0.8f)), (4f + m.size * 5f).dp.toPx(), Offset(x * w, y * h))
    }

    // Midfield line and centre ring
    drawLine(
        Brush.horizontalGradient(0f to Color.Transparent, 0.2f to Color.White.copy(alpha = 0.05f), 0.8f to Color.White.copy(alpha = 0.05f), 1f to Color.Transparent),
        Offset(0f, h * 0.5f), Offset(w, h * 0.5f), 1.dp.toPx()
    )
    translate(w / 2f, h / 2f) {
        drawCircle(Color.White.copy(alpha = 0.04f), w * 0.14f, Offset.Zero, style = Stroke(1.dp.toPx()))
    }
}
