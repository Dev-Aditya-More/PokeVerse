package com.aditya1875.pokeverse.feature.game.cardclash.data.repository

import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashMatchState
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashPokemon
import com.aditya1875.pokeverse.feature.game.core.domain.YieldResult
import kotlinx.coroutines.flow.Flow

interface CardClashRepository {

    /** Creates a room for a friend (joined by its code, never matched into by strangers) and returns its ID. */
    suspend fun createMatch(myId: String, myName: String): String

    /** Creates a room in the random queue, open to anyone searching, and returns its ID. */
    suspend fun createQuickMatch(myId: String, myName: String): String

    /**
     * Joins an open random-queue room. Tries every candidate, not only the newest, so losing a race for one
     * room moves on to the next. Returns the matchId, or null if there is nothing open.
     */
    suspend fun joinRandomMatch(myId: String, myName: String): String?

    /** How many OTHER players are waiting in the random queue right now, kept live. Never throws. */
    fun observeQuickPool(myId: String): Flow<Int>

    /**
     * Another waiting player this one should go and join, or null to keep waiting. Two players who tapped
     * "play" together each opened a room and cannot see the other's; this is how they find each other.
     */
    suspend fun findRoomToYieldTo(mine: String, myId: String): String?

    /** Gives up [mine] and takes the seat in [other], in that order so nobody ends up in two rooms. */
    suspend fun yieldToRoom(mine: String, other: String, myId: String, myName: String): YieldResult

    /** Joins a match by 6-char room code. Returns matchId or null if code is invalid/full. */
    suspend fun joinMatchByCode(roomCode: String, myId: String, myName: String): String?

    /** Writes the dealt hand to the hands subcollection for this player. */
    suspend fun saveHand(matchId: String, userId: String, cards: List<ClashPokemon>)

    /** Fetches the stored hand for a player (called by the joiner after they deal). */
    suspend fun loadHand(matchId: String, userId: String): List<ClashPokemon>

    /** Marks this player as ready (hand dealt). Match goes active when both are ready. */
    suspend fun markReady(matchId: String, isPlayer1: Boolean)

    /** Fires when a player taps Lock In, just sets the locked flag, card stays in ViewModel. */
    suspend fun lockCard(matchId: String, isPlayer1: Boolean)

    /**
     * Called when BOTH players are locked, each player writes their own card ID independently.
     * Firestore merges the two concurrent writes. Sets roundRevealed = true.
     */
    suspend fun revealMyCard(matchId: String, isPlayer1: Boolean, cardId: Int, roundNumber: Int)

    /**
     * Idempotent, transactional round resolution, safe for BOTH players to call. Appends the
     * round (with both card IDs and running totals) to completedRounds, resets the per-round
     * fields and advances currentRound; on the final round it also sets status = finished and
     * the winner in the same write. Returns false if the round was already resolved.
     */
    suspend fun resolveRound(
        matchId: String,
        roundNumber: Int,
        p1CardId: Int,
        p2CardId: Int,
        roundWinner: String,
        isFinalRound: Boolean
    ): Boolean

    /** Sets status = finished and winner field on the match document. */
    suspend fun finishMatch(matchId: String, winner: String, p1Score: Double, p2Score: Double)

    /**
     * Atomically abandons the waiting room for the bot-fallback path, but only if it's
     * still actually "waiting". Returns false without writing anything if a real opponent
     * already joined (status moved past "waiting") in the window between the fallback timer
     * firing and this call landing, so the caller can let the real match proceed instead of
     * stomping it.
     */
    suspend fun cancelWaitingMatchIfUnjoined(matchId: String): Boolean

    /** Real-time Firestore listener for the match document. Emits on every change. */
    fun observeMatch(matchId: String): Flow<ClashMatchState>

    /** Writes a heartbeat timestamp so the opponent can detect disconnection. */
    suspend fun updateHeartbeat(matchId: String, isPlayer1: Boolean)

    /** Sets status = "active" (called by player1 when both hands are ready). */
    suspend fun activateMatch(matchId: String)

    /** Fetches a single Pokémon from PokeAPI by numeric ID. Returns null on error. */
    suspend fun fetchPokemonById(id: Int): ClashPokemon?

    /** Fetches 6 unique random Pokémon from PokeAPI for the player's hand. */
    suspend fun fetchRandomHand(): List<ClashPokemon>
}
