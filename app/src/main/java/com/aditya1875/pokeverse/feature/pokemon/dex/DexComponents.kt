package com.aditya1875.pokeverse.feature.pokemon.dex

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import java.text.NumberFormat

private val BallRed = Color(0xFFE53935)
private val BallWhite = Color(0xFFF5F5F5)
private val BallInk = Color(0xFF212121)

/** A tiny Poké Ball, drawn rather than shipped as an asset, used as the "caught" mark. */
@Composable
fun PokeBallIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val d = size.minDimension
        val r = d / 2f
        val c = Offset(r, r)
        val band = d * 0.1f
        // Top half red, bottom half white.
        drawArc(BallRed, 180f, 180f, useCenter = true, topLeft = Offset.Zero, size = Size(d, d))
        drawArc(BallWhite, 0f, 180f, useCenter = true, topLeft = Offset.Zero, size = Size(d, d))
        drawRect(BallInk, topLeft = Offset(0f, r - band / 2f), size = Size(d, band))
        drawCircle(BallInk, radius = d * 0.2f, center = c)
        drawCircle(BallWhite, radius = d * 0.11f, center = c)
        drawCircle(BallInk, radius = r - d * 0.03f, center = c, style = Stroke(d * 0.07f))
    }
}

/**
 * Seen and caught as two progress bars. Totals are deliberately not printed: the two pools differ
 * (see [DexProgress.CATCHABLE]) and two different "out of" numbers read like a bug.
 * A new entry, found on this screen or any other, animates the bar it landed in.
 */
@Composable
fun DexProgressStrip(
    progress: DexProgress,
    modifier: Modifier = Modifier,
    onPlayWildCatch: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val rattle = remember { Animatable(0f) }
    // Any new entry makes the ball in the header rattle.
    val onBump: () -> Unit = {
        scope.launch {
            rattle.snapTo(0f)
            for (angle in listOf(16f, -14f, 10f, -6f, 0f)) rattle.animateTo(angle, tween(70))
        }
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PokeBallIcon(Modifier.size(18.dp).graphicsLayer { rotationZ = rattle.value })
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.dex_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            DexBar("seen", stringResource(R.string.dex_seen), progress.seenCount, DexProgress.TOTAL, MaterialTheme.colorScheme.primary, progress.loaded, onBump)
            DexBar("caught", stringResource(R.string.dex_caught), progress.caughtCount, DexProgress.CATCHABLE, BallRed, progress.loaded, onBump)
            // Caught only comes from Wild Catch, so say so (and offer the shortcut).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.dex_catch_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (onPlayWildCatch != null) {
                    TextButton(onClick = onPlayWildCatch) {
                        Text(stringResource(R.string.dex_play), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Count each bar showed the last time it was on screen, for the lifetime of the process. */
private val lastShownCounts = mutableMapOf<String, Int>()

@Composable
private fun DexBar(
    key: String,
    label: String,
    count: Int,
    total: Int,
    color: Color,
    loaded: Boolean,
    onBump: () -> Unit,
) {
    // Start from where this bar stood last time, so a Pokémon found on another screen animates in
    // when you come back instead of simply being there.
    val from = remember { lastShownCounts[key]?.takeIf { loaded && it < count } ?: count }
    var target by remember { mutableIntStateOf(from) }
    var bumpId by remember { mutableIntStateOf(0) }
    var bumpDelta by remember { mutableIntStateOf(0) }
    val pop = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }
    val rise = remember { Animatable(0f) }

    LaunchedEffect(count, loaded) {
        if (!loaded) return@LaunchedEffect // the placeholder zero is not a real value
        val previous = lastShownCounts[key]
        lastShownCounts[key] = count
        target = count
        // The very first load of the session just counts up; only a real increase celebrates.
        if (previous != null && count > previous) {
            bumpDelta = count - previous
            bumpId++
        }
    }
    LaunchedEffect(bumpId) {
        if (bumpId == 0) return@LaunchedEffect
        onBump()
        coroutineScope {
            launch {
                pop.snapTo(1f)
                pop.animateTo(1.5f, tween(140))
                pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
            }
            launch {
                flash.snapTo(1f)
                flash.animateTo(0f, tween(900))
            }
            launch {
                rise.snapTo(0f)
                rise.animateTo(1f, tween(1300, easing = LinearOutSlowInEasing))
            }
        }
    }

    val shown by animateIntAsState(target, tween(700, easing = FastOutSlowInEasing), label = "dexCount")
    val fraction by animateFloatAsState(
        targetValue = (target.toFloat() / total).coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "dexBar",
    )
    val nf = NumberFormat.getIntegerInstance()
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(contentAlignment = Alignment.Center) {
                Text(
                    nf.format(shown),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                )
                if (rise.value > 0f && rise.value < 1f) {
                    Text(
                        "+$bumpDelta",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = color,
                        modifier = Modifier.graphicsLayer {
                            translationY = -(10f + 16f * rise.value) * density
                            alpha = 1f - rise.value
                        },
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .graphicsLayer { scaleY = 1f + 0.9f * flash.value }
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            if (fraction > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                ) {
                    // A bright flash sweeps through the fill when something new lands.
                    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f * flash.value)))
                }
            }
        }
    }
}

/** "New Pokédex entry registered!" toast that slides in from the top of the detail page. */
@Composable
fun DexEntryBanner(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFF101626),
            shadowElevation = 8.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PokeBallIcon(Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.dex_new_entry),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** "Trainer's party": every member of a full team was caught in Wild Catch. Colours are passed in so it sits right on light and dark cards. */
@Composable
fun PartyBadge(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PokeBallIcon(Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.dex_party_badge),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = content,
        )
    }
}

/** A small Poké Ball that bobs, and rattles while [talking]; the app's stand-in for a character. */
@Composable
fun MascotBall(talking: Boolean, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val transition = rememberInfiniteTransition(label = "mascotBall")
    val bob by transition.animateFloat(
        initialValue = -2f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val rattle by transition.animateFloat(
        initialValue = -9f, targetValue = 9f,
        animationSpec = infiniteRepeatable(tween(230, easing = LinearEasing), RepeatMode.Reverse),
        label = "rattle",
    )
    PokeBallIcon(
        modifier
            .size(size)
            .graphicsLayer {
                translationY = bob.dp.toPx()
                rotationZ = if (talking) rattle else 0f
            }
    )
}
