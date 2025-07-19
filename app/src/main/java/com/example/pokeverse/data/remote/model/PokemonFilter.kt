package com.example.pokeverse.data.remote.model

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

data class PokemonFilter(
    val selectedRegion: Region? = null,
)