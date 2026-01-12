package com.aditya1875.pokeverse.screens.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

@Composable
fun ResponsiveMetaballSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // optional sizes — keeps sensible defaults but scalable
    trackWidth: Dp = 44.dp,
    trackHeight: Dp = 26.dp,
    thumbDp: Dp = 20.dp,
) {
    val minTouch = 48.dp // recommended minimum touch target
    val density = LocalDensity.current

    // animated progress 0..1
    val progress by animateFloatAsState(targetValue = if (checked) 1f else 0f, label = "switchProgress")

    // ensure the composed size won't be flattened below min touch
    val finalModifier = modifier
        .requiredSizeIn(minWidth = minTouch, minHeight = minTouch)
        .padding(2.dp) // small extra breathing room

    // Convert to px when drawing
    Box(
        modifier = finalModifier
            .wrapContentSize(align = Alignment.Center)
            .semantics {
                this.contentDescription = if (checked) "On" else "Off"
                this.stateDescription = if (checked) "On" else "Off"
            }
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
    ) {
        Canvas(
            modifier = Modifier
                .size(trackWidth.coerceAtLeast(minTouch), trackHeight.coerceAtMost(trackWidth))
        ) {
            val w = size.width
            val h = size.height
            val radius = h / 2f

            // track colors
            val trackOn = Color(0xFF802525)
            val trackOff = Color(0xFF616161)

            // draw track (rounded capsule)
            drawRoundRect(
                color = androidx.compose.ui.graphics.lerp(trackOff, trackOn, progress),
                cornerRadius = CornerRadius(radius, radius),
                size = Size(w, h),
                topLeft = Offset.Zero
            )

            // thumb position
            val thumbRadius = with(density) { thumbDp.toPx() } / 2f
            val centerY = h / 2f
            // leftmost center
            val leftX = radius
            val rightX = w - radius
            val cx = lerp(leftX, rightX, progress)

            // subtle shadow/outline for thumb
            drawCircle(
                color = Color.Black.copy(alpha = 0.16f),
                radius = thumbRadius + 2f,
                center = Offset(cx, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(cx, centerY)
            )

            // optional metaball glow when pressed/checked (small highlight)
            if (progress > 0.2f) {
                drawCircle(
                    color = Color(0xFFFFF59D).copy(alpha = 0.08f * progress),
                    radius = thumbRadius * 1.6f * progress,
                    center = Offset(cx, centerY)
                )
            }
        }
    }
}
