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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import com.aditya1875.pokeverse.feature.game.chase.data.ChaseSprite
import com.aditya1875.pokeverse.feature.game.chase.data.ChaseSprites
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseConfig
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseEntity
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseWorld
import com.aditya1875.pokeverse.feature.game.chase.domain.model.EntityKind
import com.aditya1875.pokeverse.feature.game.chase.domain.model.NetWarning
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private val RocketRed = Color(0xFFE53935)
private val NetColor = Color(0xFFECEFF1)
private val ThunderYellow = Color(0xFFFFEB3B)

/**
 * Draws one [ChaseWorld] frame. The world is read inside the draw lambda, so a
 * new frame only re-runs drawing — never composition.
 */
@Composable
internal fun ChaseTrack(
    world: State<ChaseWorld>,
    sprites: ChaseSprites?,
    config: ChaseConfig,
    fx: ChaseFx? = null,
    modifier: Modifier = Modifier
) {
    // Canvas doesn't clip by default — without this the balloon spills under the system nav bar.
    Canvas(modifier = modifier.clipToBounds()) {
        val track = TrackGeometry(size, config)
        if (!track.isUsable) return@Canvas
        val w = world.value
        val palette = ChaseBiome.paletteAt(w.meters)
        val (shakeX, shakeY) = shakeOffset(w, config, track)

        translate(shakeX, shakeY) {
            drawGround(track, w, config, palette)
            drawScenery(track, w, config, palette)
            drawSpeedStreaks(track, w, config, palette)
            w.entities.forEach { drawEntity(track, it, w.elapsedMs, sprites, palette) }
            w.warnings.forEach { drawNetWarning(track, it) }
            drawHaze(palette)
            drawSpeedLines(track, w)
            drawPikachu(track, w, sprites, palette)
            drawRocketBalloon(track, w, sprites)
            fx?.let { drawBursts(track, w, it) }
            drawStrikes(track, w, config)
        }
        drawHitVignette(w, config)
        drawThunder(track, w, config)
    }
}

/** A short, decaying shake on hits and Thunderbolts — the camera moves, not the world. */
private fun shakeOffset(world: ChaseWorld, config: ChaseConfig, track: TrackGeometry): Pair<Float, Float> {
    val hit = if (world.invulnerableMs in 1..config.invulnerableMs)
        ((world.invulnerableMs - (config.invulnerableMs - 300)) / 300f).coerceIn(0f, 1f) else 0f
    val thunder = (world.thunderFlashMs / config.thunderFlashMs.toFloat()).coerceIn(0f, 1f) * 0.6f
    val amount = max(hit, thunder)
    if (amount == 0f) return 0f to 0f
    val amp = track.laneWidth * 0.06f * amount
    return sin(world.elapsedMs * 0.11f) * amp to cos(world.elapsedMs * 0.13f) * amp * 0.6f
}

// ── Ground ──────────────────────────────────────────────────────────────────

