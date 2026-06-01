package com.aditya1875.pokeverse.utils

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for Pokémon type accent colors used across particle
 * screens, trivia cards, clash cards, and any future type-colored UI.
 *
 * Prefer this over defining private maps in individual feature files.
 */
val pokemonTypeColors: Map<String, Color> = mapOf(
    "fire"     to Color(0xFFFF6B35),
    "water"    to Color(0xFF4FC3F7),
    "grass"    to Color(0xFF81C784),
    "electric" to Color(0xFFFFD54F),
    "psychic"  to Color(0xFFEC407A),
    "ice"      to Color(0xFF80DEEA),
    "dragon"   to Color(0xFF7E57C2),
    "dark"     to Color(0xFF616161),
    "fairy"    to Color(0xFFF48FB1),
    "fighting" to Color(0xFFFF7043),
    "flying"   to Color(0xFF90CAF9),
    "poison"   to Color(0xFFAB47BC),
    "ground"   to Color(0xFFBCAAA4),
    "rock"     to Color(0xFFBDBDBD),
    "bug"      to Color(0xFF9CCC65),
    "ghost"    to Color(0xFF7E57C2),
    "steel"    to Color(0xFF78909C),
    "normal"   to Color(0xFFEEEEEE),
)

/** Returns the accent color for a given type name, defaulting to normal-type gray. */
fun pokemonTypeColor(typeName: String): Color =
    pokemonTypeColors[typeName.lowercase()] ?: Color(0xFFEEEEEE)
