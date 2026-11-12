package com.aditya1875.pokeverse.feature.progression.presentation

import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import com.aditya1875.pokeverse.utils.SoundManager
import com.aditya1875.pokeverse.utils.Haptics
import com.aditya1875.pokeverse.feature.pokemon.onboarding.Partner
import com.aditya1875.pokeverse.R
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.key
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.feature.pokemon.onboarding.Starter
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val CardShape = RoundedCornerShape(28.dp)

/**
 * The look of a card with nothing equipped, which is how every new trainer's first impression looks: a deep indigo
 * dusk sliding into violet and orchid, with soft pastel smoke. It is the same for every partner on purpose.
 * Darkening each type colour instead gave muddy olives and browns (yellow, grass, ground...), and the partner's own
 * colour still shows, as the glow behind them.
 */
private val DefaultSky = listOf(Color(0xFF171C4A), Color(0xFF38287A), Color(0xFF7A3E96))
private val DefaultSmoke = listOf(Color(0xFF7C6CF0), Color(0xFFFF7EB6), Color(0xFF5CC8FF))

/** The card's sky: the backdrop when one is equipped, otherwise the default dusk. */
private fun skyBrush(palette: List<Color>?, @Suppress("UNUSED_PARAMETER") typeColor: Color): Brush =
    Brush.linearGradient(palette ?: DefaultSky)

private fun smokePalette(palette: List<Color>?, @Suppress("UNUSED_PARAMETER") typeColor: Color): List<Color> =
    palette ?: DefaultSmoke

/**
 * The profile's hero card. This is where cosmetics become visible:
 *  - the equipped backdrop paints the whole card, with drifting smoke and light particles over it,
 *  - the equipped frame wraps the card's edge as well as the avatar,
 *  - the player's partner (their starter) stands on it, large and gently bobbing.
 *
 * With nothing equipped it still looks intentional: the card takes the partner's type colour.
 * [partner] is null when Original Assets is off, since the art is official franchise artwork.
 * Callers lay out their content in the card's top-left; the partner owns the bottom-right corner.
 */
@Composable
fun ProfileShowcase(
    partner: Partner?,
    backdropId: String,
    frameId: String,
    modifier: Modifier = Modifier,
    onChangePartner: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val typeColor = partner?.type?.let { pokemonTypeColor(it) } ?: MaterialTheme.colorScheme.primary
    val palette = backdropColors(backdropId)
    val sky = skyBrush(palette, typeColor)
    val smoke = smokePalette(palette, typeColor)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 236.dp)
            .clip(CardShape)
            .background(sky)
            .then(if (frameId.isEmpty()) Modifier.border(1.dp, Color.White.copy(alpha = 0.14f), CardShape) else Modifier)
            .cardFrame(frameId)
    ) {
        SmokeLayer(smoke, Modifier.matchParentSize())
        BackdropDecor(backdropId, Modifier.matchParentSize())
        // Keeps text readable on any backdrop and grounds the partner.
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f))))
        )
        if (partner != null) PartnerArt(partner, typeColor, 156.dp, Modifier.align(Alignment.BottomEnd).offset(x = 10.dp), onChangePartner)
        Box(Modifier.padding(16.dp)) { content() }
    }
}

/**
 * Slow, soft smoke: big radial blobs drifting on looping paths, in the card's own colours.
 * Every motion is a whole number of sine cycles over the loop, so there is no visible restart.
 */
