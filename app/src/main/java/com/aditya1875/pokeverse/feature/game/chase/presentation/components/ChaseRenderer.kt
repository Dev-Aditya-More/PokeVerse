package com.aditya1875.pokeverse.feature.game.chase.presentation.components

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import com.aditya1875.pokeverse.feature.game.chase.data.ChaseSprite
import com.aditya1875.pokeverse.feature.game.chase.data.ChaseSprites
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseConfig
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseEntity
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseWorld
import com.aditya1875.pokeverse.feature.game.chase.domain.model.EntityKind
import com.aditya1875.pokeverse.feature.game.chase.domain.model.NetWarning
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private val GrassLight = Color(0xFF6FBF4A)
private val GrassDark = Color(0xFF4E9A33)
private val PathColor = Color(0xFFD9B77A)
private val PathEdge = Color(0xFFB8925A)
private val LaneDash = Color(0xFFF3E2BC)
private val RocketRed = Color(0xFFE53935)
private val NetColor = Color(0xFFECEFF1)
private val ThunderYellow = Color(0xFFFFEB3B)

/**
 * Draws one [ChaseWorld] frame. The world is read inside the draw lambda, so a
 * new frame only re-runs drawing — never composition.
 */
@Composable
fun ChaseTrack(
    world: State<ChaseWorld>,
    sprites: ChaseSprites?,
    config: ChaseConfig,
    modifier: Modifier = Modifier
) {
    // Canvas doesn't clip by default — without this the balloon spills under the system nav bar.
    Canvas(modifier = modifier.clipToBounds()) {
        val w = world.value
        val track = TrackGeometry(size, config)

        drawGround(track, w, config)
        w.entities.forEach { drawEntity(track, it, w.elapsedMs, sprites) }
        w.warnings.forEach { drawNetWarning(track, it) }
        drawPikachu(track, w, sprites)
        drawRocketBalloon(track, w, sprites)
        drawHitVignette(w, config)
        drawThunder(track, w, config)
    }
}

/** Maps world units (lanes, screen fractions) to pixels. */
private class TrackGeometry(val size: Size, val config: ChaseConfig) {
    val pathLeft = size.width * 0.07f
    val pathWidth = size.width - pathLeft * 2
    val laneWidth = pathWidth / config.laneCount
    val spriteSize = laneWidth * 0.78f

    fun laneX(lane: Float) = pathLeft + (lane + 0.5f) * laneWidth
    fun y(fraction: Float) = fraction * size.height
}

// ── Ground ──────────────────────────────────────────────────────────────────

private fun DrawScope.drawGround(track: TrackGeometry, world: ChaseWorld, config: ChaseConfig) {
    drawRect(Brush.horizontalGradient(listOf(GrassDark, GrassLight, GrassDark)))

    drawRect(PathColor, topLeft = Offset(track.pathLeft, 0f), size = Size(track.pathWidth, size.height))
    val edge = track.laneWidth * 0.06f
    drawRect(PathEdge, topLeft = Offset(track.pathLeft, 0f), size = Size(edge, size.height))
    drawRect(PathEdge, topLeft = Offset(track.pathLeft + track.pathWidth - edge, 0f), size = Size(edge, size.height))

    // Dashes and grass tufts scroll with distance run, which is what sells the speed.
    val scrolledPx = world.meters / config.metersPerScreen * size.height
    val dashLength = size.height * 0.06f
    val dashPeriod = dashLength * 2.2f
    val dashOffset = scrolledPx % dashPeriod
    for (divider in 1 until config.laneCount) {
        val x = track.pathLeft + divider * track.laneWidth
        var y = -dashPeriod + dashOffset
        while (y < size.height) {
            drawLine(LaneDash, Offset(x, y), Offset(x, y + dashLength), strokeWidth = edge * 0.8f, cap = StrokeCap.Round)
            y += dashPeriod
        }
    }

    val tuftPeriod = size.height * 0.18f
    val tuftOffset = scrolledPx % tuftPeriod
    var y = -tuftPeriod + tuftOffset
    var index = 0
    while (y < size.height + tuftPeriod) {
        val x = if (index % 2 == 0) track.pathLeft * 0.45f else size.width - track.pathLeft * 0.45f
        drawCircle(GrassDark.copy(alpha = 0.8f), radius = track.pathLeft * 0.28f, center = Offset(x, y))
        y += tuftPeriod
        index++
    }
}

// ── Entities ────────────────────────────────────────────────────────────────

