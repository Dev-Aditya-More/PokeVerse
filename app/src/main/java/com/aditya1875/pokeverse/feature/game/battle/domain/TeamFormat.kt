package com.aditya1875.pokeverse.feature.game.battle.domain

/**
 * What kind of team the player is building. Each format has its own size, its own Legendary limit and its
 * own collection of saved teams, so a 3 vs 3 team never shows up (or gets trimmed) in a 6 vs 6 lineup.
 *
 * Pure Kotlin: the picker, quick fill, saved teams and the online matchmaking all read these same numbers.
 */
enum class TeamFormat(
    /** Exactly this many Pokémon, or null for Practice, where anything from 1 to [TeamRules.MAX_TEAM] goes. */
    val size: Int?,
    /** Most Legendary or Mythical Pokémon a team may bring. A 3 vs 3 full of legends is decided on stats alone. */
    val maxLegendaries: Int,
    /** How many "Suggest a team" fills in. */
    val suggestSize: Int,
    /** Legendaries a suggested team may contain. */
    val suggestLegendaries: Int,
) {
    /** Against the bot: any size up to six, and the original limit of three Legendaries. */
    PRACTICE(size = null, maxLegendaries = TeamRules.MAX_LEGENDARIES, suggestSize = TeamRules.MAX_TEAM, suggestLegendaries = 2),

    /** A quick 3 vs 3: one Legendary at most. */
    QUICK_3(size = 3, maxLegendaries = 1, suggestSize = 3, suggestLegendaries = 1),

    /** The full 6 vs 6: up to two Legendaries. */
    QUICK_6(size = 6, maxLegendaries = 2, suggestSize = 6, suggestLegendaries = 2);

    /** The most Pokémon a team can have in this format. */
    val maxTeam: Int get() = size ?: TeamRules.MAX_TEAM

    /** Online formats need exactly [size] Pokémon; Practice does not. */
    val isOnline: Boolean get() = size != null

    companion object {
        /** The online format with exactly [size] Pokémon, or null if there is none. */
        fun quick(size: Int): TeamFormat? = entries.firstOrNull { it.size == size }
    }
}
