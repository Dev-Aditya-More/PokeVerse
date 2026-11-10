package com.aditya1875.pokeverse.feature.game.battle.domain

import com.aditya1875.pokeverse.utils.LegendaryPokemon
import kotlin.random.Random

/**
 * Who may be on a Team Battle team. A team has up to [MAX_TEAM] Pokémon and at most [MAX_LEGENDARIES] of them
 * Legendary or Mythical, so a team of nothing but legends can't simply win on stats. Pure Kotlin, so the rule
 * is the same everywhere it is checked: the picker, a loaded saved team, quick fill and the online room.
 */
object TeamRules {
    const val MAX_TEAM = 6
    const val MAX_LEGENDARIES = 3

    fun isLegendary(id: Int): Boolean = LegendaryPokemon.isLegendary(id)

    fun legendaryCount(ids: Collection<Int>): Int = ids.count { isLegendary(it) }

    /** Whether [id] can join [current] without breaking the size or Legendary limit. */
    fun canAdd(current: List<Int>, id: Int): Boolean =
        id !in current && current.size < MAX_TEAM && (!isLegendary(id) || legendaryCount(current) < MAX_LEGENDARIES)

    /** [ids] in order, dropping repeats and any Legendary past the limit, capped at [MAX_TEAM]. */
    fun trim(ids: List<Int>): List<Int> {
        val kept = mutableListOf<Int>()
        for (id in ids) if (canAdd(kept, id)) kept += id
        return kept
    }

    /** A full team picked at random from [roster] that respects every limit. */
    fun quickFill(roster: Collection<Int>, random: Random): List<Int> = trim(roster.toList().shuffled(random))
}
