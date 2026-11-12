package com.aditya1875.pokeverse.feature.game.battle.domain

import com.aditya1875.pokeverse.utils.PokemonIndex
import kotlin.random.Random

/**
 * A ready-made team from the player's own roster. A random pick is often six of the same type, which is
 * both easy to beat and dull to play; this prefers Pokémon that bring a type the team does not have yet,
 * then fills any remaining places with whoever is left. Pure, so every guarantee is unit tested.
 *
 * Guarantees: at most [size] Pokémon, no repeats, only ids from the roster, at most [maxLegendaries]
 * Legendaries, and never more than [TeamRules] allows.
 */
object TeamSuggester {

    fun suggest(
        roster: Collection<Int>,
        index: Map<Int, PokemonIndex.Entry>,
        size: Int,
        maxLegendaries: Int,
        random: Random,
    ): List<Int> {
        val limit = size.coerceIn(0, TeamRules.MAX_TEAM)
        val legendCap = maxLegendaries.coerceIn(0, TeamRules.MAX_LEGENDARIES)
        if (limit == 0) return emptyList()

        // Non-legendaries get the first look so a legend is only added when it brings something new.
        val shuffled = roster.distinct().shuffled(random)
        val candidates = shuffled.filter { !TeamRules.isLegendary(it) } + shuffled.filter { TeamRules.isLegendary(it) }

        val team = mutableListOf<Int>()
        val covered = HashSet<String>()

        fun fits(id: Int) =
            id !in team && team.size < limit &&
                (!TeamRules.isLegendary(id) || TeamRules.legendaryCount(team) < legendCap)

        // Pass 1: only Pokémon whose every type is new to the team.
        for (id in candidates) {
            if (!fits(id)) continue
            val types = index[id]?.types.orEmpty()
            if (types.isNotEmpty() && types.none { it in covered }) {
                team += id
                covered += types
            }
        }
        // Pass 2: fill what is left with anyone legal.
        for (id in candidates) {
            if (team.size >= limit) break
            if (fits(id)) team += id
        }
        return team
    }
}