private fun DrawScope.drawGround(track: TrackGeometry, world: ChaseWorld, config: ChaseConfig, p: ChasePalette) {
    val scrolledPx = world.meters / config.metersPerScreen * size.height

    // Field: lighter toward the player for depth, with mowed stripes that scroll.
    drawRect(Brush.verticalGradient(listOf(p.fieldTop, p.fieldBottom)))
    val stripe = size.height * 0.09f
    var sy = -stripe * 2 + scrolledPx % (stripe * 2)
    while (sy < size.height) {
        drawRect(Color.White.copy(alpha = 0.035f * (1f - p.night)), Offset(0f, sy), Size(size.width, stripe))
        sy += stripe * 2
    }

    // Path: shaded far to near, with soft inner shadows along both kerbs.
    drawRect(
        Brush.verticalGradient(listOf(p.pathShade, p.path)),
        topLeft = Offset(track.pathLeft, 0f), size = Size(track.pathWidth, size.height)
    )
    val inner = track.laneWidth * 0.16f
    val right = track.pathLeft + track.pathWidth
    drawRect(
        Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent), track.pathLeft, track.pathLeft + inner),
        topLeft = Offset(track.pathLeft, 0f), size = Size(inner, size.height)
    )
    drawRect(
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)), right - inner, right),
        topLeft = Offset(right - inner, 0f), size = Size(inner, size.height)
    )

    // Gravel speckle scrolling with the path.
    val speckle = size.height * 0.11f
    var gy = -speckle + scrolledPx % speckle
    var row = 0
    while (gy < size.height) {
        for (k in 0 until 5) {
            val fx = ((row * 37 + k * 53) % 97) / 97f
            drawCircle(
                Color.Black.copy(alpha = 0.06f),
                radius = track.laneWidth * 0.012f,
                center = Offset(track.pathLeft + fx * track.pathWidth, gy + (k * 0.19f) * speckle)
            )
        }
        gy += speckle
        row++
    }

    // Kerbs: a solid edge by day, a neon tube at night.
    val edge = track.laneWidth * 0.045f
    if (p.night > 0.3f) {
        val glow = p.edge.copy(alpha = 0.28f * p.night)
        drawLine(glow, Offset(track.pathLeft, 0f), Offset(track.pathLeft, size.height), edge * 4f)
        drawLine(glow, Offset(right, 0f), Offset(right, size.height), edge * 4f)
    }
    drawLine(p.edge, Offset(track.pathLeft, 0f), Offset(track.pathLeft, size.height), edge)
    drawLine(p.edge, Offset(right, 0f), Offset(right, size.height), edge)

    // Lane dashes scroll with distance run, which is what sells the speed.
    val dashLength = size.height * 0.06f
    val dashPeriod = dashLength * 2.2f
    val dashOffset = scrolledPx % dashPeriod
    for (divider in 1 until config.laneCount) {
        val x = track.pathLeft + divider * track.laneWidth
        var y = -dashPeriod + dashOffset
        while (y < size.height) {
            if (p.night > 0.3f) {
                drawLine(p.laneLine.copy(alpha = 0.25f * p.night), Offset(x, y), Offset(x, y + dashLength), edge * 2.6f, StrokeCap.Round)
            }
            drawLine(p.laneLine.copy(alpha = 0.85f), Offset(x, y), Offset(x, y + dashLength), edge * 0.7f, StrokeCap.Round)
            y += dashPeriod
        }
    }
}

/**
 * Parallax roadside: trees by day (two layers at different speeds), lamp posts
 * casting pools of light at night. They only overhang the kerb, never a lane centre.
 */
private fun DrawScope.drawScenery(track: TrackGeometry, world: ChaseWorld, config: ChaseConfig, p: ChasePalette) {
    val scrolledPx = world.meters / config.metersPerScreen * size.height
    val margin = track.pathLeft
    val right = track.pathLeft + track.pathWidth

    // Far layer: slower, darker, smaller.
    canopyRow(scrolledPx * 0.6f, size.height * 0.23f, margin * 0.75f, p.foliageDark.copy(alpha = 0.9f), margin * 0.35f, right + margin * 0.65f, phase = 0.5f)
    // Near layer: full speed.
    canopyRow(scrolledPx, size.height * 0.31f, margin * 1.05f, p.foliage, margin * 0.55f, right + margin * 0.45f, phase = 0f)

    // Night: lamp posts with light pooling onto the path.
    if (p.night > 0.05f) {
        val period = size.height * 0.42f
        var y = -period + scrolledPx % period
        var i = 0
        while (y < size.height + period) {
            val left = i % 2 == 0
            val x = if (left) track.pathLeft - margin * 0.1f else right + margin * 0.1f
            val lamp = if (left) p.edge else p.laneLine
            val pool = track.laneWidth * 0.9f
            drawCircle(
                Brush.radialGradient(listOf(lamp.copy(alpha = 0.22f * p.night), Color.Transparent), Offset(x, y), pool),
                radius = pool, center = Offset(x, y)
            )
            drawCircle(lamp.copy(alpha = p.night), radius = margin * 0.16f, center = Offset(x, y))
            drawCircle(Color.White.copy(alpha = 0.8f * p.night), radius = margin * 0.07f, center = Offset(x, y))
            y += period
            i++
        }
    }
}

