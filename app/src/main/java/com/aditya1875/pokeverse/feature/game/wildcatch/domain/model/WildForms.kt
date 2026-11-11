package com.aditya1875.pokeverse.feature.game.wildcatch.domain.model

/**
 * Which alternate forms may turn up in Wild Catch, and what to call them. Anything that needs a Mega Stone
 * (or Primal Reversion, or a Gigantamax/Dynamax state) is left out; every other form is fair game.
 */
object WildForms {
    private val EXCLUDED = listOf("mega", "primal", "gmax")

    /** [pokeApiName] is a variety name such as "pikachu-rock-star" or "charizard-mega-x". */
    fun allowed(pokeApiName: String): Boolean {
        val parts = pokeApiName.lowercase().split("-")
        return parts.none { it in EXCLUDED }
    }

    private val REGIONAL = mapOf("alola" to "Alolan", "galar" to "Galarian", "hisui" to "Hisuian", "paldea" to "Paldean")

    /** "rattata-alola" -> "Alolan Rattata"; "pikachu-rock-star" -> "Pikachu Rock Star"; "mr-mime-galar" -> "Galarian Mr Mime". */
    fun displayName(pokeApiName: String): String {
        val parts = pokeApiName.split("-")
        val regional = parts.firstOrNull { it in REGIONAL }
        val rest = parts.filter { it !in REGIONAL }.joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
        return if (regional != null) "${REGIONAL.getValue(regional)} $rest" else rest
    }
}
