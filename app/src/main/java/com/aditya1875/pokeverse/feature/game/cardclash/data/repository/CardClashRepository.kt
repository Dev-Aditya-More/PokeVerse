package com.aditya1875.pokeverse.feature.game.cardclash.data.repository

import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashMatchState
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashPokemon
import kotlinx.coroutines.flow.Flow

interface CardClashRepository {

    /** Creates a new match and returns its Firestore ID. */
    suspend fun createMatch(myId: String, myName: String): String

    /** Finds the first open match in the queue and joins it. Returns matchId or null if none found. */
    suspend fun joinRandomMatch(myId: String, myName: String): String?

    /** Joins a match by 6-char room code. Returns matchId or null if code is invalid/full. */
    suspend fun joinMatchByCode(roomCode: String, myId: String, myName: String): String?

    /** Writes the dealt hand to the hands subcollection for this player. */
    suspend fun saveHand(matchId: String, userId: String, cards: List<ClashPokemon>)

    /** Fetches the stored hand for a player (called by the joiner after they deal). */
    suspend fun loadHand(matchId: String, userId: String): List<ClashPokemon>

    /** Marks this player as ready (hand dealt). Match goes active when both are ready. */
    suspend fun markReady(matchId: String, isPlayer1: Boolean)

    /** Fires when a player taps Lock In — just sets the locked flag, card stays in ViewModel. */
    suspend fun lockCard(matchId: String, isPlayer1: Boolean)

    /**
     * Called when BOTH players are locked — each player writes their own card ID independently.
     * Firestore merges the two concurrent writes. Sets roundRevealed = true.
     */
    suspend fun revealMyCard(matchId: String, isPlayer1: Boolean, cardId: Int)

    /** Appends the round result to completedRounds, resets round state, increments currentRound. */
    suspend fun saveRoundResult(
        matchId: String,
        roundNumber: Int,
        p1CardId: Int,
        p2CardId: Int,
        roundWinner: String,
        roundP1Score: Double,
        roundP2Score: Double,
        newP1Score: Double,
        newP2Score: Double
    )

    /** Sets status = finished and winner field on the match document. */
    suspend fun finishMatch(matchId: String, winner: String, p1Score: Double, p2Score: Double)

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