private fun DrawScope.drawEntity(track: TrackGeometry, entity: ChaseEntity, elapsedMs: Long, sprites: ChaseSprites?) {
    var x = track.laneX(entity.lane.toFloat())
    val y = track.y(entity.y)

    when (entity.kind) {
        EntityKind.LANDED_NET -> drawNet(Offset(x, y), track.spriteSize * 0.5f, alpha = 0.9f)
        EntityKind.ORAN_BERRY, EntityKind.THUNDER_STONE -> {
            // Pickups bob so they read as "grab me" rather than "avoid me".
            val bob = sin(elapsedMs / 180f + entity.id) * track.spriteSize * 0.05f
            val center = Offset(x, y + bob)
            drawCircle(Color.White.copy(alpha = 0.35f), radius = track.spriteSize * 0.32f, center = center)
            drawSpriteOr(sprites?.get(entity.kind.sprite()), center, track.spriteSize * 0.5f, fallback = entity.kind.fallbackColor())
        }
        else -> {
            if (entity.kind == EntityKind.KOFFING) {
                x += sin(elapsedMs / 300f + entity.id) * track.laneWidth * 0.06f
            }
            drawShadow(Offset(x, y + track.spriteSize * 0.36f), track.spriteSize * 0.32f)
            drawSpriteOr(sprites?.get(entity.kind.sprite()), Offset(x, y), track.spriteSize, fallback = entity.kind.fallbackColor())
        }
    }
}

private fun DrawScope.drawNetWarning(track: TrackGeometry, warning: NetWarning) {
    val center = Offset(track.laneX(warning.lane.toFloat()), track.y(track.config.playerY))
    val radius = track.spriteSize * 0.55f
    val pulse = 0.55f + 0.45f * sin(warning.progress * 18f)

    drawCircle(RocketRed.copy(alpha = 0.18f + 0.12f * pulse), radius = radius, center = center)
    drawCircle(RocketRed.copy(alpha = pulse), radius = radius, center = center, style = Stroke(width = radius * 0.08f))
    drawArc(
        color = RocketRed,
        startAngle = -90f,
        sweepAngle = 360f * warning.progress,
        useCenter = false,
        topLeft = Offset(center.x - radius * 1.18f, center.y - radius * 1.18f),
        size = Size(radius * 2.36f, radius * 2.36f),
        style = Stroke(width = radius * 0.12f, cap = StrokeCap.Round)
    )
    // The net's shadow grows as it falls.
    drawNet(center, radius * 0.25f + radius * 0.6f * warning.progress, alpha = 0.25f + 0.5f * warning.progress)
}

// ── Pikachu & Team Rocket ───────────────────────────────────────────────────

private fun DrawScope.drawPikachu(track: TrackGeometry, world: ChaseWorld, sprites: ChaseSprites?) {
    val blinkHidden = world.invulnerableMs > 0 && (world.invulnerableMs / 90) % 2 == 0L
    val x = track.laneX(world.playerX)
    val y = track.y(track.config.playerY)
    val bob = sin(world.elapsedMs / 70f) * track.spriteSize * 0.04f

    drawShadow(Offset(x, y + track.spriteSize * 0.4f), track.spriteSize * 0.3f)
    if (blinkHidden) return
    // Lean into lane switches.
    val lean = (world.targetLane - world.playerX) * 14f
    rotate(lean, pivot = Offset(x, y)) {
        drawSpriteOr(sprites?.get(ChaseSprite.PIKACHU_BACK), Offset(x, y + bob), track.spriteSize * 1.05f, fallback = ThunderYellow)
    }
}

/**
 * Team Rocket's Meowth balloon chasing from behind. It rises from below the
 * screen edge with every hit, so the danger is visible at a glance.
 */
private fun DrawScope.drawRocketBalloon(track: TrackGeometry, world: ChaseWorld, sprites: ChaseSprites?) {
    val radius = track.laneWidth * 0.95f
    // At full lives just Meowth's ears and the "R" peek over the bottom edge.
    val restY = size.height + radius * 0.55f
    val closeY = track.y(track.config.playerY) + track.spriteSize * 1.15f
    val y = restY + (closeY - restY) * world.rocketCloseness + sin(world.elapsedMs / 400f) * radius * 0.05f
    val followX = track.laneX(world.playerX) * 0.6f + size.width / 2 * 0.4f
    val center = Offset(followX, y)

    drawCircle(Color(0xFFF5E6C8), radius = radius, center = center)
    drawCircle(Color(0xFFBFA77A), radius = radius, center = center, style = Stroke(width = radius * 0.05f))
    drawSpriteOr(sprites?.get(ChaseSprite.MEOWTH), center, radius * 1.5f, fallback = Color(0xFFFFE0B2))
    // Team Rocket's red "R" emblem on the balloon.
    val badge = Offset(center.x + radius * 0.62f, center.y - radius * 0.55f)
    val badgeRadius = radius * 0.2f
    drawCircle(Color.White, radius = badgeRadius, center = badge)
    drawCircle(RocketRed, radius = badgeRadius, center = badge, style = Stroke(width = radius * 0.05f))
    drawIntoCanvas {
        RocketLetterPaint.textSize = badgeRadius * 1.3f
        it.nativeCanvas.drawText("R", badge.x, badge.y + RocketLetterPaint.textSize * 0.36f, RocketLetterPaint)
    }
}

