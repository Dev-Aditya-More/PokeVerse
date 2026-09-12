package com.aditya1875.pokeverse.feature.leaderboard.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.feature.leaderboard.data.remote.model.LeaderboardEntry
import kotlinx.coroutines.delay

private const val AUTO_DISMISS_MS = 3200L
private val BUBBLE_MAX_WIDTH = 220.dp
private val BUBBLE_EST_HEIGHT = 56.dp // used only to place the "above" case before layout
private val GAP = 10.dp
private val EDGE_MARGIN = 12.dp

/**
 * A minimal speech-bubble popup showing a leaderboard entry's bio, anchored near
 * the tapped row (flips above/below depending on available space, clamped so it
 * never runs off the sides). Dismisses on a timeout, an outside tap, or the
 * caller toggling it off (tapping the same row again).
 */
@Composable
fun BioSpeechBubble(
    entry: LeaderboardEntry,
    anchor: Rect,
    onDismiss: () -> Unit
) {
    LaunchedEffect(entry.uid) {
        delay(AUTO_DISMISS_MS)
        onDismiss()
    }

    val scale = remember(entry.uid) { Animatable(0.85f) }
    val alpha = remember(entry.uid) { Animatable(0f) }
    LaunchedEffect(entry.uid) {
        scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }
    LaunchedEffect(entry.uid) {
        alpha.animateTo(1f, tween(180))
    }

    // Full-screen invisible scrim so tapping anywhere else dismisses the bubble.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(entry.uid) { detectTapGestures(onTap = { onDismiss() }) }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val bubbleWidthPx = with(density) { BUBBLE_MAX_WIDTH.toPx() }
            val bubbleHeightPx = with(density) { BUBBLE_EST_HEIGHT.toPx() }
            val edgeMarginPx = with(density) { EDGE_MARGIN.toPx() }
            val gapPx = with(density) { GAP.toPx() }

            // Flip above the row once it's in the lower part of the screen so the
            // bubble never gets squeezed against the bottom edge.
            val showAbove = anchor.center.y > maxHeightPx * 0.6f
            val minX = edgeMarginPx
            val maxX = (maxWidthPx - bubbleWidthPx - edgeMarginPx).coerceAtLeast(minX)
            val bubbleLeft = (anchor.center.x - bubbleWidthPx / 2f).coerceIn(minX, maxX)
            val bubbleTop = if (showAbove) anchor.top - gapPx - bubbleHeightPx else anchor.bottom + gapPx
            val pivotFractionX = ((anchor.center.x - bubbleLeft) / bubbleWidthPx).coerceIn(0.1f, 0.9f)

            Box(
                modifier = Modifier
                    .widthIn(max = BUBBLE_MAX_WIDTH)
                    .graphicsLayer {
                        translationX = bubbleLeft
                        translationY = bubbleTop
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                        // Pop outward from the point nearest the tapped row, not the bubble's own center.
                        transformOrigin = TransformOrigin(pivotFractionX, if (showAbove) 1f else 0f)
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.inverseSurface)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
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