private fun DrawScope.canopyRow(scrolled: Float, period: Float, r: Float, color: Color, leftX: Float, rightX: Float, phase: Float) {
    var y = -period + (scrolled + period * phase) % period
    var i = 0
    while (y < size.height + period) {
        val x = if (i % 2 == 0) leftX else rightX
        // Three overlapping blobs plus a highlight read as a tree, not a dot.
        drawCircle(Color.Black.copy(alpha = 0.18f), radius = r, center = Offset(x + r * 0.2f, y + r * 0.35f))
        drawCircle(color, radius = r, center = Offset(x, y))
        drawCircle(color, radius = r * 0.75f, center = Offset(x - r * 0.55f, y + r * 0.35f))
        drawCircle(color, radius = r * 0.7f, center = Offset(x + r * 0.5f, y + r * 0.4f))
        drawCircle(Color.White.copy(alpha = 0.08f), radius = r * 0.45f, center = Offset(x - r * 0.25f, y - r * 0.3f))
        y += period
        i++
    }
}

/** Distant haze at the top: obstacles emerge out of it instead of popping in. */
private fun DrawScope.drawHaze(p: ChasePalette) {
    drawRect(
        Brush.verticalGradient(
            0f to p.haze.copy(alpha = 0.55f),
            0.22f to p.haze.copy(alpha = 0f),
            0.85f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.25f)
        )
    )
}

/** Faint streaks in the margins that build up as the run speeds up. */
private fun DrawScope.drawSpeedStreaks(track: TrackGeometry, world: ChaseWorld, config: ChaseConfig, p: ChasePalette) {
    val intensity = ((world.speed - config.startSpeed) / (config.maxSpeed - config.startSpeed)).coerceIn(0f, 1f)
    if (intensity < 0.15f) return
    val count = 8
    val travel = (world.elapsedMs % 600) / 600f
    for (i in 0 until count) {
        val left = i % 2 == 0
        val seed = ((i * 7919) % 89) / 89f
        val x = if (left) track.pathLeft * seed * 0.8f else size.width - track.pathLeft * seed * 0.8f
        val length = size.height * (0.05f + 0.05f * seed) * intensity
        val y = ((travel + i / count.toFloat()) % 1f) * (size.height + length) - length
        drawLine(
            p.laneLine.copy(alpha = 0.18f * intensity),
            Offset(x, y), Offset(x, y + length),
            strokeWidth = track.laneWidth * 0.012f, cap = StrokeCap.Round
        )
    }
}

// ── Entities ────────────────────────────────────────────────────────────────

