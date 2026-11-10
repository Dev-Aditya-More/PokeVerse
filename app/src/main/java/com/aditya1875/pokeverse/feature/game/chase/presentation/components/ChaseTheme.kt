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
 * The run passes through seven places, each a different length, and loops every [CYCLE_METERS]. The stretches
 * are deliberately uneven (450 m of meadow, 800 m in the hideout) so the scenery never changes on a beat you
 * can predict. Purely cosmetic: the engine never sees them.
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
    COAST(
        R.string.chase_biome_coast, 450f,
        ChasePalette(
            fieldTop = Color(0xFF2E9FB0), fieldBottom = Color(0xFF63C7D4),
            path = Color(0xFFF2DDA4), pathShade = Color(0xFFD9BC7A),
            edge = Color(0xFFB8935A), laneLine = Color(0xFFFFFFFF),
            foliage = Color(0xFF2E8B57), foliageDark = Color(0xFF1F6B42),
            haze = Color(0xFFBFEFFF), night = 0f
        )
    ),
    FOREST(
        R.string.chase_biome_forest, 1050f,
        ChasePalette(
            fieldTop = Color(0xFF1F5C2A), fieldBottom = Color(0xFF2F7A3A),
            path = Color(0xFFB58F5A), pathShade = Color(0xFF8F6B3E),
            edge = Color(0xFF5A3F22), laneLine = Color(0xFFEAD9A8),
            foliage = Color(0xFF14451E), foliageDark = Color(0xFF0B2E13),
            haze = Color(0xFF9FC9A0), night = 0.15f
        )
    ),
    SUNSET(
        R.string.chase_biome_sunset, 1550f,
        ChasePalette(
            fieldTop = Color(0xFF5C4A2E), fieldBottom = Color(0xFF8C7B3A),
            path = Color(0xFFE8A96B), pathShade = Color(0xFFB9714A),
            edge = Color(0xFF8E4C2E), laneLine = Color(0xFFFFE2B8),
            foliage = Color(0xFF5B3A2A), foliageDark = Color(0xFF3A2219),
            haze = Color(0xFFFF8A65), night = 0.35f
        )
    ),
    SNOW(
        R.string.chase_biome_snow, 2250f,
        ChasePalette(
            fieldTop = Color(0xFF8FB3CC), fieldBottom = Color(0xFFB8D4E6),
            path = Color(0xFFB5C7D6), pathShade = Color(0xFF93A8BA),
            edge = Color(0xFF6F8AA3), laneLine = Color(0xFFFFFFFF),
            foliage = Color(0xFF4F7F8F), foliageDark = Color(0xFF35606E),
            haze = Color(0xFFDCEBF5), night = 0.1f
        )
    ),
    CAVE(
        R.string.chase_biome_cave, 2800f,
        ChasePalette(
            fieldTop = Color(0xFF2A2433), fieldBottom = Color(0xFF3B3347),
            path = Color(0xFF6B6275), pathShade = Color(0xFF4F4859),
            edge = Color(0xFF8E7CC3), laneLine = Color(0xFFD9C9FF),
            foliage = Color(0xFF3A3050), foliageDark = Color(0xFF241E33),
            haze = Color(0xFF1A1624), night = 0.7f
        )
    ),
    HIDEOUT(
        R.string.chase_biome_hideout, 3500f,
        ChasePalette(
            fieldTop = Color(0xFF0D0F24), fieldBottom = Color(0xFF1A1D42),
            path = Color(0xFF2C3058), pathShade = Color(0xFF1E2142),
            edge = Color(0xFFFF3DCB), laneLine = Color(0xFF4DE8FF),
            foliage = Color(0xFF15183A), foliageDark = Color(0xFF0A0C20),
            haze = Color(0xFF0A0B1E), night = 1f
        )
    );

    companion object {
        const val CYCLE_METERS = 4300f
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
