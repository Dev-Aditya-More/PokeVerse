package com.aditya1875.pokeverse.feature.leaderboard.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.feature.leaderboard.data.remote.model.LeaderboardEntry
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BUBBLE_MAX_WIDTH = 260.dp
private val GAP = 6.dp
private val EDGE_MARGIN = 12.dp
private val TAIL_HEIGHT = 9.dp
private val TAIL_HALF_WIDTH = 8.dp
private val CORNER_RADIUS = 16.dp
private val SPOTLIGHT_INSET = 4.dp
private val SPOTLIGHT_RADIUS = 16.dp
private val BELOW_SPACE_NEEDED = 150.dp
private const val SCRIM_ALPHA = 0.5f

/**
 * Live layout handles for one tappable leaderboard entry. Positions are only
 * resolved at tap time, so list scrolling and graphicsLayer entrance animations
 * (which don't re-trigger onGloballyPositioned) can never leave a stale rect.
 */
class BioAnchorHandles {
    /** The whole tappable row / podium column — gets the spotlight. */
    var target: LayoutCoordinates? = null
    /** The avatar inside it — the tail aims at its x. */
    var avatar: LayoutCoordinates? = null

    fun resolve(): BioAnchor? {
        val t = target?.takeIf { it.isAttached } ?: return null
        val targetRect = t.boundsInWindow()
        val avatarRect = avatar?.takeIf { it.isAttached }?.boundsInWindow() ?: targetRect
        return BioAnchor(targetRect, avatarRect)
    }
}

/** Window-space rects of a tapped entry. */
data class BioAnchor(val target: Rect, val avatar: Rect)

/**
 * Spotlights the tapped player (everything else dims) and pops a speech bubble
 * that names them and quotes their bio, with the tail pointing straight at
 * their avatar. Window coordinates are converted into this overlay's own space,
 * so any Scaffold/inset padding above it doesn't shift the bubble onto a
 * neighbouring row.
 */
