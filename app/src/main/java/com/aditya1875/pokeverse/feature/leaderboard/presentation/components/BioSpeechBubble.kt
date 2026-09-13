package com.aditya1875.pokeverse.feature.leaderboard.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.feature.leaderboard.data.remote.model.LeaderboardEntry
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AUTO_DISMISS_MS = 3200L
private val BUBBLE_MAX_WIDTH = 220.dp
private val BUBBLE_EST_HEIGHT = 62.dp // used only to place the "above" case before layout
private val GAP = 8.dp
private val EDGE_MARGIN = 12.dp
private val TAIL_HEIGHT = 9.dp
private val TAIL_HALF_WIDTH = 8.dp
private val CORNER_RADIUS = 16.dp

/**
 * A minimal rounded-rect speech bubble with a real pointer tail aimed at the
 * tapped row — the classic "who said that" shape, built as a single unioned
 * fill path (rect + triangle) rather than a clipped rect, so the tail never
 * gets cut off. Pops in growing outward from the tail tip (so it visibly
 * "comes from" the row/profile it's attached to) and reverses smoothly on
 * the way out instead of just vanishing.
 */
@Composable
fun BioSpeechBubble(
    entry: LeaderboardEntry,
    anchor: Rect,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scale = remember(entry.uid) { Animatable(0.55f) }
    val alpha = remember(entry.uid) { Animatable(0f) }
    val settle = remember(entry.uid) { Animatable(1f) } // 1 = start offset, 0 = fully settled
    var dismissing by remember(entry.uid) { mutableStateOf(false) }

    suspend fun playOut() {
        if (dismissing) return
        dismissing = true
        coroutineScope {
            launch { alpha.animateTo(0f, tween(140, easing = FastOutSlowInEasing)) }
            scale.animateTo(0.6f, tween(140, easing = FastOutSlowInEasing))
        }
        onDismiss()
    }

    LaunchedEffect(entry.uid) {
        launch { alpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing)) }
        launch { settle.animateTo(0f, tween(320, easing = FastOutSlowInEasing)) }
        scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
    }
    LaunchedEffect(entry.uid) {
        delay(AUTO_DISMISS_MS)
        playOut()
    }

    // Full-screen invisible scrim so tapping anywhere else dismisses the bubble.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(entry.uid) {
                detectTapGestures(onTap = { scope.launch { playOut() } })
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val bubbleWidthPx = with(density) { BUBBLE_MAX_WIDTH.toPx() }
            val bubbleHeightPx = with(density) { BUBBLE_EST_HEIGHT.toPx() }
            val edgeMarginPx = with(density) { EDGE_MARGIN.toPx() }
            val gapPx = with(density) { GAP.toPx() }
            val settlePx = with(density) { 12.dp.toPx() }
            val tailHeightPx = with(density) { TAIL_HEIGHT.toPx() }
            val tailHalfWidthPx = with(density) { TAIL_HALF_WIDTH.toPx() }
            val cornerRadiusPx = with(density) { CORNER_RADIUS.toPx() }

            // Flip above the row once it's in the lower part of the screen so the
            // bubble never gets squeezed against the bottom edge. When it sits
            // above, the tail points down at the row (and vice versa).
            val showAbove = anchor.center.y > maxHeightPx * 0.6f
            val minX = edgeMarginPx
            val maxX = (maxWidthPx - bubbleWidthPx - edgeMarginPx).coerceAtLeast(minX)
            val bubbleLeft = (anchor.center.x - bubbleWidthPx / 2f).coerceIn(minX, maxX)
            val bubbleTop = if (showAbove) anchor.top - gapPx - bubbleHeightPx else anchor.bottom + gapPx
            val tailX = (anchor.center.x - bubbleLeft).coerceIn(0f, bubbleWidthPx)
            val pivotFractionX = (tailX / bubbleWidthPx).coerceIn(0.1f, 0.9f)
            val settleOffset = if (showAbove) settle.value * settlePx else -settle.value * settlePx
            val bubbleColor = MaterialTheme.colorScheme.inverseSurface

            Box(
                modifier = Modifier
                    .widthIn(max = BUBBLE_MAX_WIDTH)
                    .graphicsLayer {
                        translationX = bubbleLeft
                        translationY = bubbleTop + settleOffset
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                        // Grows outward from the tail tip — the point nearest the row — not the bubble's own center.
                        transformOrigin = TransformOrigin(pivotFractionX, if (showAbove) 1f else 0f)
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
                Text(
                    text = entry.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
            }
        }
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
