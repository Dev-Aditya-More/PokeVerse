package com.aditya1875.pokeverse.feature.pokemon.onboarding

import com.aditya1875.pokeverse.feature.pokemon.dex.DexProgress

/**
 * A trainer's partner Pokémon: one of the four starters, or any Pokémon they have caught.
 *
 * It is persisted in the profile's existing `starter` text field, so older app versions and the server
 * rules keep working: a starter stays its plain id ("pikachu"), anyone else is "dexId:name:type"
 * (for example "133:eevee:normal"), which carries the name and type so no lookup is needed to draw it.
 */
data class Partner(val dexId: Int, val displayName: String, val type: String?) {

    val artworkUrl: String
        get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$dexId.png"

    companion object {
        fun of(starter: Starter) = Partner(starter.dexId, starter.displayName, starter.type)

        /** The text saved on the profile for a non-starter partner. */
        fun encode(dexId: Int, name: String, type: String?): String =
            "$dexId:${name.lowercase().take(16)}:${type.orEmpty()}"

        fun from(id: String): Partner? {
            Starter.fromId(id)?.let { return of(it) }
            val parts = id.split(':')
            val dex = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 1..DexProgress.TOTAL } ?: return null
            val name = parts.getOrNull(1).orEmpty().ifBlank { "#$dex" }
                .replace('-', ' ').replaceFirstChar { it.uppercase() }
            return Partner(dex, name, parts.getOrNull(2)?.takeIf { it.isNotBlank() })
        }
    }
}
