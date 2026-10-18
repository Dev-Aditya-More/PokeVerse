package com.aditya1875.pokeverse.feature.game.chase.presentation.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.aditya1875.pokeverse.R

/** Colours for one stretch of the route. Every field cross-fades into the next biome. */
internal data class ChasePalette(
    val fieldTop: Color,
    val fieldBottom: Color,
    val path: Color,
    val pathShade: Color,
    val edge: Color,
    val laneLine: Color,
    val foliage: Color,
    val foliageDark: Color,
    val haze: Color,
    /** 0 = daylight, 1 = full night (lamps lit, neon glow, Pikachu's light). */
    val night: Float
)

/**
 * The run moves through three biomes, looping every [CYCLE_METERS]. Purely cosmetic —
 * the engine never sees them.
 */
internal enum class ChaseBiome(@param:StringRes val label: Int, val startMeters: Float, val palette: ChasePalette) {
    MEADOW(
        R.string.chase_biome_meadow, 0f,
        ChasePalette(
            fieldTop = Color(0xFF3F8F33), fieldBottom = Color(0xFF64B54E),
            path = Color(0xFFE6C98F), pathShade = Color(0xFFC9A76B),
            edge = Color(0xFFB08850), laneLine = Color(0xFFFFF4D6),
            foliage = Color(0xFF2E7D32), foliageDark = Color(0xFF1B5E20),
            haze = Color(0xFFCFEAFF), night = 0f
        )
    ),
    SUNSET(
        R.string.chase_biome_sunset, 700f,
        ChasePalette(
            fieldTop = Color(0xFF5C4A2E), fieldBottom = Color(0xFF8C7B3A),
            path = Color(0xFFE8A96B), pathShade = Color(0xFFB9714A),
            edge = Color(0xFF8E4C2E), laneLine = Color(0xFFFFE2B8),
            foliage = Color(0xFF5B3A2A), foliageDark = Color(0xFF3A2219),
            haze = Color(0xFFFF8A65), night = 0.35f
        )
    ),
    HIDEOUT(
        R.string.chase_biome_hideout, 1600f,
        ChasePalette(
            fieldTop = Color(0xFF0D0F24), fieldBottom = Color(0xFF1A1D42),
            path = Color(0xFF2C3058), pathShade = Color(0xFF1E2142),
            edge = Color(0xFFFF3DCB), laneLine = Color(0xFF4DE8FF),
            foliage = Color(0xFF15183A), foliageDark = Color(0xFF0A0C20),
            haze = Color(0xFF0A0B1E), night = 1f
        )
    );

    companion object {
        const val CYCLE_METERS = 2400f
        private const val BLEND_METERS = 140f

        fun at(meters: Float): ChaseBiome {
            val m = meters % CYCLE_METERS
            return entries.last { m >= it.startMeters }
        }

        /** Palette at [meters], blending into the next biome over the last [BLEND_METERS]. */
        fun paletteAt(meters: Float): ChasePalette {
            val m = meters % CYCLE_METERS
            val current = at(meters)
            val nextIndex = (current.ordinal + 1) % entries.size
            val next = entries[nextIndex]
            val end = if (nextIndex == 0) CYCLE_METERS else next.startMeters
            val t = ((m - (end - BLEND_METERS)) / BLEND_METERS).coerceIn(0f, 1f)
            return if (t == 0f) current.palette else blend(current.palette, next.palette, t)
        }

        private fun blend(a: ChasePalette, b: ChasePalette, t: Float) = ChasePalette(
            fieldTop = lerp(a.fieldTop, b.fieldTop, t),
            fieldBottom = lerp(a.fieldBottom, b.fieldBottom, t),
            path = lerp(a.path, b.path, t),
            pathShade = lerp(a.pathShade, b.pathShade, t),
            edge = lerp(a.edge, b.edge, t),
            laneLine = lerp(a.laneLine, b.laneLine, t),
            foliage = lerp(a.foliage, b.foliage, t),
            foliageDark = lerp(a.foliageDark, b.foliageDark, t),
            haze = lerp(a.haze, b.haze, t),
            night = a.night + (b.night - a.night) * t
        )
    }
}
