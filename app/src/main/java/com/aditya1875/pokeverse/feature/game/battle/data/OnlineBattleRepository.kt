package com.aditya1875.pokeverse.feature.game.battle.data

import com.aditya1875.pokeverse.feature.game.battle.domain.online.ActionLog
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineAction
import com.aditya1875.pokeverse.feature.game.battle.domain.online.Seat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.aditya1875.pokeverse.feature.game.core.domain.Matchmaking
import com.aditya1875.pokeverse.feature.game.core.domain.YieldResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/** The room document, as the app reads it. */
data class OnlineMatch(
    val id: String,
    val hostId: String,
    val guestId: String,
    val hostName: String,
    val guestName: String,
    val status: String,
    val roomCode: String,
    val mode: String,
    val seed: Long,
    val heartbeatHostMs: Long,
    val heartbeatGuestMs: Long,
    val quitSeat: Seat?,
    val abandonedSeat: Seat?,
    /** Each seat's latest reaction as (emoji index, counter); the counter changing is what shows a new one. */
    val reactHost: Pair<Int, Long>? = null,
    val reactGuest: Pair<Int, Long>? = null,
) {
    fun reactionOf(seat: Seat) = if (seat == Seat.HOST) reactHost else reactGuest

    val hasGuest: Boolean get() = guestId.isNotEmpty()
    val finished: Boolean get() = status == STATUS_FINISHED

    fun nameOf(seat: Seat) = if (seat == Seat.HOST) hostName else guestName
    fun heartbeatOf(seat: Seat) = if (seat == Seat.HOST) heartbeatHostMs else heartbeatGuestMs

    companion object {
        const val STATUS_WAITING = "waiting"
        const val STATUS_ACTIVE = "active"
        const val STATUS_FINISHED = "finished"
        const val MODE_QUICK = "quick"
        const val MODE_FRIEND = "friend"
    }
}

/** Room, both teams (as Pokémon ids, by seat) and every recorded action, kept up to date live. */
data class OnlineSnapshot(
    val match: OnlineMatch,
    val teams: Map<Seat, List<Int>>,
    val actions: ActionLog,
)

interface OnlineBattleRepository {
    /** Opens a room and waits in it as host. [mode] is [OnlineMatch.MODE_QUICK] or [OnlineMatch.MODE_FRIEND]. */
    suspend fun createRoom(uid: String, name: String, mode: String, team: List<Int>): OnlineMatch

    /** Joins the freshest open quick room, or returns null when there is none. */
    suspend fun joinQuick(uid: String, name: String, team: List<Int>): String?

    /** Joins a friend's room by its code, or returns null when the code is wrong or the room is taken. */
    suspend fun joinByCode(code: String, uid: String, name: String, team: List<Int>): String?

    /** How many OTHER players are waiting for a quick match right now, kept live. Never throws. */
    fun observeQuickPool(uid: String, teamSize: Int): Flow<Int>

    /**
     * Another waiting player this one should go and join, or null to keep waiting. Two players who tapped
     * "play" together each opened a room and never saw the other's; this is how they find each other
     * (see [com.aditya1875.pokeverse.feature.game.core.domain.Matchmaking]).
     */
    suspend fun findRoomToYieldTo(mine: String, uid: String, teamSize: Int): String?

    /** Gives up [mine] and takes the seat in [other], in that order so nobody ends up in two rooms. */
    suspend fun yieldToRoom(mine: String, other: String, uid: String, name: String, team: List<Int>): YieldResult

    /** Records this seat's choice for [step]. A second write for the same step is ignored: choices are final. */
    suspend fun submitAction(matchId: String, uid: String, seat: Seat, step: Int, action: OnlineAction)

    suspend fun heartbeat(matchId: String, seat: Seat)

    /** Shows reaction [index] on the rival's screen; [counter] grows with each one so repeats still show. */
    suspend fun react(matchId: String, seat: Seat, index: Int, counter: Long) {}

