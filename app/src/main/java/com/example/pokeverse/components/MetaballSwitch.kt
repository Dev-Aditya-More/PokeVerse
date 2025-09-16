package com.example.pokeverse.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MetaballSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(checked, label = "MetaballSwitch")

    val thumbOffset by transition.animateDp(
        transitionSpec = { tween(durationMillis = 400, easing = FastOutSlowInEasing) },
        label = "ThumbOffset"
    ) { state ->
        if (state) 20.dp else 0.dp
    }

    val thumbScale by transition.animateFloat(
        transitionSpec = { tween(400, easing = FastOutSlowInEasing) },
        label = "ThumbScale"
    ) { state ->
        if (state) 1.2f else 1f
    }

    val gooeyScale by transition.animateFloat(
        transitionSpec = { tween(400, easing = FastOutSlowInEasing) },
        label = "GooeyScale"
    ) { state ->
        if (state) 1f else 0f
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (checked) Color(0xFF802525) else Color.Black)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .height(28.dp)
            .width(47.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Gooey metaball "stretch" effect
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (gooeyScale > 0f) {
                drawCircle(
                    color = Color(0xFF802525),
                    radius = 14f * gooeyScale,
                    center = Offset(
                        size.width / 2,
                        size.height / 2
                    ),
                    alpha = 0.4f
                )
            }
        }

        // Thumb
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .scale(thumbScale)
                .background(Color.White, CircleShape)
        )
    }
}
