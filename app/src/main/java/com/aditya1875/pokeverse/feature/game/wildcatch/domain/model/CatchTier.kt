package com.aditya1875.pokeverse.feature.game.wildcatch.domain.model

import com.aditya1875.pokeverse.utils.LegendaryPokemon

/**
 * How "important" a wild Pokémon is: legendary status first, then base-stat total. Tougher tiers
 * are harder to catch (lower odds, faster ring) and pay more; how they move on screen is decided
 * by the UI from the same tier.
 */
enum class CatchTier(
    val catchFactor: Float,
    val ringSpeedFactor: Float,
    val scoreBonus: Int,
) {
    COMMON(1.00f, 1.00f, 0),
    UNCOMMON(0.92f, 0.95f, 20),
    RARE(0.82f, 0.88f, 50),
    EPIC(0.70f, 0.80f, 100),
    LEGENDARY(0.55f, 0.70f, 200);

    companion object {
        fun of(id: Int, baseStatTotal: Int): CatchTier = when {
            LegendaryPokemon.isLegendary(id) -> LEGENDARY
            baseStatTotal >= 580 -> EPIC
            baseStatTotal >= 500 -> RARE
            baseStatTotal >= 400 -> UNCOMMON
            else -> COMMON
        }
    }
}
