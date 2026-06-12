package com.aditya1875.pokeverse.feature.game.cardclash.domain.model

data class ClashUiState(
    val phase: ClashPhase = ClashPhase.LOBBY,
    val isLoading: Boolean = false,
    val error: String? = null,

    // Match meta
    val matchId: String? = null,
    val roomCode: String? = null,
    val opponentName: String = "Opponent",

    // My hand
    val myHand: List<ClashPokemon> = emptyList(),
    val myUsedIds: Set<Int> = emptySet(),

    // Opponent revealed cards (known after each round)
    val opponentRevealedCards: List<ClashPokemon> = emptyList(),

    // Round progress
    val currentRound: Int = 1,
    val myScore: Float = 0f,
    val opponentScore: Float = 0f,

    // Current round selection
    val selectedCardId: Int? = null,
    val myLocked: Boolean = false,
    val opponentLocked: Boolean = false,

    // Reveal moment (populated when phase == REVEALING)
    val revealMyCard: ClashPokemon? = null,
    val revealOpponentCard: ClashPokemon? = null,
    val revealRound: ClashRound? = null,

    // History
    val roundHistory: List<ClashRound> = emptyList(),

    // Final result
    val matchOutcome: MatchOutcome? = null,

    // Round timer (counts down from 60)
    val timerSeconds: Int = 60,

    // Opponent disconnect detection
    val opponentDisconnected: Boolean = false,

    // Friend code input in lobby
    val enteredCode: String = "",

    // True when Play Random found no open match and created a waiting room as fallback
    val isRandomWait: Boolean = false,

    // True when the match is against the AI bot (fully local, no Firestore)
    val isBotMatch: Boolean = false,

    // Countdown seconds remaining before bot match kicks in (only relevant during random wait)
    val matchmakingSecondsLeft: Int = 30
)

enum class ClashPhase {
    LOBBY,
    WAITING_FOR_OPPONENT,
    DEALING,
    SELECTING,
    REVEALING,
    MATCH_FINISHED
}

enum class MatchOutcome { WIN, LOSE, DRAW }
