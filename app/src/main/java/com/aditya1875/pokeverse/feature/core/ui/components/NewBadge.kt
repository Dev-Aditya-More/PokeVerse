package com.aditya1875.pokeverse.feature.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NewBadgeStart = Color(0xFF00E5A0)
private val NewBadgeEnd = Color(0xFF00B8D4)

/**
 * A small, gently pulsing "NEW" pill for flagging freshly-shipped features so
 * returning users notice them. Deliberately lighter-weight than
 * [LegendaryBadge] — this should read as a quiet nudge, not a big callout.
 */
@Composable
fun NewBadge(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "new_badge")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "new_pulse"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(NewBadgeStart, NewBadgeEnd)))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "NEW",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = 0.5.sp,
            fontSize = 10.sp
        )
    }
}
