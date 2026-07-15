package com.aditya1875.pokeverse.feature.friends.data.repository

import com.aditya1875.pokeverse.feature.friends.data.model.FriendProfile
import com.aditya1875.pokeverse.feature.friends.data.model.FriendRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore layout:
 *  - friend_requests/{fromUid}_{toUid}  — pending requests; deleted on accept/decline/cancel
 *  - friendships/{pairId}               — pairId is the two uids sorted + joined with "_";
 *                                         `members` array enables whereArrayContains queries
 *  - Trainer search reads the public `leaderboard` collection (displayNameLower prefix)
 */
class FriendsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val requests get() = firestore.collection("friend_requests")
    private val friendships get() = firestore.collection("friendships")
    private val leaderboard get() = firestore.collection("leaderboard")

    val currentUid: String? get() = auth.currentUser?.uid

    private fun pairId(a: String, b: String) =
        if (a < b) "${a}_$b" else "${b}_$a"

    // ── Live friends list ──────────────────────────────────────────────────────
    fun friendUidsFlow(): Flow<List<String>> = callbackFlow {
        val uid = currentUid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg: ListenerRegistration = friendships
            .whereArrayContains("members", uid)
            .addSnapshotListener { snap, _ ->
                val uids = snap?.documents
                    ?.mapNotNull { doc ->
                        @Suppress("UNCHECKED_CAST")
                        (doc.get("members") as? List<String>)?.firstOrNull { it != uid }
                    }
                    ?: emptyList()
                trySend(uids)
            }
        awaitClose { reg.remove() }
    }

    // ── Live incoming / outgoing pending requests ─────────────────────────────
    fun incomingRequestsFlow(): Flow<List<FriendRequest>> = requestsFlow("toUid")

    fun outgoingRequestsFlow(): Flow<List<FriendRequest>> = requestsFlow("fromUid")

    private fun requestsFlow(field: String): Flow<List<FriendRequest>> = callbackFlow {
        val uid = currentUid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg = requests
            .whereEqualTo(field, uid)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents
                    ?.map { it.toFriendRequest() }
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    // ── Actions ────────────────────────────────────────────────────────────────
    suspend fun sendRequest(to: FriendProfile, fromName: String, fromPhotoUrl: String, fromLevel: Int) {
        val uid = currentUid ?: return
        if (to.uid == uid) return
        requests.document("${uid}_${to.uid}").set(
            mapOf(
                "fromUid" to uid,
                "fromName" to fromName,
                "fromPhotoUrl" to fromPhotoUrl,
                "fromLevel" to fromLevel,
                "toUid" to to.uid,
                "toName" to to.displayName,
                "toPhotoUrl" to to.photoUrl,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun cancelRequest(toUid: String) {
        val uid = currentUid ?: return
        requests.document("${uid}_$toUid").delete().await()
    }

    suspend fun acceptRequest(request: FriendRequest) {
        val uid = currentUid ?: return
        val batch = firestore.batch()
        batch.set(
            friendships.document(pairId(uid, request.fromUid)),
            mapOf(
                "members" to listOf(uid, request.fromUid).sorted(),
                "createdAt" to System.currentTimeMillis()
            )
        )
        batch.delete(requests.document(request.id))
        batch.commit().await()
    }

    suspend fun declineRequest(request: FriendRequest) {
        requests.document(request.id).delete().await()
    }

    suspend fun removeFriend(friendUid: String) {
        val uid = currentUid ?: return
        friendships.document(pairId(uid, friendUid)).delete().await()
    }

    // ── Trainer search (prefix on displayNameLower in leaderboard docs) ───────
    suspend fun searchTrainers(query: String, limit: Long = 20): List<FriendProfile> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        return try {
            val snap = leaderboard
                .whereGreaterThanOrEqualTo("displayNameLower", q)
                .whereLessThanOrEqualTo("displayNameLower", q + "\uf8ff")
                .limit(limit)
                .get().await()
            snap.documents.map { it.toFriendProfile() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Batch-fetch public profiles for a set of uids ──────────────────────────
    suspend fun getProfiles(uids: List<String>): List<FriendProfile> {
        if (uids.isEmpty()) return emptyList()
        return try {
            uids.chunked(10).flatMap { chunk ->
                leaderboard
                    .whereIn(FieldPath.documentId(), chunk)
                    .get().await()
                    .documents.map { it.toFriendProfile() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun DocumentSnapshot.toFriendProfile() = FriendProfile(
        uid = getString("uid") ?: id,
        displayName = getString("displayName") ?: "Trainer",
        photoUrl = getString("photoUrl") ?: "",
        level = (getLong("level") ?: 1L).toInt(),
        totalXp = (getLong("totalXp") ?: 0L).toInt(),
        weeklyXp = (getLong("weeklyXp") ?: 0L).toInt()
    )

    private fun DocumentSnapshot.toFriendRequest() = FriendRequest(
        id = id,
        fromUid = getString("fromUid") ?: "",
        fromName = getString("fromName") ?: "Trainer",
        fromPhotoUrl = getString("fromPhotoUrl") ?: "",
        fromLevel = (getLong("fromLevel") ?: 1L).toInt(),
        toUid = getString("toUid") ?: "",
        toName = getString("toName") ?: "Trainer",
        toPhotoUrl = getString("toPhotoUrl") ?: "",
        createdAt = getLong("createdAt") ?: 0L
    )
}
