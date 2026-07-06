package com.aditya1875.pokeverse.feature.game.core.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ComboLabel(combo: Int, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = combo,
        transitionSpec = {
            if (targetState > initialState) {
                scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) + fadeIn() togetherWith
                    scaleOut(tween(80)) + fadeOut(tween(80))
            } else {
                fadeIn(tween(100)) togetherWith fadeOut(tween(200))
            }
        },
        modifier = modifier
    ) { c ->
        if (c >= 2) {
            val (label, color) = when {
                c >= 7 -> "LEGENDARY! ⚡" to Color(0xFFFFD700)
                c >= 5 -> "AMAZING! 💥" to Color(0xFFFF6D00)
                c >= 3 -> "GREAT! 🔥" to Color(0xFFFF9800)
                else -> "NICE! 🎯" to Color(0xFF4CAF50)
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = color.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "×$c",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = color
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        } else {
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun PbChip(bestScore: Int, currentScore: Int, modifier: Modifier = Modifier) {
    if (bestScore <= 0) return
    val isBeating = currentScore > bestScore
    val color = if (isBeating) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isBeating) Color(0xFF4CAF50).copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isBeating) BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f)) else null,
        modifier = modifier
    ) {
        Text(
            text = if (isBeating) "NEW PB! 🏆" else "PB $bestScore",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isBeating) FontWeight.Black else FontWeight.Normal,
            color = color
        )
    }
}
