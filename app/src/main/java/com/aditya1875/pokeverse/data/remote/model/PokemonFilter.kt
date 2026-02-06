package com.aditya1875.pokeverse.data.remote.model

import androidx.compose.ui.graphics.Color

enum class Region(val id: Int, val displayName: String, val range: IntRange) {
    KANTO(1, "Kanto", 1..151),
    JOHTO(2, "Johto", 152..251),
    HOENN(3, "Hoenn", 252..386),
    SINNOH(4, "Sinnoh", 387..493),
    UNOVA(5, "Unova", 494..649),
    KALOS(6, "Kalos", 650..721),
    ALOLA(7, "Alola", 722..809),
    GALAR(8, "Galar", 810..898),
    PALDEA(9, "Paldea", 899..1010);

    val offset: Int get() = range.first - 1
    val limit: Int get() = range.last - offset
}

enum class PokemonType(val displayName: String, val color: Color) {
    NORMAL("Normal", Color(0xFFA8A878)),
    FIRE("Fire", Color(0xFFF08030)),
    WATER("Water", Color(0xFF6890F0)),
    ELECTRIC("Electric", Color(0xFFF8D030)),
    GRASS("Grass", Color(0xFF78C850)),
    ICE("Ice", Color(0xFF98D8D8)),
    FIGHTING("Fighting", Color(0xFFC03028)),
    POISON("Poison", Color(0xFFA040A0)),
    GROUND("Ground", Color(0xFFE0C068)),
    FLYING("Flying", Color(0xFFA890F0)),
    PSYCHIC("Psychic", Color(0xFFF85888)),
    BUG("Bug", Color(0xFFA8B820)),
    ROCK("Rock", Color(0xFFB8A038)),
    GHOST("Ghost", Color(0xFF705898)),
    DRAGON("Dragon", Color(0xFF7038F8)),
    DARK("Dark", Color(0xFF705848)),
    STEEL("Steel", Color(0xFFB8B8D0)),
    FAIRY("Fairy", Color(0xFFEE99AC));
}

data class PokemonFilter(
    val selectedRegion: Region? = null,
    val selectedType: PokemonType? = null
)