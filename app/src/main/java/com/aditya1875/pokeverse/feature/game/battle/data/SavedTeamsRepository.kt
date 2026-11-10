package com.aditya1875.pokeverse.feature.game.battle.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A team the player saved to load again in one tap. Only Pokémon ids are kept, in battle order. */
data class SavedTeam(val name: String, val ids: List<Int>)

/**
 * The player's team collection for Team Battle, kept on this phone. A saved team can name Pokémon the
 * player has since evolved away; loading it simply skips those, so a collection never goes stale.
 */
class SavedTeamsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("battle_saved_teams", Context.MODE_PRIVATE)
    private val _teams = MutableStateFlow(decode(prefs.getString(KEY, "").orEmpty()))
    val teams: StateFlow<List<SavedTeam>> = _teams.asStateFlow()

    /** Saves [ids] as [name], replacing a team of the same name. False when the collection is full. */
    fun save(name: String, ids: List<Int>): Boolean {
        val clean = cleanName(name)
        if (clean.isEmpty() || ids.isEmpty()) return false
        val current = _teams.value
        val replacing = current.any { it.name.equals(clean, ignoreCase = true) }
        if (!replacing && current.size >= MAX_TEAMS) return false
        val next = current.filterNot { it.name.equals(clean, ignoreCase = true) } +
            SavedTeam(clean, com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.trim(ids))
        write(next)
        return true
    }

    fun delete(name: String) = write(_teams.value.filterNot { it.name == name })

    private fun write(teams: List<SavedTeam>) {
        _teams.value = teams
        prefs.edit().putString(KEY, encode(teams)).apply()
    }

    companion object {
        const val MAX_TEAMS = 20
        const val MAX_TEAM_SIZE = 6
        const val MAX_NAME = 20
        private const val KEY = "teams"
        private const val TEAM_SEP = "\n"
        private const val FIELD_SEP = "|"

        /** A name without the characters the storage format uses, trimmed and capped. */
        fun cleanName(name: String): String =
            name.replace(FIELD_SEP, " ").replace("\n", " ").replace("\r", " ").trim().take(MAX_NAME)

        fun encode(teams: List<SavedTeam>): String =
            teams.joinToString(TEAM_SEP) { it.name + FIELD_SEP + it.ids.joinToString(",") }

        /** Reads what [encode] wrote; anything malformed is dropped rather than crashing. */
        fun decode(raw: String): List<SavedTeam> = raw.split(TEAM_SEP).mapNotNull { line ->
            val i = line.indexOf(FIELD_SEP)
            if (i <= 0) return@mapNotNull null
            val name = cleanName(line.substring(0, i)).takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val ids = line.substring(i + 1).split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }
            if (ids.isEmpty()) null else SavedTeam(name, ids.distinct().take(MAX_TEAM_SIZE))
        }.take(MAX_TEAMS)
    }
}