    /**
     * Closes a room nobody joined. Returns false (and changes nothing) if someone joined in the meantime,
     * so the caller lets that real match carry on instead of stomping it.
     */
    suspend fun cancelWaitingIfUnjoined(matchId: String): Boolean

    /** Marks a battle that ended normally, so the room stops being matched into and gets cleaned up. */
    suspend fun finish(matchId: String)

    /** This seat leaves mid-battle: the other seat wins. Only ever writes the caller's own seat. */
    suspend fun quit(matchId: String, seat: Seat)

    /**
     * Claims the win because [gone] stopped answering. The server only accepts this when [gone]'s last
     * heartbeat really is that old, so it cannot be used against someone who is still playing.
     */
    suspend fun claimAbandoned(matchId: String, gone: Seat)

    fun observe(matchId: String): Flow<OnlineSnapshot>
}

/** A friend's room needs [required] Pokémon, and the joiner brought a different number. */
class TeamSizeMismatch(val required: Int) : Exception("room needs $required Pokémon")


class FirestoreOnlineBattleRepository : OnlineBattleRepository {

    private val firestore get() = FirebaseFirestore.getInstance()
    private val matches get() = firestore.collection(COLLECTION)

    companion object {
        const val COLLECTION = "team_battles"
        /** A quick room older than this was abandoned; never match into it. Above the 30s queue wait on purpose. */
        private const val STALE_ROOM_MS = 40_000L

        /**
         * The battle-rules version this build plays. Both phones replay the same log through the engine, so they must
         * be on the SAME rules: 2 adds Guards that ride along with a move. A room carries the version it was opened
         * under, a phone only matches rooms on its own, and the server refuses a join across versions (firestore.rules),
         * so an older app and this one can never be paired into a battle they would compute differently.
         */
        const val RULES_VERSION = 2
        /** Most open rooms read at once: plenty for matching and for the "players waiting" count. */
        private const val POOL_LIMIT = 50
        private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val KIND_READY = "ready"
    }

    override suspend fun createRoom(uid: String, name: String, mode: String, team: List<Int>): OnlineMatch {
        val doc = matches.document()
        val code = (1..6).map { CODE_CHARS.random() }.joinToString("")
        val seed = Random.nextLong(1L, 1L shl 52)
        doc.set(
            mapOf(
                "hostId" to uid,
                "guestId" to "",
                "hostName" to name,
                "guestName" to "",
                "status" to OnlineMatch.STATUS_WAITING,
                "roomCode" to code,
                "mode" to mode,
                // How many Pokémon each side brings. The joiner must match it (see the rules and joinByCode).
                "teamSize" to team.size,
                "rules" to RULES_VERSION,
                "seed" to seed,
                "heartbeatHostAt" to FieldValue.serverTimestamp(),
                "heartbeatGuestAt" to FieldValue.serverTimestamp(),
                "createdAt" to Timestamp.now(),
                "lastUpdated" to Timestamp.now(),
            )
        ).await()
        saveTeam(doc.id, uid, team)
        return OnlineMatch(doc.id, uid, "", name, "", OnlineMatch.STATUS_WAITING, code, mode, seed, 0L, 0L, null, null)
    }

    /** Open quick rooms for [teamSize]: equality-only filters, so no composite index is needed (same as Card Clash). */
    private fun openQuickQuery(teamSize: Int) = matches
        .whereEqualTo("status", OnlineMatch.STATUS_WAITING)
        .whereEqualTo("mode", OnlineMatch.MODE_QUICK)
        .whereEqualTo("guestId", "")
        // Quick matches only pair teams of the same size, so nobody faces more Pokémon than they brought.
        .whereEqualTo("teamSize", teamSize)
        // Only rooms on this build's battle rules: a room from an older version could never be played correctly.
        .whereEqualTo("rules", RULES_VERSION)
        .limit(POOL_LIMIT.toLong())