private fun DrawScope.drawEntity(track: TrackGeometry, entity: ChaseEntity, elapsedMs: Long, sprites: ChaseSprites?, p: ChasePalette) {
    var x = track.laneX(entity.lane.toFloat())
    val y = track.y(entity.y)

    when (entity.kind) {
        EntityKind.LANDED_NET -> drawNet(Offset(x, y), track.spriteSize * 0.5f, alpha = 0.9f)
        EntityKind.ORAN_BERRY, EntityKind.THUNDER_STONE -> {
            // Pickups bob so they read as "grab me" rather than "avoid me".
            val bob = sin(elapsedMs / 180f + entity.id) * track.spriteSize * 0.05f
            val center = Offset(x, y + bob)
            val halo = if (entity.kind == EntityKind.THUNDER_STONE) ChaseFx.StoneGreen else ChaseFx.BerryBlue
            val haloR = track.spriteSize * (0.5f + 0.06f * sin(elapsedMs / 160f + entity.id))
            drawCircle(
                Brush.radialGradient(listOf(halo.copy(alpha = 0.45f + 0.2f * p.night), Color.Transparent), center, haloR),
                radius = haloR, center = center
            )
            drawSparkle(
                Offset(center.x + track.spriteSize * 0.22f, center.y - track.spriteSize * 0.2f),
                track.spriteSize * 0.12f * (0.6f + 0.4f * sin(elapsedMs / 120f + entity.id * 3)),
                Color.White.copy(alpha = 0.9f)
            )
            drawSpriteOr(entity.kind.sprite()?.let { sprites?.get(it) }, center, track.spriteSize * 0.5f, fallback = entity.kind.fallbackColor())
        }
        else -> {
            if (entity.kind == EntityKind.KOFFING) {
                x += sin(elapsedMs / 300f + entity.id) * track.laneWidth * 0.06f
            }
            drawShadow(Offset(x, y + track.spriteSize * 0.36f), track.spriteSize * 0.32f)
            drawSpriteOr(entity.kind.sprite()?.let { sprites?.get(it) }, Offset(x, y), track.spriteSize, fallback = entity.kind.fallbackColor())
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

private fun DrawScope.drawPikachu(track: TrackGeometry, world: ChaseWorld, sprites: ChaseSprites?, p: ChasePalette) {
    val blinkHidden = world.invulnerableMs > 0 && (world.invulnerableMs / 90) % 2 == 0L
    val snare = world.snare
    // Snared Pikachu stops running and thrashes side to side instead.
    val struggle = if (snare != null) sin(world.elapsedMs / 35f) * track.laneWidth * 0.05f else 0f
    val x = track.laneX(world.playerX) + struggle
    val y = track.y(track.config.playerY)
    val bob = if (snare != null) 0f else sin(world.elapsedMs / 70f) * track.spriteSize * 0.04f

    drawShadow(Offset(x, y + track.spriteSize * 0.4f), track.spriteSize * 0.3f)
    if (snare == null) drawDustTrail(track, world, x, y, p)
    if (blinkHidden) return

    // Pikachu carries its own light: subtle by day, a real glow at night.
    val glowR = track.spriteSize * 0.9f
    drawCircle(
        Brush.radialGradient(listOf(ThunderYellow.copy(alpha = 0.12f + 0.28f * p.night), Color.Transparent), Offset(x, y), glowR),
        radius = glowR, center = Offset(x, y)
    )

    // Agility: a crackling electric aura behind Pikachu.
    if (world.isAgile) {
        val flicker = 0.75f + 0.25f * sin(world.elapsedMs / 45f)
        drawCircle(
            Brush.radialGradient(
                colors = listOf(ThunderYellow.copy(alpha = 0.55f * flicker), Color.Transparent),
                center = Offset(x, y),
                radius = track.spriteSize * 0.85f
            ),
            radius = track.spriteSize * 0.85f,
            center = Offset(x, y)
        )
    }

    // Lean into lane switches.
    val lean = (world.targetLane - world.playerX) * 14f

    // Agility after-images trailing behind.
    val ghost = sprites?.get(ChaseSprite.PIKACHU_BACK)
    if (world.isAgile && ghost != null) {
        for (k in 3 downTo 1) {
            ghost.alpha = 70 / k
            drawSpriteOr(ghost, Offset(x, y + bob + track.spriteSize * 0.2f * k), track.spriteSize * 1.05f, fallback = ThunderYellow)
        }
        ghost.alpha = 255
    }
    rotate(lean, pivot = Offset(x, y)) {
        drawSpriteOr(sprites?.get(ChaseSprite.PIKACHU_BACK), Offset(x, y + bob), track.spriteSize * 1.05f, fallback = ThunderYellow)
    }


    if (snare != null) {
        val netRadius = track.spriteSize * 0.55f
        drawNet(Offset(x, y), netRadius, alpha = 0.95f)
        // Time left before Team Rocket reels Pikachu in.
        drawArc(
            color = RocketRed,
            startAngle = -90f,
            sweepAngle = 360f * (1f - snare.progress),
            useCenter = false,
            topLeft = Offset(x - netRadius * 1.25f, y - netRadius * 1.25f),
            size = Size(netRadius * 2.5f, netRadius * 2.5f),
            style = Stroke(width = netRadius * 0.1f, cap = StrokeCap.Round)
        )
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

    // While snared, a rope runs from the balloon to the net — Team Rocket reeling Pikachu in.
    if (world.isSnared) {
        val pikachu = Offset(track.laneX(world.playerX), track.y(track.config.playerY))
        drawLine(Color(0xFF5D4037), Offset(center.x, center.y - radius), pikachu, strokeWidth = radius * 0.04f)
    }

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

/** Agility dash: streaking speed lines down the track, faster as the dash peaks. */
private fun DrawScope.drawSpeedLines(track: TrackGeometry, world: ChaseWorld) {
    if (!world.isAgile) return
    val lineCount = 14
    val travel = (world.elapsedMs % 400) / 400f
    for (i in 0 until lineCount) {
        // Deterministic pseudo-random layout per line, so they don't flicker frame to frame.
        val seed = (i * 7919) % 101 / 101f
        val x = track.pathLeft + seed * track.pathWidth
        val length = size.height * (0.08f + 0.06f * ((i * 31) % 7) / 7f)
        val y = ((travel + i / lineCount.toFloat()) % 1f) * (size.height + length) - length
        drawLine(
            Color.White.copy(alpha = 0.18f + 0.12f * seed),
            Offset(x, y), Offset(x, y + length),
            strokeWidth = track.laneWidth * 0.015f,
            cap = StrokeCap.Round
        )
    }
}

/** Thunderbolt: a screen-wide flash with a bolt down every lane. */
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

/** Each zap is a jagged bolt from Pikachu to the attacker it just knocked out. */
private fun DrawScope.drawStrikes(track: TrackGeometry, world: ChaseWorld, config: ChaseConfig) {
    if (world.strikes.isEmpty()) return
    val from = Offset(track.laneX(world.playerX), track.y(config.playerY) - track.spriteSize * 0.35f)
    world.strikes.forEach { strike ->
        val strength = strike.remainingMs / config.strikeFlashMs.toFloat()
        val to = Offset(track.laneX(strike.lane.toFloat()), track.y(strike.y))
        val segments = 6
        val bolt = Path().apply {
            moveTo(from.x, from.y)
            for (i in 1 until segments) {
                val t = i / segments.toFloat()
                val jitter = (if (i % 2 == 0) 1f else -1f) * track.laneWidth * 0.12f
                lineTo(from.x + (to.x - from.x) * t + jitter, from.y + (to.y - from.y) * t)
            }
            lineTo(to.x, to.y)
        }
        drawPath(bolt, ThunderYellow.copy(alpha = strength), style = Stroke(width = track.laneWidth * 0.06f, cap = StrokeCap.Round))
        drawPath(bolt, Color.White.copy(alpha = strength), style = Stroke(width = track.laneWidth * 0.02f, cap = StrokeCap.Round))
        // Impact burst where the attacker was.
        drawCircle(ThunderYellow.copy(alpha = 0.5f * strength), radius = track.spriteSize * 0.45f * (1.4f - strength * 0.4f), center = to)
    }
}

/** Little puffs kicked up behind Pikachu's feet, drifting back down the track. */
private fun DrawScope.drawDustTrail(track: TrackGeometry, world: ChaseWorld, x: Float, y: Float, p: ChasePalette) {
    val puffs = 6
    val life = 420f
    val color = if (p.night > 0.5f) ThunderYellow else Color.White
    for (i in 0 until puffs) {
        val age = ((world.elapsedMs + i * (life / puffs)) % life) / life
        val side = if (i % 2 == 0) -1f else 1f
        val px = x + side * track.spriteSize * (0.12f + 0.1f * age)
        val py = y + track.spriteSize * (0.38f + 0.55f * age)
        drawCircle(color.copy(alpha = 0.28f * (1f - age)), radius = track.spriteSize * (0.05f + 0.07f * age), center = Offset(px, py))
    }
}

/** Pickup bursts, impact rings and floating "+10"s from [ChaseFx]. */
private fun DrawScope.drawBursts(track: TrackGeometry, world: ChaseWorld, fx: ChaseFx) {
    fx.bursts.forEach { b ->
        val t = (world.elapsedMs - b.atMs) / ChaseFx.LIFETIME_MS.toFloat()
        if (t < 0f || t > 1f) return@forEach
        val c = Offset(track.laneX(b.lane), track.y(b.y))
        val fade = 1f - t
        if (b.ring) {
            drawCircle(
                b.color.copy(alpha = 0.7f * fade),
                radius = track.spriteSize * (0.3f + 0.9f * t), center = c,
                style = Stroke(track.spriteSize * 0.06f * fade + 1f)
            )
        }
        for (i in 0 until b.sparks) {
            val a = (i / b.sparks.toFloat()) * 2f * PI.toFloat() + (b.atMs % 7)
            val d = track.spriteSize * (0.15f + 0.65f * t)
            drawCircle(b.color.copy(alpha = fade), radius = track.spriteSize * 0.045f * fade + 1f, center = Offset(c.x + cos(a) * d, c.y + sin(a) * d))
        }
        b.label?.let { label ->
            drawIntoCanvas {
                PopupPaint.textSize = track.spriteSize * 0.32f
                PopupPaint.alpha = (255 * fade).toInt()
                PopupShadowPaint.textSize = PopupPaint.textSize
                PopupShadowPaint.alpha = (160 * fade).toInt()
                val ty = c.y - track.spriteSize * (0.55f + 0.5f * t)
                it.nativeCanvas.drawText(label, c.x + 2f, ty + 2f, PopupShadowPaint)
                it.nativeCanvas.drawText(label, c.x, ty, PopupPaint)
            }
        }
    }
}

private val PopupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.WHITE
    textAlign = Paint.Align.CENTER
    typeface = Typeface.DEFAULT_BOLD
}

private val PopupShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.BLACK
    textAlign = Paint.Align.CENTER
    typeface = Typeface.DEFAULT_BOLD
}

private fun DrawScope.drawSparkle(center: Offset, r: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        quadraticTo(center.x, center.y, center.x + r, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + r)
        quadraticTo(center.x, center.y, center.x - r, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - r)
        close()
    }
    drawPath(path, color)
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
    // A decoder/bitmap failure inside a third-party drawable must never take the game down.
    val drawn = runCatching { drawIntoCanvas { drawable.draw(it.nativeCanvas) } }.isSuccess
    if (!drawn) drawCircle(fallback, radius = boxSize * 0.32f, center = center)
}

private fun EntityKind.sprite(): ChaseSprite? = when (this) {
    EntityKind.KOFFING -> ChaseSprite.KOFFING
    EntityKind.EKANS -> ChaseSprite.EKANS
    EntityKind.WOBBUFFET -> ChaseSprite.WOBBUFFET
    EntityKind.ORAN_BERRY -> ChaseSprite.ORAN_BERRY
    EntityKind.THUNDER_STONE -> ChaseSprite.THUNDER_STONE
    EntityKind.LANDED_NET -> null // drawn as a shape
}

private fun EntityKind.fallbackColor(): Color = when (this) {
    EntityKind.KOFFING -> Color(0xFF7E57C2)
    EntityKind.EKANS -> Color(0xFF8E24AA)
    EntityKind.WOBBUFFET -> Color(0xFF42A5F5)
    EntityKind.ORAN_BERRY -> Color(0xFF1E88E5)
    EntityKind.THUNDER_STONE -> Color(0xFF66BB6A)
    EntityKind.LANDED_NET -> NetColor
}
