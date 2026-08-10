package com.aditya1875.pokeverse.feature.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LegendaryGoldDeep = Color(0xFF8A5B00)
private val LegendaryGold = Color(0xFFFFC107)
private val LegendaryGoldBright = Color(0xFFFFF3C4)

/**
 * Eye-catching pill marking a Pokémon as Legendary/Mythical: a solid gold
 * gradient, a twinkling rotating star, a light-sweep shimmer, and a soft
 * pulsing glow halo behind it.
 */
@Composable
fun LegendaryBadge(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "legendary_flash")

    val glow by infinite.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val sparkleScale by infinite.animateFloat(
        initialValue = 0.8f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sparkle_scale"
    )
    val sparkleRotation by infinite.animateFloat(
        initialValue = -20f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sparkle_rotation"
    )
    // Sweeps left-to-right across the pill, like light catching foil
    val shimmerProgress by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "shimmer"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Soft pulsing glow halo behind the pill
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(LegendaryGold.copy(alpha = 0.4f * glow), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(LegendaryGoldDeep, LegendaryGold, LegendaryGoldBright, LegendaryGold, LegendaryGoldDeep)
                    )
                )
                .drawWithCache {
                    val bandWidth = size.width * 0.4f
                    val x = -bandWidth + (size.width + bandWidth * 2f) * shimmerProgress
                    onDrawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                                start = Offset(x, 0f),
                                end = Offset(x + bandWidth, size.height)
                            )
                        )
                    }
                }
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Legendary",
                tint = Color.White,
                modifier = Modifier
                    .size(13.dp)
                    .graphicsLayer {
                        scaleX = sparkleScale
                        scaleY = sparkleScale
                        rotationZ = sparkleRotation
                    }
            )
            Text(
                "LEGENDARY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }
    }
}
