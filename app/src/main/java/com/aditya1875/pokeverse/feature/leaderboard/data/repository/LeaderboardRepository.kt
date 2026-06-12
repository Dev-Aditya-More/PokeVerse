package com.aditya1875.pokeverse.feature.leaderboard.data.repository

import com.aditya1875.pokeverse.feature.leaderboard.data.remote.model.LeaderboardEntry
import com.aditya1875.pokeverse.feature.leaderboard.presentation.viewmodels.LeaderboardType
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

sealed class LeaderboardState {
    object Loading : LeaderboardState()
    data class Success(
        val entries: List<LeaderboardEntry>,
        val userEntry: LeaderboardEntry?,
        val userRank: Int,
        val canLoadMore: Boolean
    ) : LeaderboardState()

    data class Error(val message: String) : LeaderboardState()
}

class LeaderboardRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val PAGE_SIZE = 50L

    // ── In-memory cache (lives for the session) ───────────────────────────────
    private val cache = mutableMapOf<LeaderboardType, List<LeaderboardEntry>>()
    private val lastDocs = mutableMapOf<LeaderboardType, DocumentSnapshot?>()
    private val cacheTimestamps = mutableMapOf<LeaderboardType, Long>()
    private var lastDocument: DocumentSnapshot? = null
    private val CACHE_TTL_MS = 5 * 60 * 1000L

    suspend fun getLeaderboard(
        type: LeaderboardType = LeaderboardType.GLOBAL,
        forceRefresh: Boolean = false
    ): LeaderboardState {

        val cachedEntries = cache[type] ?: emptyList()
        val cacheTimestamp = cacheTimestamps[type] ?: 0L
        val now = System.currentTimeMillis()

        if (!forceRefresh && cachedEntries.isNotEmpty() && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return buildState(cachedEntries, canLoadMore = cachedEntries.size.toLong() == PAGE_SIZE)
        }

        return try {
            val snapshot = when (type) {
                LeaderboardType.LAST_WEEK -> return LeaderboardState.Error("Use getLastWeekSnapshot()")
                LeaderboardType.GLOBAL -> {
                    firestore.collection("leaderboard")
                        .orderBy("totalXp", Query.Direction.DESCENDING)
                        .orderBy("updatedAt", Query.Direction.ASCENDING)
                        .limit(PAGE_SIZE)
                        .get().await()
                }
                LeaderboardType.WEEKLY -> {
                    // Primary: only weeklyActive players
                    // updatedAt direction matches the existing Firestore composite index
                    // (weeklyActive ASC, weeklyXp DESC, updatedAt DESC)
                    val primary = try {
                        firestore.collection("leaderboard")
                            .whereEqualTo("weeklyActive", true)
                            .orderBy("weeklyXp", Query.Direction.DESCENDING)
                            .orderBy("updatedAt", Query.Direction.DESCENDING)
                            .limit(PAGE_SIZE)
                            .get().await()
                    } catch (e: Exception) {
                        null
                    }

                    // Fallback: weeklyActive flag may not be set yet on older docs
                    if (primary == null || primary.isEmpty) {
                        firestore.collection("leaderboard")
                            .whereGreaterThan("weeklyXp", 0)
                            .orderBy("weeklyXp", Query.Direction.DESCENDING)
                            .orderBy("updatedAt", Query.Direction.ASCENDING)
                            .limit(PAGE_SIZE)
                            .get().await()
                    } else primary
                }
            }

            if (snapshot.isEmpty) {
                return LeaderboardState.Success(
                    entries = emptyList(),
                    userEntry = null,
                    userRank = 0,
                    canLoadMore = false
                )
            }

            val entries = snapshot.documents.mapIndexedNotNull { index, doc ->
                doc.toLeaderboardEntry(rank = index + 1)
            }

            cache[type] = entries
            lastDocs[type] = snapshot.documents.lastOrNull()
            cacheTimestamps[type] = now

            buildState(entries, canLoadMore = entries.size.toLong() == PAGE_SIZE)

        } catch (e: Exception) {
            e.printStackTrace()
            val cached = cache[type]
            if (!cached.isNullOrEmpty()) {
                buildState(cached, canLoadMore = cached.size.toLong() == PAGE_SIZE)
            } else {
                LeaderboardState.Error(e.message ?: "Failed to load leaderboard")
            }
        }
    }

    // ── Load next page (pagination) ───────────────────────────────────────────
    suspend fun loadNextPage(
        type: LeaderboardType = LeaderboardType.GLOBAL
    ): LeaderboardState {
        val cursor = lastDocs[type] ?: return LeaderboardState.Error("No more pages")
        val cachedEntries = cache[type] ?: emptyList()

        return try {
            // Query must exactly mirror the initial load query for startAfter cursor to work
            val snapshot = when (type) {
                LeaderboardType.LAST_WEEK -> return LeaderboardState.Error("No pagination for last week")
                LeaderboardType.GLOBAL -> firestore.collection("leaderboard")
                    .orderBy("totalXp", Query.Direction.DESCENDING)
                    .orderBy("updatedAt", Query.Direction.ASCENDING)
                    .startAfter(cursor)
                    .limit(PAGE_SIZE)
                    .get().await()
                LeaderboardType.WEEKLY -> firestore.collection("leaderboard")
                    .whereEqualTo("weeklyActive", true)
                    .orderBy("weeklyXp", Query.Direction.DESCENDING)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .startAfter(cursor)
                    .limit(PAGE_SIZE)
                    .get().await()
            }

            val newEntries = snapshot.documents.mapIndexedNotNull { index, doc ->
                doc.toLeaderboardEntry(rank = cachedEntries.size + index + 1)
            }

            val updated = cachedEntries + newEntries
            cache[type] = updated
            lastDocs[type] = snapshot.documents.lastOrNull()

            buildState(updated, canLoadMore = newEntries.size.toLong() == PAGE_SIZE)
        } catch (e: Exception) {
            e.printStackTrace()
            LeaderboardState.Error(e.message ?: "Failed to load more")
        }
    }

    // ── Fetch current user's own leaderboard entry + rank ─────────────────────
    // Used to pin the user row even if they're outside top-50
    suspend fun getUserEntry(): LeaderboardEntry? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val doc = firestore.collection("leaderboard").document(uid).get().await()
            if (!doc.exists()) return null
            // rank field is written by Cloud Function
            doc.toLeaderboardEntry(rank = (doc.getLong("rank") ?: 0L).toInt())
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun buildState(
        entries: List<LeaderboardEntry>,
        canLoadMore: Boolean
    ): LeaderboardState {
        val uid = auth.currentUser?.uid
        val userInList = entries.find { it.uid == uid }
        // Fetch user's own doc from Firestore when outside the loaded page so
        // the sticky rank banner always has a rank to display.
        val userEntry = userInList ?: if (uid != null) {
            try { getUserEntry() } catch (_: Exception) { null }
        } else null
        return LeaderboardState.Success(
            entries = entries,
            userEntry = userEntry,
            userRank = userEntry?.rank ?: 0,
            canLoadMore = canLoadMore
        )
    }

    private fun DocumentSnapshot.toLeaderboardEntry(rank: Int) = LeaderboardEntry(
        uid = getString("uid") ?: id,
        displayName = getString("displayName") ?: "Trainer",
        email = getString("email") ?: "trainer@dexverse",
        photoUrl = getString("photoUrl") ?: "",
        totalXp = (getLong("totalXp") ?: 0L).toInt(),
        weeklyXp = (getLong("weeklyXp") ?: 0L).toInt(),
        level = (getLong("level") ?: 1L).toInt(),
        rank = rank,
        previousRank = (getLong("previousRank") ?: 0L).toInt(),
        // Cloud function writes this as Firestore Timestamp; Android app writes it as Long.
        // Handle both to avoid RuntimeException from getLong() on a Timestamp field.
        lastWeeklyReset = when (val raw = get("lastWeeklyReset")) {
            is Long -> raw
            is Timestamp -> raw.toDate().time
            else -> null
        }
    )

    fun invalidateCache(type: LeaderboardType = LeaderboardType.GLOBAL) {
        cache[type] = emptyList()
        lastDocs[type] = null
        cacheTimestamps[type] = 0L
    }

    suspend fun saveLastWeekSnapshot(weekOf: Long, entries: List<LeaderboardEntry>) {
        try {
            val docRef = firestore.collection("leaderboard_meta").document("last_week_snapshot")
            val existing = docRef.get().await()

            if (existing.exists()) {
                val existingWeekOf = when (val v = existing.get("weekOf")) {
                    is Long -> v
                    is Timestamp -> v.toDate().time
                    else -> 0L
                }
                if (existingWeekOf == weekOf) return
            }

            val top10 = entries.take(10).mapIndexed { idx, e ->
                mapOf(
                    "uid" to e.uid,
                    "displayName" to e.displayName,
                    "photoUrl" to e.photoUrl,
                    "weeklyXp" to e.weeklyXp,
                    "level" to e.level,
                    "rank" to (idx + 1)
                )
            }

            docRef.set(mapOf("weekOf" to weekOf, "entries" to top10)).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getLastWeekSnapshot(): Pair<Long, List<LeaderboardEntry>> {
        return try {
            val doc = firestore.collection("leaderboard_meta")
                .document("last_week_snapshot")
                .get().await()

            if (!doc.exists()) return 0L to emptyList()

            val weekOf = when (val v = doc.get("weekOf")) {
                is Long -> v
                is Timestamp -> v.toDate().time
                else -> 0L
            }

            @Suppress("UNCHECKED_CAST")
            val rawEntries = doc.get("entries") as? List<Map<String, Any>> ?: emptyList()

            val entries = rawEntries.mapIndexed { idx, m ->
                LeaderboardEntry(
                    uid = m["uid"] as? String ?: "",
                    displayName = m["displayName"] as? String ?: "Trainer",
                    photoUrl = m["photoUrl"] as? String ?: "",
                    weeklyXp = when (val v = m["weeklyXp"]) {
                        is Long -> v.toInt()
                        is Int -> v
                        else -> 0
                    },
                    level = when (val v = m["level"]) {
                        is Long -> v.toInt()
                        is Int -> v
                        else -> 1
                    },
                    rank = when (val v = m["rank"]) {
                        is Long -> v.toInt()
                        is Int -> v
                        else -> idx + 1
                    }
                )
            }

            weekOf to entries
        } catch (e: Exception) {
            e.printStackTrace()
            0L to emptyList()
        }
    }
}