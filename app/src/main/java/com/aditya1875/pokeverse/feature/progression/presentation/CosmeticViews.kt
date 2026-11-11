package com.aditya1875.pokeverse.feature.progression.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.feature.progression.domain.Cosmetics

/** How a frame id looks. Kept out of the domain so the catalogue stays free of Compose. */
private data class FrameStyle(val colors: List<Color>, val animated: Boolean = false)

private val Gold = Color(0xFFFFC24B)

private fun frameStyle(id: String): FrameStyle? = when (id) {
    // Level frames are tone-on-tone (one hue, light and dark), so they sit with any backdrop instead of fighting it.
    "frame_pokeball" -> FrameStyle(listOf(Color(0xFFE53935), Color(0xFFFFCDD2), Color(0xFFE53935), Color(0xFFFFCDD2), Color(0xFFE53935)))
    "frame_ultra" -> FrameStyle(listOf(Color(0xFFFFD54F), Color(0xFFFFF3C4), Color(0xFFFFB300), Color(0xFFFFF3C4), Color(0xFFFFD54F)))
    "frame_master" -> FrameStyle(listOf(Color(0xFFAB47BC), Color(0xFFE040FB), Color(0xFF7C4DFF), Color(0xFFE040FB), Color(0xFFAB47BC)), animated = true)
    "frame_legend" -> FrameStyle(listOf(Gold, Color(0xFFFFF3C4), Color(0xFF4DD0E1), Color(0xFFFFF3C4), Gold), animated = true)
    "frame_mythic" -> FrameStyle(
        listOf(Color(0xFFFF5252), Color(0xFFFFD740), Color(0xFF69F0AE), Color(0xFF40C4FF), Color(0xFFE040FB), Color(0xFFFF5252)),
        animated = true
    )
    "frame_flame" -> FrameStyle(listOf(Color(0xFFFF6D00), Color(0xFFFFC107), Color(0xFFE53935), Color(0xFFFF6D00)))
    "frame_tide" -> FrameStyle(listOf(Color(0xFF1E88E5), Color(0xFF4DD0E1), Color(0xFF0D47A1), Color(0xFF1E88E5)))
    "frame_leaf" -> FrameStyle(listOf(Color(0xFF43A047), Color(0xFFC6FF00), Color(0xFF1B5E20), Color(0xFF43A047)))
    "frame_volt" -> FrameStyle(listOf(Color(0xFFFFEB3B), Color(0xFFFFFFFF), Color(0xFFFFC107), Color(0xFFFFEB3B)))
    "frame_shiny" -> FrameStyle(listOf(Color(0xFFE0E0E0), Gold, Color(0xFFFFFFFF), Gold, Color(0xFFE0E0E0)), animated = true)
    else -> null
}

/** Frame colours / motion for the card border in [ProfileShowcase]; null when the id isn't a frame. */
internal fun frameColors(id: String): List<Color>? = frameStyle(id)?.colors
internal fun frameAnimated(id: String): Boolean = frameStyle(id)?.animated == true

/**
 * Draws the equipped frame as a ring around whatever it modifies (an avatar). A blank or unknown id
 * draws nothing, so callers can pass the stored id straight through.
 */
@Composable
fun Modifier.avatarFrame(frameId: String, width: Dp = 3.dp): Modifier {
    val style = frameStyle(frameId) ?: return this
    val spin = if (style.animated) {
        val t = rememberInfiniteTransition(label = "frame_spin")
        val angle by t.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Restart),
            label = "frame_angle"
        )
        angle
    } else 0f
    return this.drawWithContent {
        drawContent()
        val stroke = width.toPx()
        val brush = Brush.sweepGradient(style.colors, center = Offset(size.width / 2f, size.height / 2f))
        rotate(spin) {
            drawCircle(
                brush = brush,
                radius = (size.minDimension - stroke) / 2f,
                style = Stroke(stroke)
            )
        }
    }
}

