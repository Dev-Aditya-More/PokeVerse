package com.aditya1875.pokeverse.feature.pokemon.onboarding

/** The first partners on offer in the professor intro. [id] is what gets persisted on the profile. */
enum class Starter(val id: String, val dexId: Int, val displayName: String, val type: String) {
    BULBASAUR("bulbasaur", 1, "Bulbasaur", "grass"),
    CHARMANDER("charmander", 4, "Charmander", "fire"),
    SQUIRTLE("squirtle", 7, "Squirtle", "water"),
    PIKACHU("pikachu", 25, "Pikachu", "electric");

    val artworkUrl: String
        get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$dexId.png"

    companion object {
        fun fromId(id: String): Starter? = entries.firstOrNull { it.id == id }
    }
}
