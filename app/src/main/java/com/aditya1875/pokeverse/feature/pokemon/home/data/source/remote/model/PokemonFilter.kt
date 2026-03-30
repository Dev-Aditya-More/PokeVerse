package com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model

import androidx.compose.ui.graphics.Color

enum class Region(val displayName: String, val offset: Int, val limit: Int, val range: IntRange) {
    KANTO("Kanto", 0, 151, 1..151),
    JOHTO("Johto", 151, 100, 152..251),
    HOENN("Hoenn", 251, 135, 252..386),
    SINNOH("Sinnoh", 386, 107, 387..493),
    UNOVA("Unova", 493, 156, 494..649),
    KALOS("Kalos", 649, 72, 650..721),
    ALOLA("Alola", 721, 88, 722..809),
    GALAR("Galar", 809, 96, 810..905),
    PALDEA("Paldea", 905, 105, 906..1010),
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