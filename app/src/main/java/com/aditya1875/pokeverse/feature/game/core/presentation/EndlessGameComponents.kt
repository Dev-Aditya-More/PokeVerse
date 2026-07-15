package com.aditya1875.pokeverse.feature.game.core.presentation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hearts row for endless game modes. The most recently lost heart
 * pops slightly via a spring for feedback.
 */
@Composable
fun LivesRow(
    lives: Int,
    maxLives: Int = 3,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxLives) { index ->
            val filled = index < lives
            val scale by animateFloatAsState(
                targetValue = if (filled) 1f else 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "heart_$index"
            )
            Text(
                text = if (filled) "❤️" else "🤍",
                fontSize = 16.sp,
                modifier = Modifier.scale(scale)
            )
        }
    }
}

/**
 * Shared loading state for every game screen — same wavy spinner, spacing
 * and text style everywhere. The color overrides let a game keep its own
 * background contrast (e.g. WildCatch's sky background needs light text)
 * without diverging in layout or spinner style.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameLoadingContent(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    spinnerColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularWavyProgressIndicator(
                color = spinnerColor
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}

/**
 * Skip / hint perk buttons shown during endless play. Both perks are
 * unlocked by watching a rewarded ad (or free for premium users —
 * the caller decides and just invokes the action).
 */
@Composable
fun SkipHintBar(
    onSkip: () -> Unit,
    onHint: () -> Unit = {},
    hintUsed: Boolean = true,
    showAdTag: Boolean,
    showHint: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showHint) PerkChip(
            icon = Icons.Default.Lightbulb,
            label = "50/50",
            enabled = !hintUsed,
            showAdTag = showAdTag,
            accent = Color(0xFFFFB300),
            onClick = onHint
        )
        PerkChip(
            icon = Icons.Default.SkipNext,
            label = "Skip",
            enabled = true,
            showAdTag = showAdTag,
            accent = Color(0xFF4FC3F7),
            onClick = onSkip
        )
    }
}

@Composable
private fun PerkChip(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    showAdTag: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = if (enabled) accent.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.scale(0.8f)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) accent
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            if (showAdTag && enabled) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accent.copy(alpha = 0.25f)
                ) {
                    Text(
                        "AD",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        color = accent
                    )
                }
            }
        }
    }
}
