package com.aditya1875.pokeverse.feature.game.battle.data

import android.content.Context
import com.aditya1875.pokeverse.feature.game.battle.domain.TeamFormat
import com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A team the player saved to load again in one tap. Only Pokémon ids are kept, in battle order. */
data class SavedTeam(val name: String, val ids: List<Int>)

/**
 * The player's team collections for Team Battle, kept on this phone: one per [TeamFormat], so the teams built
 * for 3 vs 3, for 6 vs 6 and for Practice never mix, and the same name can be used in each. A saved team can
 * name Pokémon the player has since evolved away; loading it simply skips those, so a collection never goes stale.
 *
 * Practice keeps the collection that existed before formats did. The first time an online format is opened, the
 * teams there that already have exactly that many Pokémon (and fit its Legendary limit) are copied in once, so
 * nobody loses a team they built; after that the collections are independent.
 */
class SavedTeamsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("battle_saved_teams", Context.MODE_PRIVATE)
    private val collections: Map<TeamFormat, MutableStateFlow<List<SavedTeam>>> =
        TeamFormat.entries.associateWith { MutableStateFlow(load(it)) }

    /** The saved teams for [format], kept up to date. */
    fun teams(format: TeamFormat): StateFlow<List<SavedTeam>> = collections.getValue(format).asStateFlow()

    /** Saves [ids] as [name] in [format]'s collection, replacing a team of the same name. False when it is full. */
    fun save(format: TeamFormat, name: String, ids: List<Int>): Boolean {
        val clean = cleanName(name)
        val team = TeamRules.trim(ids, format)
        if (clean.isEmpty() || team.isEmpty()) return false
        // An online format only keeps complete teams: a half-built team could never be used there.
        if (format.size != null && team.size != format.size) return false
        val current = collections.getValue(format).value
        val replacing = current.any { it.name.equals(clean, ignoreCase = true) }
        if (!replacing && current.size >= MAX_TEAMS) return false
        write(format, current.filterNot { it.name.equals(clean, ignoreCase = true) } + SavedTeam(clean, team))
        return true
    }

    fun delete(format: TeamFormat, name: String) =
        write(format, collections.getValue(format).value.filterNot { it.name == name })

    /** Puts the account's copy of [format]'s collection in place (used by the account sync). */
    fun replaceAll(format: TeamFormat, teams: List<SavedTeam>) {
        val clean = teams.mapNotNull { t ->
            val name = cleanName(t.name).takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val ids = t.ids.filter { it > 0 }.distinct().take(MAX_TEAM_SIZE).takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            SavedTeam(name, ids)
        }.distinctBy { it.name.lowercase() }.take(MAX_TEAMS)
        if (clean != collections.getValue(format).value) write(format, clean)
    }

    private fun write(format: TeamFormat, teams: List<SavedTeam>) {
        collections.getValue(format).value = teams
        prefs.edit().putString(keyOf(format), encode(teams)).apply()
    }

    private fun load(format: TeamFormat): List<SavedTeam> {
        val key = keyOf(format)
        if (prefs.contains(key)) return decode(prefs.getString(key, "").orEmpty())
        if (format == TeamFormat.PRACTICE) return emptyList()
        // First time in this format: bring over the legacy teams that already fit it, once.
        val seeded = decode(prefs.getString(keyOf(TeamFormat.PRACTICE), "").orEmpty()).filter { fitsExactly(it.ids, format) }
        prefs.edit().putString(key, encode(seeded)).apply()
        return seeded
    }

    companion object {
        const val MAX_TEAMS = 20
        const val MAX_TEAM_SIZE = 6
        const val MAX_NAME = 20
        private const val KEY = "teams"
        private const val TEAM_SEP = "\n"
        private const val FIELD_SEP = "|"

        /** Where a format's collection is stored. Practice keeps the original key, so nothing is migrated. */
        internal fun keyOf(format: TeamFormat): String = when (format) {
            TeamFormat.PRACTICE -> KEY
            TeamFormat.QUICK_3 -> "teams_3v3"
            TeamFormat.QUICK_6 -> "teams_6v6"
        }

        /** Whether [ids] is a complete team for [format] that already respects its Legendary limit. */
        internal fun fitsExactly(ids: List<Int>, format: TeamFormat): Boolean =
            format.size != null && ids.size == format.size && TeamRules.trim(ids, format).size == format.size

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
