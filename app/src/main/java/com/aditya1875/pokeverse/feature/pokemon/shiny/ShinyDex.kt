package com.aditya1875.pokeverse.feature.pokemon.shiny

import androidx.compose.runtime.compositionLocalOf

/**
 * Premium "Shiny Dex": shows shiny forms across the Pokédex. True only while the
 * user both has premium and has the setting on — the app shell resolves that
 * once and provides it here, so no individual screen re-checks billing.
 *
 * The per-Pokémon shiny toggle on the detail page stays free for everyone;
 * this only changes the default everywhere.
 */
val LocalShinyDex = compositionLocalOf { false }

object PokemonSprites {
    private const val BASE = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon"

    /** Classic pixel sprite used by the Pokédex grid and search suggestions. */
    fun pixel(id: Int, shiny: Boolean): String =
        if (shiny) "$BASE/shiny/$id.png" else "$BASE/$id.png"
}