    private fun rooms(docs: List<com.google.firebase.firestore.DocumentSnapshot>): List<Matchmaking.OpenRoom> =
        docs.map {
            Matchmaking.OpenRoom(
                id = it.id,
                hostId = it.getString("hostId").orEmpty(),
                createdAtMs = it.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
            )
        }

    override suspend fun joinQuick(uid: String, name: String, team: List<Int>): String? {
        val open = openQuickQuery(team.size).get().await()

        // A host that backgrounded or crashed leaves a room nobody will ever answer in: those are skipped.
        val live = Matchmaking.live(rooms(open.documents), uid, System.currentTimeMillis(), STALE_ROOM_MS)
        // Newest first, as before; every candidate is tried, so losing one race still finds the next room.
        for (candidate in live.sortedByDescending { it.createdAtMs }) {
            if (claimSeat(candidate.id, uid, name)) {
                saveTeam(candidate.id, uid, team)
                return candidate.id
            }
        }
        return null
    }

    override fun observeQuickPool(uid: String, teamSize: Int): Flow<Int> = callbackFlow {
        val reg = openQuickQuery(teamSize).addSnapshotListener { snap, error ->
            if (error != null || snap == null) return@addSnapshotListener
            trySend(Matchmaking.waitingCount(rooms(snap.documents), uid, System.currentTimeMillis(), STALE_ROOM_MS))
        }
        awaitClose { reg.remove() }
    }

    override suspend fun findRoomToYieldTo(mine: String, uid: String, teamSize: Int): String? {
        val open = runCatching { openQuickQuery(teamSize).get().await() }.getOrNull() ?: return null
        val live = Matchmaking.live(rooms(open.documents), uid, System.currentTimeMillis(), STALE_ROOM_MS)
        return Matchmaking.roomToYieldTo(mine, live)
    }

    override suspend fun yieldToRoom(mine: String, other: String, uid: String, name: String, team: List<Int>): YieldResult {
        // Close our own room FIRST. If someone joined it in the meantime this returns false, and that real
        // match wins: we never hold a seat in two rooms.
        if (!cancelWaitingIfUnjoined(mine)) return YieldResult.STAYED
        if (claimSeat(other, uid, name)) {
            saveTeam(other, uid, team)
            return YieldResult.JOINED
        }
        return YieldResult.LOST
    }

    override suspend fun joinByCode(code: String, uid: String, name: String, team: List<Int>): String? {
        val found = matches
            .whereEqualTo("roomCode", code.uppercase())
            .whereEqualTo("status", OnlineMatch.STATUS_WAITING)
            .limit(1)
            .get()
            .await()
            .documents.firstOrNull() ?: return null
        if (found.getString("hostId") == uid) return null
        val required = found.getLong("teamSize")?.toInt()
        if (required != null && required != team.size) throw TeamSizeMismatch(required)
        if (!claimSeat(found.id, uid, name)) return null
        saveTeam(found.id, uid, team)
        return found.id
    }

    /**
     * Takes the empty seat if it is still empty. Two people racing for one room: exactly one wins.
     *
     * Safe to repeat. A commit can reach the server while the reply is lost (a flaky connection), and the SDK
     * then retries the transaction: the retry finds the seat already taken, by THIS player, and used to
     * report failure. That left one phone moving on to "find opponent" while the room already counted it as
     * the guest, so the other phone saw a match and this one saw none. Our own seat now counts as success,
     * and a transaction that errored is double-checked against the server before being treated as a miss.
     */
    private suspend fun claimSeat(matchId: String, uid: String, name: String): Boolean {
        val ref = matches.document(matchId)
        return try {
            firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                val guest = snap.getString("guestId").orEmpty()
                if (guest == uid && snap.getString("status") == OnlineMatch.STATUS_ACTIVE) return@runTransaction true
                if (snap.getString("status") != OnlineMatch.STATUS_WAITING || guest.isNotEmpty()) {
                    return@runTransaction false
                }
                // A room opened under other battle rules (an older app's, which has no version) is not ours to join.
                if ((snap.getLong("rules")?.toInt() ?: 1) != RULES_VERSION) return@runTransaction false
                tx.update(
                    ref,
                    mapOf(
                        "guestId" to uid,
                        "guestName" to name,
                        // The server checks this against the room's own version before it lets the seat be taken.
                        "joinerRules" to RULES_VERSION,
                        "status" to OnlineMatch.STATUS_ACTIVE,
                        "heartbeatGuestAt" to FieldValue.serverTimestamp(),
                        "lastUpdated" to Timestamp.now(),
                    )
                )
                true
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The reply may have been lost after the seat WAS taken: ask the server rather than assume.
            seatIsOurs(ref, uid)
        }
    }