@Composable
private fun SmokeLayer(colors: List<Color>, modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "smoke").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart),
        label = "smoke_t"
    )
    Canvas(modifier) {
        val a = (t * 2f * PI).toFloat()
        val w = size.width
        val h = size.height
        // (x, y, radius, colour index, alpha, speed, phase)
        val blobs = listOf(
            floatArrayOf(0.15f, 0.25f, 0.70f, 0f, 0.34f, 1f, 0.0f),
            floatArrayOf(0.85f, 0.35f, 0.62f, 1f, 0.30f, 2f, 1.3f),
            floatArrayOf(0.45f, 0.80f, 0.75f, 2f, 0.28f, 1f, 2.6f),
            floatArrayOf(0.75f, 0.85f, 0.55f, 0f, 0.26f, 3f, 4.0f),
            floatArrayOf(0.30f, 0.55f, 0.50f, -1f, 0.10f, 2f, 5.1f)
        )
        blobs.forEach { b ->
            val cx = w * (b[0] + 0.14f * sin(a * b[5] + b[6]))
            val cy = h * (b[1] + 0.11f * sin(a * b[5] * 2f + b[6] * 1.7f))
            val radius = w * b[2]
            val color = if (b[3] < 0f) Color.White else colors[b[3].toInt().coerceIn(0, colors.lastIndex)]
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = b[4]), color.copy(alpha = b[4] * 0.45f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * The partner: soft glow, ground shadow and an idle bob. Tap it and it reacts: a bounce, hearts, a line of
 * speech (and Pikachu's cry). Long-press, or the small swap button, changes who the partner is.
 */
@Composable
private fun PartnerArt(
    partner: Partner,
    glow: Color,
    artSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onChange: (() -> Unit)? = null,
) {
    val haptics: Haptics = koinInject()
    val sound: SoundManager = koinInject()
    val reactions = stringArrayResource(R.array.partner_reactions)
    val scope = rememberCoroutineScope()
    val bounce = remember { Animatable(1f) }
    var line by remember { mutableStateOf<String?>(null) }
    var bubbleJob by remember { mutableStateOf<Job?>(null) }
    val hearts = remember { mutableStateListOf<Int>() }
    var nextHeart by remember { mutableIntStateOf(0) }

    val t = rememberInfiniteTransition(label = "partner")
    val bob by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "bob"
    )
    val lift = (sin(bob * 2f * PI) * 4.0).toFloat()
    Box(modifier = modifier.size(artSize)) {
        // glow behind the partner
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(glow.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.55f, size.height * 0.55f),
                    radius = size.minDimension * 0.62f
                ),
                radius = size.minDimension * 0.62f,
                center = Offset(size.width * 0.55f, size.height * 0.55f)
            )
            // ground shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.28f),
                topLeft = Offset(size.width * 0.22f, size.height * 0.86f),
                size = Size(size.width * 0.58f, size.height * 0.07f)
            )
        }
        AsyncImage(
            model = partner.artworkUrl,
            contentDescription = partner.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp, top = 8.dp, end = 0.dp, bottom = 10.dp)
                .graphicsLayer {
                    translationY = -lift.dp.toPx()
                    scaleX = bounce.value; scaleY = bounce.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                }
                .pointerInput(partner.dexId) {
                    detectTapGestures(
                        onTap = {
                            haptics.play(Haptics.Haptic.POP)
                            if (partner.dexId == 25) sound.play(SoundManager.Sound.PIKACHU_CRY, 0.7f, haptic = false)
                            scope.launch {
                                bounce.snapTo(1f)
                                bounce.animateTo(1.14f, tween(90))
                                bounce.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 420f))
                            }
                            repeat(3) { hearts.add(nextHeart++) }
                            line = reactions.randomOrNull()
                            bubbleJob?.cancel()
                            bubbleJob = scope.launch { delay(1500); line = null }
                        },
                        onLongPress = { onChange?.invoke() }
                    )
                }
        )
        hearts.toList().forEach { id -> key(id) { FloatingHeart { hearts.remove(id) } } }
        line?.let { text ->
            ThoughtCloud(
                text,
                modifier = Modifier
                    // Up and to the left of the partner's head, inside the card, with its puffs trailing toward the
                    // head. It may cover a little of the card's text for a moment; it is gone after about a second
                    // and a half.
                    .align(Alignment.TopStart)
                    .offset(x = (-62).dp, y = (-6).dp)
                    .widthIn(max = 150.dp)
            )
        }
        if (onChange != null) {
            val label = stringResource(R.string.partner_change)
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-14).dp, y = 8.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .semantics { contentDescription = label }
                    .clickable(onClick = onChange),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
        }
    }
}

/**
 * What the partner says, as a white comic thought cloud with dark ink: a puffy outline, two small puffs trailing down
 * toward the partner, and a quick pop-in. White on purpose, so it reads as speech whatever backdrop is equipped.
 */
