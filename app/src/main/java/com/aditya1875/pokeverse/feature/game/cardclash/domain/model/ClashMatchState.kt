package com.aditya1875.pokeverse.feature.game.cardclash.domain.model

data class ClashMatchState(
    val matchId: String = "",
    val player1Id: String = "",
    val player2Id: String = "",
    val player1Name: String = "",
    val player2Name: String = "",
    val status: String = "waiting",       // waiting | dealing | active | finished
    val roomCode: String = "",
    val currentRound: Int = 1,
    val p1Score: Double = 0.0,
    val p2Score: Double = 0.0,
    val winner: String? = null,           // "player1" | "player2" | "draw"
    val player1Ready: Boolean = false,
    val player2Ready: Boolean = false,
    val roundP1Locked: Boolean = false,
    val roundP2Locked: Boolean = false,
    val roundP1CardId: Int = -1,          // -1 = not yet revealed
    val roundP2CardId: Int = -1,
    val roundRevealed: Boolean = false,
    val roundWinner: String? = null,
    val roundP1Score: Double = 0.0,
    val roundP2Score: Double = 0.0,
    val completedRounds: List<Map<String, Any>> = emptyList(),
    val heartbeatP1Ms: Long = 0L,
    val heartbeatP2Ms: Long = 0L
)