    private suspend fun seatIsOurs(ref: com.google.firebase.firestore.DocumentReference, uid: String): Boolean =
        runCatching {
            val snap = ref.get(Source.SERVER).await()
            snap.getString("guestId") == uid && snap.getString("status") == OnlineMatch.STATUS_ACTIVE
        }.getOrDefault(false)

    private suspend fun saveTeam(matchId: String, uid: String, team: List<Int>) {
        matches.document(matchId).collection("teams").document(uid).set(mapOf("ids" to team)).await()
    }

    override suspend fun submitAction(matchId: String, uid: String, seat: Seat, step: Int, action: OnlineAction) {
        val ref = matches.document(matchId).collection("actions").document("s${step}_${seat.code}")
        // Choices are final (the rules refuse an overwrite), so a retry after a flaky network is a no-op.
        if (ref.get().await().exists()) return
        ref.set(
            buildMap<String, Any> {
                put("step", step)
                put("seat", seat.code)
                put("uid", uid)
                put("kind", action.kind)
                put("index", action.index)
                put("auto", action.auto)
                // Only written when used, so ordinary turns stay valid against rules that predate Tera.
                if (action.tera) put("tera", true)
                // Likewise a Guard, so a plain turn stays valid against rules that predate Guards.
                if (action.guard) put("guard", true)
            }
        ).await()
    }

    override suspend fun react(matchId: String, seat: Seat, index: Int, counter: Long) {
        val field = if (seat == Seat.HOST) "reactH" else "reactG"
        // Cosmetic: a rejected write (rules not deployed yet, offline) just means the rival doesn't see it.
        runCatching { matches.document(matchId).update(field, mapOf("e" to index, "n" to counter)).await() }
    }

    override suspend fun heartbeat(matchId: String, seat: Seat) {
        val field = if (seat == Seat.HOST) "heartbeatHostAt" else "heartbeatGuestAt"
        runCatching { matches.document(matchId).update(field, FieldValue.serverTimestamp()).await() }
    }

