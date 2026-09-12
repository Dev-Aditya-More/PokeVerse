package com.aditya1875.pokeverse.utils

/**
 * A small, explicitly curated set of iconic/anime-recognizable Pokémon —
 * mascots, starters' final evolutions, and well-known legendaries. This is
 * NOT real popularity data (no such dataset exists here); it exists purely
 * to add a lighthearted "fan-favorite" flavor to the Compare tool's edge
 * verdict, same "static ID set" shape as [LegendaryPokemon].
 */
object FanFavoritePokemon {
    private val ids = setOf(
        // Gen 1 icons
        1, 3, 6, 9, 25, 26, 39, 94, 130, 131, 133, 142, 143, 149, 150, 151,
        // Gen 2
        157, 160, 196, 197, 212, 229, 243, 244, 245, 249, 250,
        // Gen 3
        254, 257, 260, 282, 306, 354, 359, 373, 376, 380, 381, 384,
        // Gen 4
        389, 392, 395, 445, 448, 461, 470, 471, 483, 484, 487, 493,
        // Gen 5
        571, 609, 635, 643, 644,
        // Gen 6
        658, 700, 716, 717,
        // Gen 7
        724, 727, 730, 785, 800,
        // Gen 8
        812, 815, 818, 823, 849, 887, 888, 889
    )

    fun isFanFavorite(id: Int): Boolean = id in ids
}
