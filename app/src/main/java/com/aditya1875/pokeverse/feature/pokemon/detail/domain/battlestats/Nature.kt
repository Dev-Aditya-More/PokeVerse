package com.aditya1875.pokeverse.feature.pokemon.detail.domain.battlestats

/**
 * The 25 mainline-game natures. Each boosts one stat by 10% and lowers
 * another by 10% — except the five neutral natures, which do nothing.
 * `boosts`/`lowers` use PokeAPI's stat name convention (e.g. "special-attack")
 * so they line up directly with [com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.Stat.name].
 */
enum class Nature(val displayName: String, val boosts: String?, val lowers: String?) {
    HARDY("Hardy", null, null),
    LONELY("Lonely", "attack", "defense"),
    BRAVE("Brave", "attack", "speed"),
    ADAMANT("Adamant", "attack", "special-attack"),
    NAUGHTY("Naughty", "attack", "special-defense"),
    BOLD("Bold", "defense", "attack"),
    DOCILE("Docile", null, null),
    RELAXED("Relaxed", "defense", "speed"),
    IMPISH("Impish", "defense", "special-attack"),
    LAX("Lax", "defense", "special-defense"),
    TIMID("Timid", "speed", "attack"),
    HASTY("Hasty", "speed", "defense"),
    SERIOUS("Serious", null, null),
    JOLLY("Jolly", "speed", "special-attack"),
    NAIVE("Naive", "speed", "special-defense"),
    MODEST("Modest", "special-attack", "attack"),
    MILD("Mild", "special-attack", "defense"),
    QUIET("Quiet", "special-attack", "speed"),
    BASHFUL("Bashful", null, null),
    RASH("Rash", "special-attack", "special-defense"),
    CALM("Calm", "special-defense", "attack"),
    GENTLE("Gentle", "special-defense", "defense"),
    SASSY("Sassy", "special-defense", "speed"),
    CAREFUL("Careful", "special-defense", "special-attack"),
    QUIRKY("Quirky", null, null);

    /** 1.1 if this nature boosts the given stat, 0.9 if it lowers it, else 1.0 */
    fun multiplierFor(statName: String): Float = when (statName) {
        boosts -> 1.1f
        lowers -> 0.9f
        else -> 1.0f
    }
}