private val RocketLetterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(0xE5, 0x39, 0x35)
    textAlign = Paint.Align.CENTER
    typeface = Typeface.DEFAULT_BOLD
}

// ── Effects ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawHitVignette(world: ChaseWorld, config: ChaseConfig) {
    // Only for regular hits (a revive grants a longer grace period, with no hit to flash for).
    if (world.invulnerableMs == 0L || world.invulnerableMs > config.invulnerableMs) return
    val fresh = ((world.invulnerableMs - (config.invulnerableMs - 350)) / 350f).coerceIn(0f, 1f)
    if (fresh == 0f) return
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color.Transparent, RocketRed.copy(alpha = 0.55f * fresh)),
            center = center,
            radius = size.maxDimension * 0.7f
        )
    )
}

private fun DrawScope.drawThunder(track: TrackGeometry, world: ChaseWorld, config: ChaseConfig) {
    if (world.thunderFlashMs == 0L) return
    val strength = world.thunderFlashMs / config.thunderFlashMs.toFloat()
    drawRect(ThunderYellow.copy(alpha = 0.35f * strength))

    val segments = 9
    val step = size.height / segments
    for (lane in 0 until config.laneCount) {
        val baseX = track.laneX(lane.toFloat())
        val bolt = Path().apply {
            moveTo(baseX, 0f)
            for (i in 1..segments) {
                val jitter = if (i % 2 == 0) 1f else -1f
                lineTo(baseX + jitter * track.laneWidth * 0.14f * ((lane + i) % 3 + 1) / 3f, i * step)
            }
        }
        drawPath(bolt, ThunderYellow.copy(alpha = strength), style = Stroke(width = track.laneWidth * 0.07f, cap = StrokeCap.Round))
        drawPath(bolt, Color.White.copy(alpha = strength), style = Stroke(width = track.laneWidth * 0.025f, cap = StrokeCap.Round))
    }
}

// ── Primitives ──────────────────────────────────────────────────────────────

private fun DrawScope.drawShadow(center: Offset, halfWidth: Float) {
    drawOval(
        Color.Black.copy(alpha = 0.18f),
        topLeft = Offset(center.x - halfWidth, center.y - halfWidth * 0.3f),
        size = Size(halfWidth * 2, halfWidth * 0.6f)
    )
}

private fun DrawScope.drawNet(center: Offset, radius: Float, alpha: Float) {
    val color = NetColor.copy(alpha = alpha)
    val stroke = radius * 0.08f
    drawCircle(color, radius = radius, center = center, style = Stroke(width = stroke * 1.5f))
    val lines = 3
    for (i in -lines..lines) {
        val offset = i * radius / (lines + 1)
        val half = sqrt((radius * radius - offset * offset).coerceAtLeast(0f))
        drawLine(color, Offset(center.x + offset, center.y - half), Offset(center.x + offset, center.y + half), stroke)
        drawLine(color, Offset(center.x - half, center.y + offset), Offset(center.x + half, center.y + offset), stroke)
    }
}

/** Draws [drawable] fitted into a [boxSize] square around [center], or a plain dot if it never loaded. */
private fun DrawScope.drawSpriteOr(drawable: Drawable?, center: Offset, boxSize: Float, fallback: Color) {
    if (drawable == null) {
        drawCircle(fallback, radius = boxSize * 0.32f, center = center)
        return
    }
    val intrinsicW = drawable.intrinsicWidth.coerceAtLeast(1)
    val intrinsicH = drawable.intrinsicHeight.coerceAtLeast(1)
    val scale = boxSize / max(intrinsicW, intrinsicH)
    val halfW = intrinsicW * scale / 2
    val halfH = intrinsicH * scale / 2
    drawable.setBounds(
        (center.x - halfW).toInt(), (center.y - halfH).toInt(),
        (center.x + halfW).toInt(), (center.y + halfH).toInt()
    )
    drawIntoCanvas { drawable.draw(it.nativeCanvas) }
}

private fun EntityKind.sprite(): ChaseSprite = when (this) {
    EntityKind.KOFFING -> ChaseSprite.KOFFING
    EntityKind.EKANS -> ChaseSprite.EKANS
    EntityKind.WOBBUFFET -> ChaseSprite.WOBBUFFET
    EntityKind.ORAN_BERRY -> ChaseSprite.ORAN_BERRY
    EntityKind.THUNDER_STONE -> ChaseSprite.THUNDER_STONE
    EntityKind.LANDED_NET -> error("Nets are drawn, not sprited")
}

private fun EntityKind.fallbackColor(): Color = when (this) {
    EntityKind.KOFFING -> Color(0xFF7E57C2)
    EntityKind.EKANS -> Color(0xFF8E24AA)
    EntityKind.WOBBUFFET -> Color(0xFF42A5F5)
    EntityKind.ORAN_BERRY -> Color(0xFF1E88E5)
    EntityKind.THUNDER_STONE -> Color(0xFF66BB6A)
    EntityKind.LANDED_NET -> NetColor
}
