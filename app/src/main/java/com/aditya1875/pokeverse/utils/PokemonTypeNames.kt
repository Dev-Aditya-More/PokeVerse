package com.aditya1875.pokeverse.utils

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aditya1875.pokeverse.R

/**
 * Display names for the 18 types, in the user's language (official localized names,
 * e.g. "Fogo" / "Feu" for fire). Type ids from PokeAPI stay English everywhere else —
 * only what's shown goes through here.
 */
@StringRes
fun pokemonTypeNameRes(type: String): Int? = when (type.lowercase()) {
    "normal" -> R.string.type_normal
    "fire" -> R.string.type_fire
    "water" -> R.string.type_water
    "grass" -> R.string.type_grass
    "electric" -> R.string.type_electric
    "ice" -> R.string.type_ice
    "fighting" -> R.string.type_fighting
    "poison" -> R.string.type_poison
    "ground" -> R.string.type_ground
    "flying" -> R.string.type_flying
    "psychic" -> R.string.type_psychic
    "bug" -> R.string.type_bug
    "rock" -> R.string.type_rock
    "ghost" -> R.string.type_ghost
    "dragon" -> R.string.type_dragon
    "dark" -> R.string.type_dark
    "steel" -> R.string.type_steel
    "fairy" -> R.string.type_fairy
    else -> null
}

/** Localized type name for display; unknown ids fall back to a capitalized id rather than crashing. */
@Composable
fun localizedTypeName(type: String): String =
    pokemonTypeNameRes(type)?.let { stringResource(it) } ?: type.replaceFirstChar { it.uppercase() }
