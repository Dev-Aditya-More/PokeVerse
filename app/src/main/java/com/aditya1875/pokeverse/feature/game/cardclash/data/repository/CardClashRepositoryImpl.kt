package com.aditya1875.pokeverse.feature.game.cardclash.data.repository

import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashMatchState
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashPokemon
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.PokemonDetailsApi
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CardClashRepositoryImpl(
    private val api: PokemonDetailsApi
) : CardClashRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val matches = firestore.collection("card_battles")

    companion object {
        // A legit waiting room converts to a bot match (or gets cancelled) well before this —
        // see CardClashViewModel.BOT_MATCHMAKING_TIMEOUT (30s). Anything still "waiting" past
        // this age was abandoned without cleanup and should never be matched into. Kept
        // comfortably above the bot-fallback timeout (not equal to it) so a room that's still
        // legitimately waiting in its final seconds never gets excluded here as "stale" —
        // that would just recreate the "matches aren't happening" problem from the other side.
        private const val STALE_MATCH_THRESHOLD_MS = 40_000L
    }

    // IDs of legendary/mythical Pokémon excluded from the random pool (Gen 1-5)
    private val excludedIds = setOf(
        144, 145, 146, 150, 151,
        243, 244, 245, 249, 250, 251,
        377, 378, 379, 380, 381, 382, 383, 384, 385, 386,
        480, 481, 482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494,
        638, 639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 649
    )

    private val safePool = (1..649).filter { it !in excludedIds }

    // ─── Match lifecycle ──────────────────────────────────────────────────────

    override suspend fun createMatch(myId: String, myName: String): String {
        val roomCode = generateRoomCode()
        val doc = matches.document()
        doc.set(
            mapOf(
                "player1Id" to myId,
                "player2Id" to "",
                "player1Name" to myName,
                "player2Name" to "",
                "status" to "waiting",
                "roomCode" to roomCode,
                "currentRound" to 1,
                "p1Score" to 0.0,
                "p2Score" to 0.0,
                "winner" to null,
                "player1Ready" to false,
                "player2Ready" to false,
                "roundP1Locked" to false,
                "roundP2Locked" to false,
                "roundP1CardId" to -1,
                "roundP2CardId" to -1,
                "roundRevealed" to false,
                "roundWinner" to null,
                "roundP1Score" to 0.0,
                "roundP2Score" to 0.0,
                "completedRounds" to emptyList<Map<String, Any>>(),
                "heartbeatP1Ms" to System.currentTimeMillis(),
                "heartbeatP2Ms" to 0L,
                "createdAt" to Timestamp.now(),
                "lastUpdated" to Timestamp.now()
            )
        ).await()
        return doc.id
    }

    override suspend fun joinRandomMatch(myId: String, myName: String): String? {
        // No orderBy → avoids composite index requirement; equality-only compound queries
        // use Firestore's automatic collection-group index.
        val snapshot = matches
            .whereEqualTo("status", "waiting")
            .whereEqualTo("player2Id", "")
            .limit(10)
            .get()
            .await()

        val now = System.currentTimeMillis()

        // Waiting rooms whose creator bailed (backgrounded, crashed, force-closed) without
        // going through the cancel/bot-fallback flow linger forever with no fix. Without this
        // filter, players increasingly match into these ghost rooms whose "opponent" never
        // shows up — which is what actually made matchmaking feel broken, not just low player
        // counts. Anything older than the bot-fallback window is almost certainly abandoned;
        // prefer the freshest candidate among what's left so newer joiners see each other first.
        val target = snapshot.documents
            .filter { it.getString("player1Id") != myId }
            .filter { doc ->
                val createdAtMs = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                now - createdAtMs < STALE_MATCH_THRESHOLD_MS
            }
            .maxByOrNull { it.getTimestamp("createdAt")?.toDate()?.time ?: 0L }
            ?: return null

        target.reference.update(
            mapOf(
                "player2Id" to myId,
                "player2Name" to myName,
                "status" to "dealing",
                "lastUpdated" to Timestamp.now()
            )
        ).await()

        return target.id
    }

    override suspend fun joinMatchByCode(roomCode: String, myId: String, myName: String): String? {
        val snapshot = matches
            .whereEqualTo("roomCode", roomCode.uppercase())
            .whereEqualTo("status", "waiting")
            .limit(1)
            .get()
            .await()

        val target = snapshot.documents.firstOrNull() ?: return null

        target.reference.update(
            mapOf(
                "player2Id" to myId,
                "player2Name" to myName,
                "status" to "dealing",
                "lastUpdated" to Timestamp.now()
            )
        ).await()

        return target.id
    }

    // ─── Hand management ──────────────────────────────────────────────────────

    override suspend fun saveHand(matchId: String, userId: String, cards: List<ClashPokemon>) {
        matches.document(matchId)
            .collection("hands")
            .document(userId)
            .set(
                mapOf(
                    "cards" to cards.map { it.toMap() },
                    "usedCardIds" to emptyList<Int>()
                )
            ).await()
    }

    override suspend fun loadHand(matchId: String, userId: String): List<ClashPokemon> {
        val doc = matches.document(matchId)
            .collection("hands")
            .document(userId)
            .get()
            .await()

        @Suppress("UNCHECKED_CAST")
        val raw = doc.get("cards") as? List<Map<String, Any>> ?: return emptyList()
        return raw.map { it.toClashPokemon() }
    }

    override suspend fun markReady(matchId: String, isPlayer1: Boolean) {
        val field = if (isPlayer1) "player1Ready" else "player2Ready"
        matches.document(matchId).update(
            mapOf(field to true, "lastUpdated" to Timestamp.now())
        ).await()
    }

    // ─── Round mechanics ──────────────────────────────────────────────────────

    override suspend fun lockCard(matchId: String, isPlayer1: Boolean) {
        val field = if (isPlayer1) "roundP1Locked" else "roundP2Locked"
        matches.document(matchId).update(
            mapOf(field to true, "lastUpdated" to Timestamp.now())
        ).await()
    }

    override suspend fun revealMyCard(matchId: String, isPlayer1: Boolean, cardId: Int, roundNumber: Int) {
        val myField = if (isPlayer1) "roundP1CardId" else "roundP2CardId"
        val otherField = if (isPlayer1) "roundP2CardId" else "roundP1CardId"
        val docRef = matches.document(matchId)

        // Atomic transaction: write our card ID, and only set roundRevealed once
        // BOTH cards are present. This prevents the race where the first writer's
        // roundRevealed=true causes the second writer to skip their write entirely.
        // The round check stops a delayed/duplicate call from a previous round writing a
        // stale card into the freshly reset next round.
        firestore.runTransaction { tx ->
            val snap = tx.get(docRef)
            val docRound = (snap.get("currentRound") as? Number)?.toInt() ?: 1
            if (docRound != roundNumber || snap.getString("status") != "active") {
                return@runTransaction false
            }
            val myExisting = (snap.get(myField) as? Number)?.toInt() ?: -1
            val otherCardId = (snap.get(otherField) as? Number)?.toInt() ?: -1
            if (myExisting == cardId && (otherCardId == -1 || snap.getBoolean("roundRevealed") == true)) {
                return@runTransaction false
            }
            val updates = mutableMapOf<String, Any>(
                myField to cardId.toLong(),
                "lastUpdated" to Timestamp.now()
            )
            if (otherCardId != -1) {
                updates["roundRevealed"] = true
            }
            tx.update(docRef, updates)
            true
        }.await()
    }

    override suspend fun resolveRound(
        matchId: String,
        roundNumber: Int,
        p1CardId: Int,
        p2CardId: Int,
        roundWinner: String,
        isFinalRound: Boolean
    ): Boolean {
        val docRef = matches.document(matchId)
        // Everything (history entry, running totals, round reset, and — on the last round —
        // the finish) happens in ONE transaction, and the totals are derived from the stored
        // document rather than a client's local tally. Either player may call this; whoever
        // lands second sees the round already in completedRounds and becomes a no-op. The
        // history entry carries both card IDs, so a client that never observed the transient
        // "roundRevealed" snapshot can still rebuild the round from completedRounds.
        return firestore.runTransaction { tx ->
            val snap = tx.get(docRef)
            if (snap.getString("status") != "active") return@runTransaction false

            @Suppress("UNCHECKED_CAST")
            val history = (snap.get("completedRounds") as? List<Map<String, Any>>
                ?: emptyList())
            if (history.any { (it["round"] as? Number)?.toInt() == roundNumber }) {
                return@runTransaction false
            }

            val (d1, d2) = when (roundWinner) {
                "player1" -> 1.0 to 0.0
                "player2" -> 0.0 to 1.0
                else -> 0.5 to 0.5
            }
            val p1Total = ((snap.get("p1Score") as? Number)?.toDouble() ?: 0.0) + d1
            val p2Total = ((snap.get("p2Score") as? Number)?.toDouble() ?: 0.0) + d2

            val entry = mapOf(
                "round" to roundNumber,
                "p1CardId" to p1CardId,
                "p2CardId" to p2CardId,
                "winner" to roundWinner,
                "p1Score" to d1,
                "p2Score" to d2,
                "p1Total" to p1Total,
                "p2Total" to p2Total
            )

            val updates = mutableMapOf<String, Any?>(
                "completedRounds" to history + entry,
                "p1Score" to p1Total,
                "p2Score" to p2Total,
                "roundWinner" to roundWinner,
                "roundP1Score" to d1,
                "roundP2Score" to d2,
                "roundP1Locked" to false,
                "roundP2Locked" to false,
                "roundP1CardId" to -1,
                "roundP2CardId" to -1,
                "roundRevealed" to false,
                "lastUpdated" to Timestamp.now()
            )
            if (isFinalRound) {
                updates["status"] = "finished"
                updates["winner"] = when {
                    p1Total > p2Total -> "player1"
                    p2Total > p1Total -> "player2"
                    else -> "draw"
                }
            } else {
                updates["currentRound"] = roundNumber + 1
            }
            tx.update(docRef, updates)
            true
        }.await()
    }

    override suspend fun finishMatch(matchId: String, winner: String, p1Score: Double, p2Score: Double) {
        matches.document(matchId).update(
            mapOf(
                "status" to "finished",
                "winner" to winner,
                "p1Score" to p1Score,
                "p2Score" to p2Score,
                "lastUpdated" to Timestamp.now()
            )
        ).await()
    }

    override suspend fun cancelWaitingMatchIfUnjoined(matchId: String): Boolean {
        val docRef = matches.document(matchId)
        return firestore.runTransaction { tx ->
            val snap = tx.get(docRef)
            if (snap.getString("status") != "waiting") {
                // Someone already joined (status moved to "dealing") in the gap between the
                // fallback timer firing and this transaction running — leave the doc alone.
                false
            } else {
                tx.update(
                    docRef,
                    mapOf(
                        "status" to "finished",
                        "winner" to "draw",
                        "p1Score" to 0.0,
                        "p2Score" to 0.0,
                        "lastUpdated" to Timestamp.now()
                    )
                )
                true
            }
        }.await()
    }

    // ─── Real-time observation ────────────────────────────────────────────────

    override fun observeMatch(matchId: String): Flow<ClashMatchState> = callbackFlow {
        val listener = matches.document(matchId).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            val data = snapshot.data ?: return@addSnapshotListener

            @Suppress("UNCHECKED_CAST")
            val state = ClashMatchState(
                matchId = matchId,
                player1Id = data["player1Id"] as? String ?: "",
                player2Id = data["player2Id"] as? String ?: "",
                player1Name = data["player1Name"] as? String ?: "",
                player2Name = data["player2Name"] as? String ?: "",
                status = data["status"] as? String ?: "waiting",
                roomCode = data["roomCode"] as? String ?: "",
                currentRound = (data["currentRound"] as? Number)?.toInt() ?: 1,
                p1Score = (data["p1Score"] as? Number)?.toDouble() ?: 0.0,
                p2Score = (data["p2Score"] as? Number)?.toDouble() ?: 0.0,
                winner = data["winner"] as? String,
                player1Ready = data["player1Ready"] as? Boolean ?: false,
                player2Ready = data["player2Ready"] as? Boolean ?: false,
                roundP1Locked = data["roundP1Locked"] as? Boolean ?: false,
                roundP2Locked = data["roundP2Locked"] as? Boolean ?: false,
                roundP1CardId = (data["roundP1CardId"] as? Number)?.toInt() ?: -1,
                roundP2CardId = (data["roundP2CardId"] as? Number)?.toInt() ?: -1,
                roundRevealed = data["roundRevealed"] as? Boolean ?: false,
                roundWinner = data["roundWinner"] as? String,
                roundP1Score = (data["roundP1Score"] as? Number)?.toDouble() ?: 0.0,
                roundP2Score = (data["roundP2Score"] as? Number)?.toDouble() ?: 0.0,
                completedRounds = data["completedRounds"] as? List<Map<String, Any>> ?: emptyList(),
                heartbeatP1Ms = (data["heartbeatP1Ms"] as? Number)?.toLong() ?: 0L,
                heartbeatP2Ms = (data["heartbeatP2Ms"] as? Number)?.toLong() ?: 0L
            )
            trySend(state)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun updateHeartbeat(matchId: String, isPlayer1: Boolean) {
        val field = if (isPlayer1) "heartbeatP1Ms" else "heartbeatP2Ms"
        runCatching {
            matches.document(matchId).update(field, System.currentTimeMillis()).await()
        }
    }

    override suspend fun activateMatch(matchId: String) {
        matches.document(matchId).update(
            mapOf("status" to "active", "lastUpdated" to Timestamp.now())
        ).await()
    }

    // ─── Pokemon fetching ─────────────────────────────────────────────────────

    override suspend fun fetchPokemonById(id: Int): ClashPokemon? = runCatching {
        val response = api.getPokemonByName(id.toString())
        ClashPokemon(
            id = response.id,
            name = response.name.replaceFirstChar { it.uppercase() },
            types = response.types.map { it.type.name },
            bst = response.stats.sumOf { it.base_stat },
            spriteUrl = response.sprites.other?.officialArtwork?.frontDefault
                ?: response.sprites.front_default ?: ""
        )
    }.getOrNull()

    override suspend fun fetchRandomHand(): List<ClashPokemon> {
        val ids = safePool.shuffled().take(9) // fetch 9, take first 6 that succeed
        return ids.mapNotNull { id -> fetchPokemonById(id) }.take(6)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun ClashPokemon.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "types" to types,
        "bst" to bst,
        "spriteUrl" to spriteUrl
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toClashPokemon() = ClashPokemon(
        id = (this["id"] as? Long)?.toInt() ?: 0,
        name = this["name"] as? String ?: "",
        types = this["types"] as? List<String> ?: emptyList(),
        bst = (this["bst"] as? Long)?.toInt() ?: 0,
        spriteUrl = this["spriteUrl"] as? String ?: ""
    )
}
