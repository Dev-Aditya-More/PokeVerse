package com.aditya1875.pokeverse.feature.team.data.sync

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.aditya1875.pokeverse.feature.game.battle.data.SavedTeam
import com.aditya1875.pokeverse.feature.game.battle.data.SavedTeamsRepository
import com.aditya1875.pokeverse.feature.game.battle.domain.TeamFormat
import com.aditya1875.pokeverse.feature.team.data.local.dao.FavouritesDao
import com.aditya1875.pokeverse.feature.team.data.local.dao.TeamDao
import com.aditya1875.pokeverse.feature.team.data.local.db.TeamDatabase
import com.aditya1875.pokeverse.feature.team.data.local.entity.FavouriteEntity
import com.aditya1875.pokeverse.feature.team.data.local.entity.TeamEntity
import com.aditya1875.pokeverse.feature.team.data.local.entity.TeamMemberEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.UUID

private val Context.collectionStore by preferencesDataStore("collection_sync")

/**
 * Keeps the player's teams, favourites and Team Battle presets on their account, so a new phone, a reinstall or clearing data
 * brings them back.
 *
 * How it works: Room stays the source the screens read. This class watches it and, after each change, records
 * what changed against the last state it knew ([CollectionMerge.track]: change times and deletion tombstones).
 * A sync then combines that with the account's copy inside a Firestore transaction ([CollectionMerge.merge]),
 * writes the result to the account, and puts it back into Room. Combining instead of replacing is what makes
 * it safe: a phone that is behind can add to the account but never remove anything it didn't delete itself.
 *
 * Stored on `users/{uid}.collection` (the owner-only doc the rest of the account lives on).
 */