@Composable
private fun ThoughtCloud(text: String, modifier: Modifier = Modifier) {
    val scale = remember(text) { Animatable(0.6f) }
    LaunchedEffect(text) { scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f)) }
    val fill = Color.White
    val ink = Color(0xFF1B1F2A)
    val outline = ink.copy(alpha = 0.55f)
    Box(
        modifier.graphicsLayer {
            scaleX = scale.value; scaleY = scale.value
            // It grows out of the bottom-right corner, the one nearest the partner's head.
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)
        }
    ) {
        Text(
            text,
            color = ink,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            modifier = Modifier
                .background(fill, CloudShape)
                .border(1.5.dp, outline, CloudShape)
                .padding(horizontal = 16.dp, vertical = 11.dp)
        )
        // The trailing puffs of a thought bubble, getting smaller toward the one doing the thinking.
        Box(Modifier.align(Alignment.BottomEnd).offset(x = (-6).dp, y = 9.dp).size(8.dp).background(fill, CircleShape).border(1.dp, outline, CircleShape))
        Box(Modifier.align(Alignment.BottomEnd).offset(x = 1.dp, y = 17.dp).size(5.dp).background(fill, CircleShape).border(1.dp, outline, CircleShape))
    }
}

/** A cloud outline: a rounded body with a row of round puffs along the top and bottom and one at each end. */
private val CloudShape = object : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline {
        val w = size.width
        val h = size.height
        val r = minOf(h * 0.3f, w / 4f)
        var path = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    r, r * 0.8f, w - r, h - r * 0.8f,
                    androidx.compose.ui.geometry.CornerRadius(r * 0.5f)
                )
            )
        }
        fun puff(cx: Float, cy: Float) {
            path = androidx.compose.ui.graphics.Path.combine(
                androidx.compose.ui.graphics.PathOperation.Union,
                path,
                androidx.compose.ui.graphics.Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset(cx, cy), r))
                }
            )
        }
        val n = maxOf(2, Math.ceil(((w - 2 * r) / (r * 1.3f)).toDouble()).toInt())
        for (i in 0..n) {
            val cx = r + (w - 2 * r) * i / n
            puff(cx, r)
            puff(cx, h - r)
        }
        puff(r, h / 2f)
        puff(w - r, h / 2f)
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

/** One heart drifting up and fading from the partner. */
@Composable
private fun BoxScope.FloatingHeart(onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val dx = remember { Random.nextInt(-34, 34) }
    val duration = remember { Random.nextInt(900, 1400) }
    val size = remember { Random.nextInt(14, 22) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(duration, easing = LinearEasing))
        onDone()
    }
    Text(
        "❤",
        color = Color(0xFFFF6B8A),
        fontSize = size.sp,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(x = dx.dp, y = (20 - 110 * progress.value).dp)
            .graphicsLayer { alpha = (1f - progress.value).coerceIn(0f, 1f) }
    )
}

// ── Backdrop motion ──────────────────────────────────────────────────────────

private enum class DecorStyle { STARS, BUBBLES, EMBERS, SPARKLES }

private fun decorStyle(id: String): DecorStyle? = when (id) {
    "backdrop_cosmos", "backdrop_twilight", "backdrop_aurora", "backdrop_night" -> DecorStyle.STARS
    "backdrop_deepsea" -> DecorStyle.BUBBLES
    "backdrop_forge" -> DecorStyle.EMBERS
    else -> null
}

private class Particle(val x: Float, val y: Float, val phase: Float, val size: Float, val speed: Float)

