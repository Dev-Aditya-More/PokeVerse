package com.aditya1875.pokeverse.presentation.screens.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.toColor

@Composable
fun ResponsiveMetaballSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackWidth: Dp = 44.dp,
    trackHeight: Dp = 26.dp,
    thumbDp: Dp = 20.dp,
) {
    val minTouch = 48.dp
    val density = LocalDensity.current

    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        label = "switchProgress"
    )
    // Use theme colors
    val trackOn = MaterialTheme.colorScheme.primary.toArgb()
    val trackOff = MaterialTheme.colorScheme.surfaceVariant.toArgb()

    val finalModifier = modifier
        .requiredSizeIn(minWidth = minTouch, minHeight = minTouch)
        .padding(2.dp)

    val color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f * progress)

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
            modifier = Modifier.size(trackWidth.coerceAtLeast(minTouch), trackHeight.coerceAtMost(trackWidth))
        ) {
            val w = size.width
            val h = size.height
            val radius = h / 2f

            drawRoundRect(
                color = Color(lerp(trackOff, trackOn, progress)),
                cornerRadius = CornerRadius(radius, radius),
                size = Size(w, h),
                topLeft = Offset.Zero
            )

            val thumbRadius = with(density) { thumbDp.toPx() } / 2f
            val centerY = h / 2f
            val leftX = radius
            val rightX = w - radius
            val cx = lerp(leftX, rightX, progress)

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

            if (progress > 0.2f) {
                drawCircle(
                    color = color,
                    radius = thumbRadius * 1.6f * progress,
                    center = Offset(cx, centerY)
                )
            }
        }
    }
}
