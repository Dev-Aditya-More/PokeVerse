package com.aditya1875.pokeverse.feature.game.cardclash.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.game.cardclash.data.repository.CardClashRepository
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashMatchState
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashPhase
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashPokemon
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashRound
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashUiState
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.MatchOutcome
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.RoundWinner
import com.aditya1875.pokeverse.feature.game.pokeduel.domain.engine.DuelGameEngine
import com.aditya1875.pokeverse.feature.game.pokeduel.domain.model.DuelPokemon
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CardClashViewModel(
    private val repository: CardClashRepository,
    private val duelEngine: DuelGameEngine,
    private val xpManager: XPManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClashUiState())
    val uiState: StateFlow<ClashUiState> = _uiState.asStateFlow()

    private val _xpResult = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpResult: SharedFlow<XPResult> = _xpResult

    private val auth = FirebaseAuth.getInstance()

    private var isPlayer1 = true
    private var myId = ""
    private var myName = ""

    // Card chosen this round — kept in ViewModel, not written to Firestore until both lock
    private var pendingCard: ClashPokemon? = null

    // Cache of fetched Pokémon: id → ClashPokemon
    private val pokemonCache = mutableMapOf<Int, ClashPokemon>()

    // Prevents processing the same round reveal more than once
    private var lastProcessedRound = 0

    private var observeJob: Job? = null
    private var timerJob: Job? = null
    private var heartbeatJob: Job? = null

    companion object {
        private const val ROUND_TIMER_SECONDS = 60
        private const val HEARTBEAT_INTERVAL_MS = 20_000L
        private const val DISCONNECT_THRESHOLD_MS = 60_000L
    }

    // ─── Lobby ───────────────────────────────────────────────────────────────

    fun createMatch() {
        resolveIdentity() ?: return
        isPlayer1 = true
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.createMatch(myId, myName) }
                .onSuccess { matchId ->
                    _uiState.update {
                        it.copy(phase = ClashPhase.WAITING_FOR_OPPONENT, matchId = matchId, isLoading = false)
                    }
                    startObserving(matchId)
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun joinRandom() {
        resolveIdentity() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.joinRandomMatch(myId, myName) }
                .onSuccess { matchId ->
                    if (matchId != null) {
                        isPlayer1 = false
                        onJoinedAsPlayer2(matchId)
                    } else {
                        // No open match — create one and wait for anyone to join
                        isPlayer1 = true
                        runCatching { repository.createMatch(myId, myName) }
                            .onSuccess { newMatchId ->
                                _uiState.update {
                                    it.copy(
                                        phase = ClashPhase.WAITING_FOR_OPPONENT,
                                        matchId = newMatchId,
                                        isLoading = false,
                                        isRandomWait = true
                                    )
                                }
                                startObserving(newMatchId)
                            }
                            .onFailure { e ->
                                _uiState.update { it.copy(isLoading = false, error = e.message) }
                            }
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun joinByCode(code: String) {
        resolveIdentity() ?: return
        isPlayer1 = false
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.joinMatchByCode(code.trim().uppercase(), myId, myName) }
                .onSuccess { matchId ->
                    if (matchId != null) onJoinedAsPlayer2(matchId)
                    else _uiState.update { it.copy(isLoading = false, error = "Room not found or already full.") }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun updateEnteredCode(code: String) {
        _uiState.update { it.copy(enteredCode = code.uppercase().take(6)) }
    }

    // ─── Match entry ─────────────────────────────────────────────────────────

    private suspend fun onJoinedAsPlayer2(matchId: String) {
        _uiState.update { it.copy(phase = ClashPhase.DEALING, matchId = matchId, isLoading = true) }
        dealHand(matchId)
        startObserving(matchId)
    }

    private suspend fun dealHand(matchId: String) {
        runCatching { repository.fetchRandomHand() }
            .onSuccess { hand ->
                hand.forEach { pokemonCache[it.id] = it }
                repository.saveHand(matchId, myId, hand)
                repository.markReady(matchId, isPlayer1)
                _uiState.update { it.copy(myHand = hand, isLoading = false) }
            }
            .onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = "Failed to deal hand: ${e.message}") }
            }
    }

    private fun startObserving(matchId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeMatch(matchId).collect { onMatchStateUpdate(it) }
        }
    }

    // ─── State machine ────────────────────────────────────────────────────────

    private fun onMatchStateUpdate(state: ClashMatchState) {
        val current = _uiState.value
        val matchId = state.matchId
        val opponentName = if (isPlayer1) state.player2Name else state.player1Name

        _uiState.update {
            it.copy(
                roomCode = state.roomCode,
                opponentName = opponentName.ifBlank { "Opponent" }
            )
        }

        when (state.status) {

            "dealing" -> {
                // Player1 was waiting; joiner arrived — now player1 must also deal
                if (isPlayer1 && current.myHand.isEmpty() && !state.player1Ready) {
                    viewModelScope.launch { dealHand(matchId) }
                }
                // Player1 activates the match once both hands are ready
                if (isPlayer1 && state.player1Ready && state.player2Ready) {
                    viewModelScope.launch {
                        runCatching { repository.activateMatch(matchId) }
                    }
                }
            }

            "active" -> {
                val myLocked = if (isPlayer1) state.roundP1Locked else state.roundP2Locked
                val opponentLocked = if (isPlayer1) state.roundP2Locked else state.roundP1Locked

                // Disconnect detection: opponent heartbeat stale > 60s (skip if 0 = not yet set)
                val oppHeartbeatMs = if (isPlayer1) state.heartbeatP2Ms else state.heartbeatP1Ms
                val opponentDisconnected = oppHeartbeatMs > 0L &&
                    System.currentTimeMillis() - oppHeartbeatMs > DISCONNECT_THRESHOLD_MS

                _uiState.update {
                    it.copy(
                        currentRound = state.currentRound,
                        myScore = if (isPlayer1) state.p1Score.toFloat() else state.p2Score.toFloat(),
                        opponentScore = if (isPlayer1) state.p2Score.toFloat() else state.p1Score.toFloat(),
                        myLocked = myLocked,
                        opponentLocked = opponentLocked,
                        opponentDisconnected = opponentDisconnected
                    )
                }

                // Transition into SELECTING phase once we have our hand.
                // Never re-enter SELECTING from MATCH_FINISHED (can happen if a stale
                // "active" snapshot arrives after the "finished" one is already processed).
                if (current.phase != ClashPhase.SELECTING
                    && current.phase != ClashPhase.REVEALING
                    && current.phase != ClashPhase.MATCH_FINISHED
                ) {
                    if (current.myHand.isNotEmpty()) {
                        _uiState.update { it.copy(phase = ClashPhase.SELECTING) }
                        startRoundTimer()
                        startHeartbeat(matchId)
                    }
                }

                // Both locked but cards not yet revealed — write my card
                if (myLocked && opponentLocked && !state.roundRevealed) {
                    pendingCard?.let { card ->
                        viewModelScope.launch {
                            runCatching { repository.revealMyCard(matchId, isPlayer1, card.id) }
                        }
                    }
                }

                // Cards revealed and not yet processed for this round
                if (state.roundRevealed
                    && state.roundP1CardId != -1
                    && state.roundP2CardId != -1
                    && state.currentRound > lastProcessedRound
                ) {
                    lastProcessedRound = state.currentRound
                    processReveal(state)
                }
            }

            "finished" -> {
                // Stop timer and heartbeat — no point continuing them after the match ends
                timerJob?.cancel(); timerJob = null
                heartbeatJob?.cancel(); heartbeatJob = null

                if (current.phase != ClashPhase.MATCH_FINISHED) {
                    val myFinal = if (isPlayer1) state.p1Score.toFloat() else state.p2Score.toFloat()
                    val oppFinal = if (isPlayer1) state.p2Score.toFloat() else state.p1Score.toFloat()
                    val outcome = when (state.winner) {
                        "player1" -> if (isPlayer1) MatchOutcome.WIN else MatchOutcome.LOSE
                        "player2" -> if (!isPlayer1) MatchOutcome.WIN else MatchOutcome.LOSE
                        else -> MatchOutcome.DRAW
                    }
                    _uiState.update {
                        it.copy(
                            phase = ClashPhase.MATCH_FINISHED,
                            myScore = myFinal,
                            opponentScore = oppFinal,
                            matchOutcome = outcome
                        )
                    }
                    awardXp(outcome, current.roundHistory.count { it.winner == RoundWinner.ME })
                }
            }
        }
    }

    // ─── Round reveal logic ───────────────────────────────────────────────────

    private fun processReveal(state: ClashMatchState) {
        val myCardId = if (isPlayer1) state.roundP1CardId else state.roundP2CardId
        val oppCardId = if (isPlayer1) state.roundP2CardId else state.roundP1CardId
        val matchId = state.matchId

        viewModelScope.launch {
            // Resolve my card: prefer cache, then pendingCard, then fetch from API as last resort
            val myCard = pokemonCache[myCardId]
                ?: pendingCard?.takeIf { it.id == myCardId }
                ?: repository.fetchPokemonById(myCardId)
                ?: run {
                    _uiState.update { it.copy(error = "Card data missing — please reconnect.") }
                    return@launch
                }
            pokemonCache[myCardId] = myCard

            val oppCard = pokemonCache.getOrPut(oppCardId) {
                repository.fetchPokemonById(oppCardId) ?: run {
                    _uiState.update { it.copy(error = "Opponent card data missing.") }
                    return@launch
                }
            }

            val myEff = myCard.bst * duelEngine.computeAdvantage(myCard.toDuel(), oppCard.toDuel())
            val oppEff = oppCard.bst * duelEngine.computeAdvantage(oppCard.toDuel(), myCard.toDuel())

            val winner = when {
                myEff > oppEff -> RoundWinner.ME
                oppEff > myEff -> RoundWinner.OPPONENT
                else -> RoundWinner.DRAW
            }

            val (myDelta, oppDelta) = when (winner) {
                RoundWinner.ME -> 1f to 0f
                RoundWinner.OPPONENT -> 0f to 1f
                RoundWinner.DRAW -> 0.5f to 0.5f
            }

            val clashRound = ClashRound(
                roundNumber = state.currentRound,
                myCard = myCard,
                opponentCard = oppCard,
                myScore = myEff,
                opponentScore = oppEff,
                winner = winner
            )

            _uiState.update { s ->
                s.copy(
                    phase = ClashPhase.REVEALING,
                    revealMyCard = myCard,
                    revealOpponentCard = oppCard,
                    revealRound = clashRound,
                    myScore = s.myScore + myDelta,
                    opponentScore = s.opponentScore + oppDelta,
                    myUsedIds = s.myUsedIds + myCardId,
                    opponentRevealedCards = s.opponentRevealedCards + oppCard,
                    roundHistory = s.roundHistory + clashRound
                )
            }

            // Only player1 writes round result to avoid duplicate writes
            if (isPlayer1) {
                val updatedState = _uiState.value
                val totalP1 = if (isPlayer1) updatedState.myScore.toDouble() else updatedState.opponentScore.toDouble()
                val totalP2 = if (isPlayer1) updatedState.opponentScore.toDouble() else updatedState.myScore.toDouble()
                val winnerStr = when (winner) {
                    RoundWinner.ME -> "player1"
                    RoundWinner.OPPONENT -> "player2"
                    RoundWinner.DRAW -> "draw"
                }

                runCatching {
                    if (state.currentRound >= 6) {
                        val matchWinner = when {
                            totalP1 > totalP2 -> "player1"
                            totalP2 > totalP1 -> "player2"
                            else -> "draw"
                        }
                        repository.finishMatch(matchId, matchWinner, totalP1, totalP2)
                    } else {
                        repository.saveRoundResult(
                            matchId = matchId,
                            roundNumber = state.currentRound,
                            p1CardId = state.roundP1CardId,
                            p2CardId = state.roundP2CardId,
                            roundWinner = winnerStr,
                            roundP1Score = if (isPlayer1) myDelta.toDouble() else oppDelta.toDouble(),
                            roundP2Score = if (isPlayer1) oppDelta.toDouble() else myDelta.toDouble(),
                            newP1Score = totalP1,
                            newP2Score = totalP2
                        )
                    }
                }
            }
        }
    }

    // ─── In-round actions ─────────────────────────────────────────────────────

    fun selectCard(pokemonId: Int) {
        if (_uiState.value.myLocked) return
        _uiState.update { it.copy(selectedCardId = pokemonId) }
    }

    fun lockCard() {
        val state = _uiState.value
        val matchId = state.matchId ?: return
        val cardId = state.selectedCardId ?: return
        if (state.myLocked) return

        timerJob?.cancel()
        timerJob = null
        pendingCard = pokemonCache[cardId]
        _uiState.update { it.copy(myLocked = true) }

        viewModelScope.launch {
            runCatching { repository.lockCard(matchId, isPlayer1) }
                .onFailure { _uiState.update { s -> s.copy(myLocked = false) } }
        }
    }

    /** Called from the UI once the reveal animation finishes — ready for next round. */
    fun acknowledgeReveal() {
        val state = _uiState.value

        // Guard: if all 6 rounds have been played (or hand is exhausted), don't go back
        // to SELECTING — Firestore will deliver the "finished" status imminently.
        // Returning here keeps the user on the reveal screen until that update arrives,
        // preventing the ghost "selecting with no cards" screen.
        val roundsPlayed = state.roundHistory.size
        val handEmpty = state.myUsedIds.size >= state.myHand.size && state.myHand.isNotEmpty()
        if (roundsPlayed >= 6 || handEmpty) {
            // Just clear the reveal data; phase will flip to MATCH_FINISHED via Firestore
            _uiState.update {
                it.copy(
                    revealMyCard = null,
                    revealOpponentCard = null,
                    revealRound = null
                )
            }
            return
        }

        pendingCard = null
        _uiState.update {
            it.copy(
                phase = ClashPhase.SELECTING,
                selectedCardId = null,
                myLocked = false,
                opponentLocked = false,
                revealMyCard = null,
                revealOpponentCard = null,
                revealRound = null,
                timerSeconds = ROUND_TIMER_SECONDS,
                opponentDisconnected = false
            )
        }
        startRoundTimer()
    }

    /** Claims the win when opponent has been disconnected for over a minute. */
    fun forfeitOpponent() {
        val state = _uiState.value
        val matchId = state.matchId ?: return
        viewModelScope.launch {
            val myScore = state.myScore.toDouble()
            val oppScore = state.opponentScore.toDouble()
            val winnerStr = if (isPlayer1) "player1" else "player2"
            val p1Score = if (isPlayer1) myScore else oppScore
            val p2Score = if (isPlayer1) oppScore else myScore
            runCatching { repository.finishMatch(matchId, winnerStr, p1Score, p2Score) }
        }
    }

    // ─── XP ──────────────────────────────────────────────────────────────────

    private fun awardXp(outcome: MatchOutcome, roundsWon: Int) {
        viewModelScope.launch {
            repeat(roundsWon) {
                val r = xpManager.awardGameXP(XPEvent.CardClashRoundWin)
                if (r.xpGained > 0) _xpResult.emit(r)
            }
            if (outcome == MatchOutcome.WIN) {
                val winResult = xpManager.awardGameXP(XPEvent.CardClashWin)
                if (winResult.xpGained > 0) _xpResult.emit(winResult)
                if (roundsWon == 6) {
                    val perfectResult = xpManager.awardGameXP(XPEvent.CardClashPerfect)
                    if (perfectResult.xpGained > 0) _xpResult.emit(perfectResult)
                }
            }
        }
    }

    // ─── Reset ────────────────────────────────────────────────────────────────

    fun reset() {
        observeJob?.cancel()
        timerJob?.cancel()
        heartbeatJob?.cancel()
        observeJob = null
        timerJob = null
        heartbeatJob = null
        pendingCard = null
        pokemonCache.clear()
        lastProcessedRound = 0
        _uiState.value = ClashUiState()
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
        timerJob?.cancel()
        heartbeatJob?.cancel()
    }

    // ─── Timer & heartbeat helpers ────────────────────────────────────────────

    private fun startRoundTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timerSeconds = ROUND_TIMER_SECONDS) }
        timerJob = viewModelScope.launch {
            for (remaining in ROUND_TIMER_SECONDS - 1 downTo 0) {
                delay(1000L)
                _uiState.update { it.copy(timerSeconds = remaining) }
                if (remaining == 0) autoLock()
            }
        }
    }

    private fun autoLock() {
        val state = _uiState.value
        if (state.myLocked || state.matchId == null) return
        val cardId = state.selectedCardId
            ?: state.myHand.firstOrNull { it.id !in state.myUsedIds }?.id
            ?: return
        _uiState.update { it.copy(selectedCardId = cardId) }
        lockCard()
    }

    private fun startHeartbeat(matchId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                delay(HEARTBEAT_INTERVAL_MS)
                runCatching { repository.updateHeartbeat(matchId, isPlayer1) }
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun resolveIdentity(): Unit? {
        val user = auth.currentUser ?: run {
            _uiState.update { it.copy(error = "Sign in required to play Card Clash.") }
            return null
        }
        myId = user.uid
        myName = user.displayName?.takeIf { it.isNotBlank() } ?: "Trainer"
        return Unit
    }

    private fun ClashPokemon.toDuel() = DuelPokemon(id = id, name = name, spriteUrl = spriteUrl, types = types)
}
