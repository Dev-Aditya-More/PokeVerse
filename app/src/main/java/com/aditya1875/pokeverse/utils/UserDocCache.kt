package com.aditya1875.pokeverse.utils

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * One in-memory copy of the player's `users/{uid}` document, shared by everything that pulls from it (profile,
 * Pokédex, Dex Daily, wallet). They all run at launch and right after sign-in, and each used to fetch the same
 * document from the server on its own. Now concurrent callers wait on one request, and anything asking again
 * within [FRESH_MS] gets the copy already in memory.
 *
 * Only for merges, which are safe with a copy a little behind: every merge keeps the larger counter or the
 * union, and the profile and team writes re-read the server inside their own transactions anyway (their reads
 * are fed back in here through [remember]).
 */
object UserDocCache {
    const val FRESH_MS = 120_000L

    private val lock = Mutex()
    private var uid: String? = null
    private var snapshot: DocumentSnapshot? = null
    private var fetchedAt = 0L
    private var inFlight: CompletableDeferred<DocumentSnapshot>? = null

    /** The document, from memory when fetched within [maxAgeMs]; throws like `get()` when it can't be read. */
    suspend fun read(uid: String, maxAgeMs: Long = FRESH_MS): DocumentSnapshot {
        val (pending, owner) = lock.withLock {
            val cached = snapshot
            if (this.uid == uid && cached != null && System.currentTimeMillis() - fetchedAt < maxAgeMs) return cached
            val running = inFlight?.takeIf { this.uid == uid }
            if (running != null) running to false
            else {
                this.uid = uid
                CompletableDeferred<DocumentSnapshot>().also { inFlight = it } to true
            }
        }
        if (!owner) return pending.await()
        return try {
            val snap = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
            remember(uid, snap)
            pending.complete(snap)
            snap
        } catch (e: Throwable) {
            pending.completeExceptionally(e)
            throw e
        } finally {
            lock.withLock { if (inFlight === pending) inFlight = null }
        }
    }

    /** Stores a copy read elsewhere (e.g. inside a transaction), so the next pull needn't fetch it again. */
    suspend fun remember(uid: String, snap: DocumentSnapshot) = lock.withLock {
        this.uid = uid
        snapshot = snap
        fetchedAt = System.currentTimeMillis()
    }

    /** Sign-out: the next account starts with nothing cached. */
    suspend fun clear() = lock.withLock {
        uid = null; snapshot = null; fetchedAt = 0L; inFlight = null
    }
}
