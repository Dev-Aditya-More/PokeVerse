package com.aditya1875.pokeverse.feature.leaderboard.data.repository

import com.aditya1875.pokeverse.feature.leaderboard.data.remote.model.LeaderboardEntry
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
    private var cachedEntries: List<LeaderboardEntry> = emptyList()
    private var lastDocument: DocumentSnapshot? = null
    private var cacheTimestamp: Long = 0L
    private val CACHE_TTL_MS = 5 * 60 * 1000L

    suspend fun getLeaderboard(forceRefresh: Boolean = false): LeaderboardState {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedEntries.isNotEmpty() && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return buildState(cachedEntries, canLoadMore = cachedEntries.size.toLong() == PAGE_SIZE)
        }

        return try {
            val snapshot = firestore.collection("leaderboard")
                .orderBy("totalXp", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .get().await()

            val entries = snapshot.documents.mapIndexedNotNull { index, doc ->
                doc.toLeaderboardEntry(rank = index + 1)
            }

            cachedEntries = entries
            lastDocument = snapshot.documents.lastOrNull()
            cacheTimestamp = now

            buildState(entries, canLoadMore = entries.size.toLong() == PAGE_SIZE)
        } catch (e: Exception) {
            LeaderboardState.Error(e.message ?: "Failed to load leaderboard")
        }
    }

    // ── Load next page (pagination) ───────────────────────────────────────────
    suspend fun loadNextPage(): LeaderboardState {
        val cursor = lastDocument ?: return LeaderboardState.Error("No more pages")
        return try {
            val snapshot = firestore.collection("leaderboard")
                .orderBy("totalXp", Query.Direction.DESCENDING)
                .startAfter(cursor)
                .limit(PAGE_SIZE)
                .get().await()

            val newEntries = snapshot.documents.mapIndexedNotNull { index, doc ->
                doc.toLeaderboardEntry(rank = cachedEntries.size + index + 1)
            }

            cachedEntries = cachedEntries + newEntries
            lastDocument = snapshot.documents.lastOrNull()

            buildState(cachedEntries, canLoadMore = newEntries.size.toLong() == PAGE_SIZE)
        } catch (e: Exception) {
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

    private fun buildState(
        entries: List<LeaderboardEntry>,
        canLoadMore: Boolean
    ): LeaderboardState {
        val uid = auth.currentUser?.uid
        val userInList = entries.find { it.uid == uid }
        return LeaderboardState.Success(
            entries = entries,
            userEntry = userInList,
            userRank = userInList?.rank ?: 0,
            canLoadMore = canLoadMore
        )
    }

    private fun DocumentSnapshot.toLeaderboardEntry(rank: Int) = LeaderboardEntry(
        uid = getString("uid") ?: id,
        displayName = getString("displayName") ?: "Trainer",
        photoUrl = getString("photoUrl") ?: "",
        totalXp = (getLong("totalXp") ?: 0L).toInt(),
        level = (getLong("level") ?: 1L).toInt(),
        rank = rank,
    )

    fun invalidateCache() {
        cachedEntries = emptyList()
        lastDocument = null
        cacheTimestamp = 0L
    }
}