    override suspend fun cancelWaitingIfUnjoined(matchId: String): Boolean {
        val ref = matches.document(matchId)
        return runCatching {
            firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                if (snap.getString("status") != OnlineMatch.STATUS_WAITING) return@runTransaction false
                tx.update(ref, mapOf("status" to OnlineMatch.STATUS_FINISHED, "lastUpdated" to Timestamp.now()))
                true
            }.await()
        }.getOrDefault(false)
    }

    override suspend fun finish(matchId: String) {
        runCatching {
            matches.document(matchId)
                .update(mapOf("status" to OnlineMatch.STATUS_FINISHED, "lastUpdated" to Timestamp.now())).await()
        }
    }

    override suspend fun quit(matchId: String, seat: Seat) {
        runCatching {
            matches.document(matchId).update(
                mapOf("status" to OnlineMatch.STATUS_FINISHED, "quitSeat" to seat.code, "lastUpdated" to Timestamp.now())
            ).await()
        }
    }

    override suspend fun claimAbandoned(matchId: String, gone: Seat) {
        runCatching {
            matches.document(matchId).update(
                mapOf("status" to OnlineMatch.STATUS_FINISHED, "abandonedSeat" to gone.code, "lastUpdated" to Timestamp.now())
            ).await()
        }
    }

    private fun reaction(raw: Any?): Pair<Int, Long>? {
        val m = raw as? Map<*, *> ?: return null
        val e = (m["e"] as? Number)?.toInt() ?: return null
        val n = (m["n"] as? Number)?.toLong() ?: return null
        return e to n
    }

    override fun observe(matchId: String): Flow<OnlineSnapshot> = callbackFlow {
        val ref = matches.document(matchId)
        var match: OnlineMatch? = null
        var teamsByUid: Map<String, List<Int>> = emptyMap()
        var actions: ActionLog = emptyMap()

        fun push() {
            val m = match ?: return
            val teams = buildMap {
                teamsByUid[m.hostId]?.let { put(Seat.HOST, it) }
                if (m.hasGuest) teamsByUid[m.guestId]?.let { put(Seat.GUEST, it) }
            }
            trySend(OnlineSnapshot(m, teams, actions))
        }

        val matchReg = ref.addSnapshotListener { snap, _ ->
            val d = snap?.takeIf { it.exists() }?.data ?: return@addSnapshotListener
            match = OnlineMatch(
                id = matchId,
                hostId = d["hostId"] as? String ?: "",
                guestId = d["guestId"] as? String ?: "",
                hostName = d["hostName"] as? String ?: "",
                guestName = d["guestName"] as? String ?: "",
                status = d["status"] as? String ?: OnlineMatch.STATUS_WAITING,
                roomCode = d["roomCode"] as? String ?: "",
                mode = d["mode"] as? String ?: OnlineMatch.MODE_QUICK,
                seed = (d["seed"] as? Number)?.toLong() ?: 0L,
                heartbeatHostMs = (d["heartbeatHostAt"] as? Timestamp)?.toDate()?.time ?: 0L,
                heartbeatGuestMs = (d["heartbeatGuestAt"] as? Timestamp)?.toDate()?.time ?: 0L,
                quitSeat = Seat.fromCode(d["quitSeat"] as? String),
                abandonedSeat = Seat.fromCode(d["abandonedSeat"] as? String),
                reactHost = reaction(d["reactH"]),
                reactGuest = reaction(d["reactG"]),
            )
            push()
        }
        val teamsReg = ref.collection("teams").addSnapshotListener { snap, _ ->
            snap ?: return@addSnapshotListener
            teamsByUid = snap.documents.associate { doc ->
                doc.id to (doc.get("ids") as? List<*>).orEmpty().mapNotNull { (it as? Number)?.toInt() }
            }
            push()
        }
        val actionsReg = ref.collection("actions").addSnapshotListener { snap, _ ->
            snap ?: return@addSnapshotListener
            val log = HashMap<Int, MutableMap<Seat, OnlineAction>>()
            for (doc in snap.documents) {
                // Only the deterministic id counts: "s<step>_<seat>". Anything else is ignored.
                val m = ACTION_ID.matchEntire(doc.id) ?: continue
                val step = m.groupValues[1].toIntOrNull() ?: continue
                val seat = Seat.fromCode(m.groupValues[2]) ?: continue
                val kind = (doc.getString("kind") ?: continue).take(12)
                val index = (doc.get("index") as? Number)?.toInt() ?: 0
                val auto = doc.getBoolean("auto") ?: false
                val tera = doc.getBoolean("tera") ?: false
                val guard = doc.getBoolean("guard") ?: false
                log.getOrPut(step) { mutableMapOf() }[seat] = OnlineAction(kind, index, auto, tera, guard)
            }
            actions = log
            push()
        }
        awaitClose {
            matchReg.remove(); teamsReg.remove(); actionsReg.remove()
        }
    }

    private val ACTION_ID = Regex("s(\\d{1,3})_([hg])")
}