@Composable
fun BioSpeechBubble(
    entry: LeaderboardEntry,
    anchor: BioAnchor,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val scale = remember(entry.uid) { Animatable(0.55f) }
    val alpha = remember(entry.uid) { Animatable(0f) }
    val settle = remember(entry.uid) { Animatable(1f) } // 1 = start offset, 0 = fully settled
    var dismissing by remember(entry.uid) { mutableStateOf(false) }
    var hostOrigin by remember { mutableStateOf<Offset?>(null) }

    suspend fun playOut() {
        if (dismissing) return
        dismissing = true
        coroutineScope {
            launch { alpha.animateTo(0f, tween(160, easing = FastOutSlowInEasing)) }
            scale.animateTo(0.6f, tween(160, easing = FastOutSlowInEasing))
        }
        onDismiss()
    }

    LaunchedEffect(entry.uid) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        launch { alpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing)) }
        launch { settle.animateTo(0f, tween(320, easing = FastOutSlowInEasing)) }
        scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
    }
    LaunchedEffect(entry.uid) {
        // Longer bios stay up longer so they can actually be read.
        delay((2600L + entry.bio.length * 40L).coerceAtMost(6500L))
        playOut()
    }

    val density = LocalDensity.current
    val insetPx = with(density) { SPOTLIGHT_INSET.toPx() }
    val spotRadiusPx = with(density) { SPOTLIGHT_RADIUS.toPx() }
    val ringWidthPx = with(density) { 2.dp.toPx() }
    val ringColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { hostOrigin = it.positionInWindow() }
            .pointerInput(entry.uid) {
                detectTapGestures(onTap = { scope.launch { playOut() } })
            }
    ) {
        val origin = hostOrigin ?: return@Box
        val spot = anchor.target.translate(-origin).inflate(insetPx)
        val avatar = anchor.avatar.translate(-origin)

        // Dim everything except the tapped player, and ring them in the accent colour.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val hole = RoundRect(spot, CornerRadius(spotRadiusPx))
                    val scrim = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(Offset.Zero, size))
                        addRoundRect(hole)
                    }
                    drawPath(scrim, Color.Black.copy(alpha = SCRIM_ALPHA * alpha.value))
                    drawPath(
                        Path().apply { addRoundRect(hole) },
                        color = ringColor.copy(alpha = alpha.value),
                        style = Stroke(width = ringWidthPx)
                    )
                }
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val edgeMarginPx = with(density) { EDGE_MARGIN.toPx() }
            val gapPx = with(density) { GAP.toPx() }
            val settlePx = with(density) { 12.dp.toPx() }
            val tailHeightPx = with(density) { TAIL_HEIGHT.toPx() }
            val tailHalfWidthPx = with(density) { TAIL_HALF_WIDTH.toPx() }
            val cornerRadiusPx = with(density) { CORNER_RADIUS.toPx() }
            val belowSpacePx = with(density) { BELOW_SPACE_NEEDED.toPx() }

            // Go above only when there's genuinely no room below and more room above.
            val showAbove = spot.bottom + belowSpacePx > maxHeightPx && spot.top > maxHeightPx - spot.bottom
            val avatarX = avatar.center.x.coerceIn(0f, maxWidthPx)
            var tailX by remember { mutableFloatStateOf(0f) }
            var bubbleWidth by remember { mutableFloatStateOf(1f) }
            val settleOffset = if (showAbove) settle.value * settlePx else -settle.value * settlePx
            val bubbleColor = MaterialTheme.colorScheme.inverseSurface

            Layout(
                content = {
                    Box(
                        modifier = Modifier
                            .widthIn(max = BUBBLE_MAX_WIDTH)
                            .graphicsLayer {
                                translationY = settleOffset
                                scaleX = scale.value
                                scaleY = scale.value
                                this.alpha = alpha.value
                                // Grows outward from the tail tip — the point touching the player.
                                transformOrigin = TransformOrigin(
                                    (tailX / bubbleWidth).coerceIn(0.1f, 0.9f),
                                    if (showAbove) 1f else 0f
                                )
                            }
                            .drawBehind {
                                drawPath(
                                    buildTailBubblePath(size, tailX, tailHeightPx, tailHalfWidthPx, cornerRadiusPx, tailAtBottom = showAbove),
                                    color = bubbleColor
                                )
                            }
                            .padding(
                                start = 14.dp,
                                end = 14.dp,
                                top = if (showAbove) 10.dp else 10.dp + TAIL_HEIGHT,
                                bottom = if (showAbove) 10.dp + TAIL_HEIGHT else 10.dp
                            )
                    ) {
                        BubbleContent(entry)
                    }
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
                val maxLeft = (maxWidthPx - placeable.width - edgeMarginPx).coerceAtLeast(edgeMarginPx)
                val left = (avatarX - placeable.width / 2f).coerceIn(edgeMarginPx, maxLeft)
                val top = if (showAbove) spot.top - gapPx - placeable.height else spot.bottom + gapPx
                tailX = avatarX - left
                bubbleWidth = placeable.width.toFloat().coerceAtLeast(1f)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(left.roundToInt(), top.roundToInt())
                }
            }
        }
    }
}

@Composable
private fun BubbleContent(entry: LeaderboardEntry) {
    val onBubble = MaterialTheme.colorScheme.inverseOnSurface
    Column {
        Row {
            Text(
                text = entry.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.inversePrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (entry.level > 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Lv. ${entry.level}",
                    style = MaterialTheme.typography.labelSmall,
                    color = onBubble.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = "“${entry.bio.trim()}”",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = onBubble,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A rounded rect fused with a triangular tail — one filled path, so the tail is never clipped off. */
private fun buildTailBubblePath(
    size: Size,
    tailX: Float,
    tailHeightPx: Float,
    tailHalfWidthPx: Float,
    cornerRadiusPx: Float,
    tailAtBottom: Boolean
): Path {
    val w = size.width
    val h = size.height
    val bodyHeight = h - tailHeightPx
    val bodyTop = if (tailAtBottom) 0f else tailHeightPx

    val body = Path().apply {
        addRoundRect(
            RoundRect(
                Rect(0f, bodyTop, w, bodyTop + bodyHeight),
                CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )
        )
    }

    val tipX = tailX.coerceIn(cornerRadiusPx + tailHalfWidthPx, w - cornerRadiusPx - tailHalfWidthPx)
    val baseY = if (tailAtBottom) bodyTop + bodyHeight else bodyTop
    val tipY = if (tailAtBottom) h else 0f

    val tail = Path().apply {
        moveTo(tipX - tailHalfWidthPx, baseY)
        lineTo(tipX + tailHalfWidthPx, baseY)
        lineTo(tipX, tipY)
        close()
    }

    val merged = Path()
    merged.op(body, tail, PathOperation.Union)
    return merged
}