/** Colours of a backdrop, for the gradient and for tinting the smoke over it. Null for the default look. */
internal fun backdropColors(id: String): List<Color>? = when (id) {
    "backdrop_twilight" -> listOf(Color(0xFF1B1446), Color(0xFF4A1942), Color(0xFF8A3B5C))
    "backdrop_aurora" -> listOf(Color(0xFF04201F), Color(0xFF0B4F5C), Color(0xFF2B2A6B))
    "backdrop_cosmos" -> listOf(Color(0xFF05071A), Color(0xFF2A1458), Color(0xFF5B1E7A))
    "backdrop_night" -> listOf(Color(0xFF0A1128), Color(0xFF14264F), Color(0xFF1C3A6B))
    "backdrop_deepsea" -> listOf(Color(0xFF031C2B), Color(0xFF05475E), Color(0xFF0A6C80))
    "backdrop_forge" -> listOf(Color(0xFF1A0A06), Color(0xFF4A1508), Color(0xFF8A2B0C))
    "backdrop_verdant" -> listOf(Color(0xFF04180C), Color(0xFF0E3B1E), Color(0xFF1F6B34))
    "backdrop_storm" -> listOf(Color(0xFF0B0C1F), Color(0xFF23265A), Color(0xFF55501A))
    else -> null
}

/**
 * The one colour a cosmetic belongs to. Frames and backdrops of the same element share it (Flame and Forge,
 * Tide and Deep Sea, Leaf and Verdant, Volt and Stormfront), and the title chip takes it from whichever is
 * equipped, so the pieces of a look read as one set. Null for the rainbow Mythic frame and the plain card.
 */
private fun familyAccent(id: String): Color? = when (id) {
    "frame_flame", "backdrop_forge" -> Color(0xFFFF8A50)
    "frame_tide", "backdrop_deepsea" -> Color(0xFF4FC3F7)
    "frame_leaf", "backdrop_verdant" -> Color(0xFF81C784)
    "frame_volt", "backdrop_storm" -> Color(0xFFFFE066)
    "frame_master", "backdrop_cosmos", "backdrop_twilight" -> Color(0xFFCE93D8)
    "frame_legend", "frame_shiny", "frame_ultra" -> Gold
    "frame_pokeball" -> Color(0xFFEF9A9A)
    "backdrop_aurora" -> Color(0xFF4DD0E1)
    "backdrop_night" -> Color(0xFF90CAF9)
    else -> null
}

/** The accent of what a player wears: their frame's if it has one, otherwise their backdrop's. */
fun cosmeticAccent(frameId: String, backdropId: String): Color? = familyAccent(frameId) ?: familyAccent(backdropId)

/** Gradient behind the profile header for the equipped backdrop, or null for the plain card. */
fun backdropBrush(id: String): Brush? = backdropColors(id)?.let { Brush.linearGradient(it) }

/** The widget accent for an equipped look: null keeps the stock gold. Colours are ARGB longs so the widget can use them without Compose. */
fun widgetAccentArgb(id: String): Long? = when (id) {
    "widget_royal" -> 0xFFB388FF
    "widget_ember" -> 0xFFFF7043
    "widget_ocean" -> 0xFF4FC3F7
    "widget_mint" -> 0xFF69F0AE
    else -> null
}

/** Title as a small pill. Level-earned titles read gold; bought ones use the theme colour. */
@Composable
fun TitleChip(titleId: String, modifier: Modifier = Modifier, accent: Color? = null) {
    val cosmetic = Cosmetics.find(titleId) ?: return
    val earned = cosmetic.levelUnlock != null || cosmetic.fromChallenge
    // Matches the equipped frame or backdrop when there is one; otherwise gold for earned titles.
    val tint = accent ?: if (earned) Gold else MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.16f),
        modifier = modifier
    ) {
        Text(
            text = stringResource(cosmetic.name),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold,
            color = tint,
            maxLines = 1
        )
    }
}

/** Paints the backdrop (if any) behind a profile card, rounded and slightly translucent so text stays readable. */
fun Modifier.profileBackdrop(id: String): Modifier {
    val brush = backdropBrush(id) ?: return this
    return this.background(brush, RoundedCornerShape(24.dp), alpha = 0.30f)
}