/** Light particles drifting over the backdrop. Nothing is drawn for the default look. */
@Composable
fun BackdropDecor(id: String, modifier: Modifier = Modifier) {
    val style = decorStyle(id) ?: return
    val particles = remember(id) {
        val r = Random(id.hashCode())
        List(26) { Particle(r.nextFloat(), r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 1.6f, 0.5f + r.nextFloat()) }
    }
    val t by rememberInfiniteTransition(label = "decor").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart),
        label = "decor_t"
    )
    Canvas(modifier) {
        particles.forEach { p ->
            // Looping phase 0..1 for this particle; each has its own speed so they never move in lockstep.
            val phase = (t * p.speed + p.phase) % 1f
            val px = p.x * size.width
            when (style) {
                DecorStyle.STARS -> {
                    val twinkle = 0.25f + 0.75f * (0.5f + 0.5f * sin((phase * 2f * PI).toFloat() * 2f))
                    drawCircle(Color.White.copy(alpha = twinkle * 0.9f), radius = p.size.dp.toPx(), center = Offset(px, p.y * size.height))
                }
                DecorStyle.BUBBLES -> {
                    val y = size.height * (1f - phase)
                    drawCircle(
                        Color.White.copy(alpha = 0.35f * (1f - phase)),
                        radius = (p.size * 2.2f).dp.toPx(),
                        center = Offset(px + sin((phase * 2f * PI).toFloat()) * 8.dp.toPx(), y),
                        style = Stroke(1.2.dp.toPx())
                    )
                }
                DecorStyle.EMBERS -> {
                    val y = size.height * (1f - phase)
                    drawCircle(
                        Color(0xFFFFB74D).copy(alpha = 0.85f * (1f - phase)),
                        radius = p.size.dp.toPx(),
                        center = Offset(px + sin((phase * 2f * PI).toFloat() * 1.5f) * 10.dp.toPx(), y)
                    )
                }
                DecorStyle.SPARKLES -> {
                    val a = 0.5f + 0.5f * sin((phase * 2f * PI).toFloat())
                    val c = Offset(px, p.y * size.height)
                    val r = p.size.dp.toPx() * (0.8f + 0.6f * a)
                    drawLine(Color.White.copy(alpha = a * 0.8f), Offset(c.x - r * 1.6f, c.y), Offset(c.x + r * 1.6f, c.y), strokeWidth = 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = a * 0.8f), Offset(c.x, c.y - r * 1.6f), Offset(c.x, c.y + r * 1.6f), strokeWidth = 1.dp.toPx())
                }
            }
        }
    }
}

// ── Frame around the whole card ──────────────────────────────────────────────

/**
 * Draws the equipped frame as the card's border, using the same colours as the avatar ring.
 * Animated frames sweep their colours around the card. A blank or unknown id draws nothing.
 */
@Composable
fun Modifier.cardFrame(
    frameId: String,
    width: androidx.compose.ui.unit.Dp = 3.dp,
    corner: androidx.compose.ui.unit.Dp = 28.dp
): Modifier {
    val colors = frameColors(frameId) ?: return this
    val animated = frameAnimated(frameId)
    val shift = if (animated) {
        val t = rememberInfiniteTransition(label = "card_frame")
        val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart), label = "card_frame_shift")
        v
    } else 0f
    return this.drawWithContent {
        drawContent()
        val stroke = width.toPx()
        // A mirrored gradient slid along the card: animated frames shimmer around the edge, static ones hold still.
        val x0 = -size.width + shift * size.width * 2f
        val brush = Brush.linearGradient(
            colors = colors,
            start = Offset(x0, 0f),
            end = Offset(x0 + size.width, size.height),
            tileMode = TileMode.Mirror
        )
        drawRoundRect(
            brush = brush,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(corner.toPx() - stroke / 2f),
            style = Stroke(stroke)
        )
    }
}

/**
 * A small version of the profile card for the shop, so every preview shows the real result:
 * the player's own partner, with whatever they've already equipped in the other slot.
 */
@Composable
fun ShowcasePreview(
    partner: Partner?,
    backdropId: String,
    frameId: String,
    modifier: Modifier = Modifier
) {
    val typeColor = partner?.type?.let { pokemonTypeColor(it) } ?: MaterialTheme.colorScheme.primary
    val palette = backdropColors(backdropId)
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp)
            .clip(shape)
            .background(skyBrush(palette, typeColor))
            .then(if (frameId.isEmpty()) Modifier.border(1.dp, Color.White.copy(alpha = 0.14f), shape) else Modifier)
            .cardFrame(frameId, width = 2.5.dp, corner = 18.dp)
    ) {
        SmokeLayer(smokePalette(palette, typeColor), Modifier.matchParentSize())
        BackdropDecor(backdropId, Modifier.matchParentSize())
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))))
        )
        if (partner != null) PartnerArt(partner, typeColor, 84.dp, Modifier.align(Alignment.BottomEnd))
    }
}
