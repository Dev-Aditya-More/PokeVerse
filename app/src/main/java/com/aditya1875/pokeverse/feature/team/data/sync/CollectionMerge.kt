package com.aditya1875.pokeverse.feature.team.data.sync

/**
 * Teams and favourites as they travel between this phone and the account, plus the rules for combining two
 * copies. Pure Kotlin so every rule is unit tested.
 *
 * Nothing is ever lost by a sync: two copies are combined, never replaced. Each team carries the time it was
 * last changed, and the newer version of a team wins. Deleting something leaves a tombstone (key -> time), so a
 * phone that still has the old copy can't bring it back; adding it again later is newer than the tombstone and
 * sticks. The default team ("My Team") is the same team on every phone, whatever id each install gave it.
 */
data class SyncMember(val name: String, val imageUrl: String, val addedAt: Long)

data class SyncTeam(
    val id: String,
    val name: String,
    val createdAt: Long,
    val isDefault: Boolean,
    val members: List<SyncMember>,
    /** When this team was last changed (name or members); 0 = never touched (a fresh install's empty default). */
    val updatedAt: Long,
)

data class SyncFav(val name: String, val imageUrl: String, val addedAt: Long)

/** A Team Battle preset: [format] is the TeamFormat name, ids in battle order. */
data class SyncBattleTeam(val format: String, val name: String, val ids: List<Int>, val updatedAt: Long) {
    val key get() = format + ":" + name.lowercase()
}

data class CollectionState(
    val teams: List<SyncTeam> = emptyList(),
    val favourites: List<SyncFav> = emptyList(),
    val battleTeams: List<SyncBattleTeam> = emptyList(),
    /** "team:<id>", "fav:<name>" or "bteam:<format>:<name>" -> when it was deleted. */
    val deleted: Map<String, Long> = emptyMap(),
)

object CollectionMerge {

    const val MAX_MEMBERS = 6

    /** Tombstones older than this are forgotten, so the list can't grow forever. */
    const val TOMBSTONE_TTL_MS = 180L * 24 * 60 * 60 * 1000

    fun teamKey(id: String) = "team:$id"
    fun favKey(name: String) = "fav:$name"
    fun battleKey(key: String) = "bteam:$key"

    /** Saved battle teams kept per format (matches SavedTeamsRepository.MAX_TEAMS). */
    const val MAX_BATTLE_TEAMS = 20

    private fun SyncTeam.slot() = if (isDefault) "default" else id

    /** Same team content: what the player sees, ignoring when each member happened to be stored. */
    private fun SyncTeam.sameAs(o: SyncTeam) =
        name == o.name && isDefault == o.isDefault && members.map { it.name } == o.members.map { it.name }

    /** Combines two copies; the result holds everything either side added and nothing either side deleted. */
    fun merge(a: CollectionState, b: CollectionState, nowMs: Long): CollectionState {
        val deleted = (a.deleted.keys + b.deleted.keys)
            .associateWith { maxOf(a.deleted[it] ?: 0L, b.deleted[it] ?: 0L) }
            .filterValues { nowMs - it < TOMBSTONE_TTL_MS }

        val teams = (a.teams + b.teams)
            .groupBy { it.slot() }
            .map { (_, copies) -> copies.maxBy { it.updatedAt } } // first wins a tie, i.e. [a]
            .filter { t -> t.isDefault || (deleted[teamKey(t.id)] ?: -1L) < t.updatedAt }
            .let(::oneDefault)
            .sortedWith(compareByDescending<SyncTeam> { it.isDefault }.thenBy { it.createdAt })

        val favourites = (a.favourites + b.favourites)
            .groupBy { it.name }
            .map { (_, copies) -> copies.maxBy { it.addedAt } }
            .filter { f -> (deleted[favKey(f.name)] ?: -1L) < f.addedAt }
            .sortedByDescending { it.addedAt }

        val battleTeams = (a.battleTeams + b.battleTeams)
            .groupBy { it.key }
            .map { (_, copies) -> copies.maxBy { it.updatedAt } }
            .filter { t -> (deleted[battleKey(t.key)] ?: -1L) < t.updatedAt }
            .groupBy { it.format }
            .flatMap { (_, teams) -> teams.sortedByDescending { it.updatedAt }.take(MAX_BATTLE_TEAMS).sortedBy { it.updatedAt } }

        return CollectionState(teams, favourites, battleTeams, deleted)
    }

    /** Exactly one default team: a phone that somehow has none gets an empty one back. */
    private fun oneDefault(teams: List<SyncTeam>): List<SyncTeam> {
        val defaults = teams.filter { it.isDefault }
        if (defaults.size == 1) return teams
        if (defaults.isEmpty()) return teams + SyncTeam("default", "My Team", 0L, true, emptyList(), 0L)
        val keep = defaults.maxBy { it.updatedAt }
        return teams.filter { !it.isDefault || it === keep }
    }

    /**
     * What changed on this phone since [previous] (the last state it knew about): new or edited teams get
     * [nowMs] as their change time, and anything that disappeared gets a tombstone. [previous] null means this
     * phone has never been tracked: what it has is recorded as-is, and an untouched empty default team is marked
     * as never changed so it can't overwrite the account's real one.
     */
    fun track(
        previous: CollectionState?,
        current: List<SyncTeam>,
        currentFavs: List<SyncFav>,
        nowMs: Long,
        currentBattle: List<SyncBattleTeam> = emptyList(),
    ): CollectionState {
        if (previous == null) {
            val teams = current.map { t ->
                val touched = if (t.isDefault && t.members.isEmpty()) 0L
                              else maxOf(t.createdAt, t.members.maxOfOrNull { it.addedAt } ?: 0L)
                t.copy(updatedAt = touched)
            }
            // Presets already on the phone count as never edited, so the account's version of the same one wins.
            return CollectionState(teams, currentFavs, currentBattle.map { it.copy(updatedAt = 0L) }, emptyMap())
        }
        val before = previous.teams.associateBy { it.id }
        val teams = current.map { t ->
            val p = before[t.id]
            t.copy(updatedAt = if (p != null && p.sameAs(t)) p.updatedAt else nowMs)
        }
        val nowIds = current.map { it.id }.toSet()
        val nowFavs = currentFavs.map { it.name }.toSet()
        val deleted = previous.deleted.toMutableMap()
        previous.teams.filter { it.id !in nowIds && !it.isDefault }.forEach { deleted[teamKey(it.id)] = nowMs }
        previous.favourites.filter { it.name !in nowFavs }.forEach { deleted[favKey(it.name)] = nowMs }
        val beforeBattle = previous.battleTeams.associateBy { it.key }
        val battle = currentBattle.map { t ->
            val p = beforeBattle[t.key]
            t.copy(updatedAt = if (p != null && p.ids == t.ids && p.name == t.name) p.updatedAt else nowMs)
        }
        val nowBattle = currentBattle.map { it.key }.toSet()
        previous.battleTeams.filter { it.key !in nowBattle }.forEach { deleted[battleKey(it.key)] = nowMs }
        return CollectionState(teams, currentFavs, battle, deleted)
    }

    /** True when the two hold the same teams and favourites (ignoring storage order and timestamps). */
    fun sameContent(a: CollectionState, b: CollectionState): Boolean {
        fun key(s: CollectionState) = Triple(
            s.teams.map { Triple(it.id, it.name, it.members.map { m -> m.name }) }.toSet(),
            s.favourites.map { it.name }.toSet(),
            s.battleTeams.map { Triple(it.format, it.name, it.ids) }.toSet(),
        )
        return key(a) == key(b)
    }
}