class CollectionSync(
    context: Context,
    private val db: TeamDatabase,
    private val teamDao: TeamDao,
    private val favouritesDao: FavouritesDao,
    private val savedTeams: SavedTeamsRepository,
) {
    private val store = context.collectionStore
    private val snapshotKey = stringPreferencesKey("snapshot")
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private var pendingSync: Job? = null

    private val auth get() = FirebaseAuth.getInstance()
    private val firestore get() = FirebaseFirestore.getInstance()

    init {
        // Every change the player makes, wherever it comes from, lands here.
        scope.launch {
            combine(
                listOf(teamDao.getAllTeamsWithMembers(), favouritesDao.getAllFavorites()) +
                    TeamFormat.entries.map { savedTeams.teams(it) }
            ) { }
                .collect {
                    val changed = lock.withLock { recordLocalChanges() }
                    if (changed) scheduleSync()
                }
        }
    }

    /** Pulls the account's teams and favourites and publishes this phone's; safe to call any time. */
    suspend fun sync(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return lock.withLock { syncLocked(uid) }
    }

    /**
     * Signing out: publish anything not yet on the account, then empty this phone so the next account starts
     * clean. If the publish fails the phone keeps its teams, since they would otherwise exist nowhere.
     */
    suspend fun flushAndClear() {
        pendingSync?.cancel()
        val uid = auth.currentUser?.uid
        lock.withLock {
            val published = uid != null && syncLocked(uid)
            if (!published) return
            db.withTransaction {
                teamDao.deleteAllMembers()
                teamDao.deleteAllTeams()
                favouritesDao.clearAllFavorites()
                teamDao.createTeam(TeamEntity(teamId = UUID.randomUUID().toString(), teamName = "My Team", isDefault = true))
            }
            TeamFormat.entries.forEach { savedTeams.replaceAll(it, emptyList()) }
            store.edit { it.clear() }
        }
    }

    // ── Internals (call with [lock] held) ──────────────────────────────────────

    private fun scheduleSync() {
        if (auth.currentUser == null) return
        pendingSync?.cancel()
        pendingSync = scope.launch {
            delay(SYNC_DEBOUNCE_MS)
            sync()
        }
    }

    /** Updates the stored snapshot from Room; true when the player changed something. */
    private suspend fun recordLocalChanges(): Boolean {
        val previous = loadSnapshot()
        val next = track(previous)
        if (next == previous) return false
        saveSnapshot(next)
        return true
    }

    private suspend fun track(previous: CollectionState?): CollectionState {
        val l = readLocal()
        return CollectionMerge.track(previous, l.teams, l.favourites, System.currentTimeMillis(), l.battleTeams)
    }

    private suspend fun syncLocked(uid: String): Boolean {
        val local = track(loadSnapshot())
        val ref = firestore.collection("users").document(uid)
        var txSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null
        val merged = try {
            firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                txSnapshot = snap
                val cloud = decode(snap.get(FIELD))
                val merged = CollectionMerge.merge(local, cloud, System.currentTimeMillis())
                tx.set(ref, mapOf(FIELD to encode(merged)), SetOptions.merge())
                merged
            }.await()
        } catch (e: Exception) {
            Log.w(TAG, "sync failed: ${e.message}")
            saveSnapshot(local)
            return false
        }
        txSnapshot?.let { com.aditya1875.pokeverse.utils.UserDocCache.remember(uid, it) }
        // The player may have changed something while the transaction ran: fold that in rather than drop it.
        val room = readLocal()
        val sinceStart = CollectionMerge.track(local, room.teams, room.favourites, System.currentTimeMillis(), room.battleTeams)
        val final = if (sinceStart == local) merged else CollectionMerge.merge(sinceStart, merged, System.currentTimeMillis())
        saveSnapshot(final)
        if (!CollectionMerge.sameContent(final, room)) writeLocal(final)
        if (final != merged) scheduleSync()
        return true
    }

    /** Everything this phone holds now (change times unknown: [CollectionMerge.track] fills them in). */
    private suspend fun readLocal(): CollectionState {
        val members = teamDao.getAllMembersOnce().groupBy { it.teamId }
        val teams = teamDao.getAllTeamsOnce().map { t ->
            SyncTeam(
                id = t.teamId, name = t.teamName, createdAt = t.createdAt, isDefault = t.isDefault,
                members = members[t.teamId].orEmpty().map { SyncMember(it.name, it.imageUrl, it.addedAt) },
                updatedAt = 0L,
            )
        }
        val favs = favouritesDao.getAllFavoritesOnce().map { SyncFav(it.name, it.imageUrl, it.addedAt) }
        val battle = TeamFormat.entries.flatMap { f ->
            savedTeams.teams(f).value.map { SyncBattleTeam(f.name, it.name, it.ids, 0L) }
        }
        return CollectionState(teams, favs, battle)
    }

    private suspend fun writeLocal(state: CollectionState) {
        TeamFormat.entries.forEach { f ->
            savedTeams.replaceAll(f, state.battleTeams.filter { it.format == f.name }.map { SavedTeam(it.name, it.ids) })
        }
        db.withTransaction {
            teamDao.deleteAllMembers()
            teamDao.deleteAllTeams()
            favouritesDao.clearAllFavorites()
            teamDao.insertTeams(state.teams.map { TeamEntity(it.id, it.name, it.createdAt, it.isDefault) })
            teamDao.insertMembers(state.teams.flatMap { t ->
                t.members.take(CollectionMerge.MAX_MEMBERS).map { m ->
                    TeamMemberEntity(teamId = t.id, name = m.name, imageUrl = m.imageUrl, addedAt = m.addedAt)
                }
            })
            favouritesDao.insertFavorites(state.favourites.map { FavouriteEntity(it.name, it.imageUrl, it.addedAt) })
        }
    }

    private suspend fun loadSnapshot(): CollectionState? =
        store.data.first()[snapshotKey]?.let { runCatching { gson.fromJson(it, CollectionState::class.java) }.getOrNull() }
            // Gson ignores Kotlin defaults: a snapshot written before a list existed reads it as null.
            ?.let { s ->
                @Suppress("USELESS_ELVIS", "SENSELESS_COMPARISON")
                s.copy(
                    teams = s.teams ?: emptyList(),
                    favourites = s.favourites ?: emptyList(),
                    battleTeams = s.battleTeams ?: emptyList(),
                    deleted = s.deleted ?: emptyMap(),
                )
            }

    private suspend fun saveSnapshot(state: CollectionState) {
        store.edit { it[snapshotKey] = gson.toJson(state) }
    }

    private fun encode(s: CollectionState): Map<String, Any> = mapOf(
        "teams" to s.teams.map { t ->
            mapOf(
                "id" to t.id, "name" to t.name.take(40), "createdAt" to t.createdAt, "isDefault" to t.isDefault,
                "updatedAt" to t.updatedAt,
                "members" to t.members.take(CollectionMerge.MAX_MEMBERS).map { m ->
                    mapOf("name" to m.name, "imageUrl" to m.imageUrl, "addedAt" to m.addedAt)
                },
            )
        },
        "favourites" to s.favourites.map { f -> mapOf("name" to f.name, "imageUrl" to f.imageUrl, "addedAt" to f.addedAt) },
        "battleTeams" to s.battleTeams.map { t ->
            mapOf("format" to t.format, "name" to t.name, "ids" to t.ids, "updatedAt" to t.updatedAt)
        },
        "deleted" to s.deleted,
    )

    private fun decode(raw: Any?): CollectionState {
        val m = raw as? Map<*, *> ?: return CollectionState()
        fun Map<*, *>.str(k: String) = this[k] as? String ?: ""
        fun Map<*, *>.long(k: String) = (this[k] as? Number)?.toLong() ?: 0L
        val teams = (m["teams"] as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }.mapNotNull { t ->
            val id = t.str("id").ifEmpty { return@mapNotNull null }
            SyncTeam(
                id = id, name = t.str("name"), createdAt = t.long("createdAt"), isDefault = t["isDefault"] == true,
                members = (t["members"] as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }
                    .map { SyncMember(it.str("name"), it.str("imageUrl"), it.long("addedAt")) }
                    .filter { it.name.isNotEmpty() },
                updatedAt = t.long("updatedAt"),
            )
        }
        val favs = (m["favourites"] as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }
            .map { SyncFav(it.str("name"), it.str("imageUrl"), it.long("addedAt")) }
            .filter { it.name.isNotEmpty() }
        val deleted = (m["deleted"] as? Map<*, *>).orEmpty().entries
            .mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? Number)?.toLong()?.let { key to it } } }
            .toMap()
        val known = TeamFormat.entries.map { it.name }.toSet()
        val battle = (m["battleTeams"] as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }.mapNotNull { t ->
            val ids = (t["ids"] as? List<*>).orEmpty().mapNotNull { (it as? Number)?.toInt() }
            SyncBattleTeam(t.str("format"), t.str("name"), ids, t.long("updatedAt"))
                .takeIf { it.format in known && it.name.isNotEmpty() && ids.isNotEmpty() }
        }
        return CollectionState(teams, favs, battle, deleted)
    }

    private companion object {
        const val TAG = "CollectionSync"
        const val FIELD = "collection"
        const val SYNC_DEBOUNCE_MS = 3_000L
    }
}
