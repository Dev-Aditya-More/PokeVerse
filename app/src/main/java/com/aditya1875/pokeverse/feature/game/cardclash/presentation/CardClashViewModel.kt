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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

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

    // How many entries of the match's persisted completedRounds have been applied to the UI.
    // Real-match round state (history, used cards, scores, opponent's remaining cards) is rebuilt
    // ONLY from that persisted history — never from the transient roundRevealed/roundXCardId
    // fields, which the resolver resets immediately and a slow listener can miss entirely. That
    // miss was what left one player with 3 cards and the other with 2.
    private var appliedRounds = 0

    // Latest Firestore snapshot; fed to the reconcile worker (conflated — each snapshot carries
    // the full history, so skipping intermediate ones is safe).
    private var latestState: ClashMatchState? = null
    private val roundSync = MutableStateFlow<ClashMatchState?>(null)
    private var syncJob: Job? = null

    // Round for which this client has already written its card to Firestore
    private var revealWrittenRound = 0

    // Outcome delivered by Firestore "finished"; shown once the final reveal has been watched
    private var pendingOutcome: MatchOutcome? = null

    private var observeJob: Job? = null
    private var timerJob: Job? = null
    private var heartbeatJob: Job? = null
    private var matchmakingTimerJob: Job? = null

    // Bot match state — only used when isBotMatch = true
    private var isBotMatchActive = false
    private var botHand: List<ClashPokemon> = emptyList()
    private var botUsedIds: Set<Int> = emptySet()

    companion object {
        private const val TOTAL_ROUNDS = 6
        private const val ROUND_TIMER_SECONDS = 60
        private const val HEARTBEAT_INTERVAL_MS = 20_000L
        private const val DISCONNECT_THRESHOLD_MS = 60_000L
        // Real players need a real shot at finding each other before we give up on their
        // behalf — 18s (previously cut down from 30s for a "snappier" fallback) meant two
        // humans coordinating a match would routinely both still be tapping through menus
        // when the bot took over. Keep in sync with
        // CardClashRepositoryImpl.STALE_MATCH_THRESHOLD_MS, which must stay comfortably above
        // this so a room is never treated as abandoned while it's still legitimately waiting.
        private const val BOT_MATCHMAKING_TIMEOUT = 30
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
                        // No open match — create one and wait; start countdown to bot fallback
                        isPlayer1 = true
                        runCatching { repository.createMatch(myId, myName) }
                            .onSuccess { newMatchId ->
                                _uiState.update {
                                    it.copy(
                                        phase = ClashPhase.WAITING_FOR_OPPONENT,
                                        matchId = newMatchId,
                                        isLoading = false,
                                        isRandomWait = true,
                                        matchmakingSecondsLeft = BOT_MATCHMAKING_TIMEOUT
                                    )
                                }
                                startObserving(newMatchId)
                                startMatchmakingCountdown(newMatchId)
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

    // ─── Matchmaking countdown & bot fallback ────────────────────────────────

    private fun startMatchmakingCountdown(waitingMatchId: String) {
        matchmakingTimerJob?.cancel()
        matchmakingTimerJob = viewModelScope.launch {
            for (remaining in BOT_MATCHMAKING_TIMEOUT - 1 downTo 0) {
                delay(1000L)
                _uiState.update { it.copy(matchmakingSecondsLeft = remaining) }
                if (remaining == 0) {
                    startBotMatch(waitingMatchId)
                }
            }
        }
    }

    private suspend fun startBotMatch(waitingMatchId: String) {
        // A real opponent's join can land after our countdown hits zero but before our
        // snapshot listener observes it (network latency on either side) — check-and-abandon
        // atomically so we never overwrite a match someone just joined. If this returns
        // false, a real player got there first; bail out and let the normal "dealing"/"active"
        // Firestore updates (still being observed — see below) carry the match forward.
        val claimedForBot = runCatching { repository.cancelWaitingMatchIfUnjoined(waitingMatchId) }
            .getOrDefault(false)
        if (!claimedForBot) return

        // Stop watching Firestore — the bot match runs entirely client-side.
        // Do NOT cancel matchmakingTimerJob here: this function runs inside that job,
        // so cancelling it would abort this function before the hands can be fetched.
        observeJob?.cancel()
        observeJob = null
        syncJob?.cancel()
        syncJob = null

        isBotMatchActive = true
        _uiState.update {
            it.copy(
                phase = ClashPhase.DEALING,
                isBotMatch = true,
                opponentName = "CPU",
                isLoading = true,
                matchId = null
            )
        }

        val myHand = runCatching { repository.fetchRandomHand() }.getOrDefault(emptyList())
        val fetchedBotHand = runCatching { repository.fetchRandomHand() }.getOrDefault(emptyList())

        if (myHand.isEmpty() || fetchedBotHand.isEmpty()) {
            // Reset back to LOBBY so the user isn't trapped on the loading screen
            _uiState.update { it.copy(isLoading = false, phase = ClashPhase.LOBBY, isBotMatch = false, error = "Couldn't fetch Pokémon. Check your connection and try again.") }
            return
        }

        myHand.forEach { pokemonCache[it.id] = it }
        fetchedBotHand.forEach { pokemonCache[it.id] = it }
        botHand = fetchedBotHand

        _uiState.update {
            it.copy(
                myHand = myHand,
                isLoading = false,
                phase = ClashPhase.SELECTING
            )
        }
        startRoundTimer()
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
        syncJob?.cancel()
        roundSync.value = null
        latestState = null
        observeJob = viewModelScope.launch {
            repository.observeMatch(matchId).collect { state ->
                latestState = state
                onMatchStateUpdate(state)
                roundSync.value = state
            }
        }
        syncJob = viewModelScope.launch {
            roundSync.filterNotNull().collect { reconcileRounds(it) }
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
                // A real player joined — cancel the bot countdown
                matchmakingTimerJob?.cancel()
                matchmakingTimerJob = null

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

                // Round number and scores are deliberately NOT taken from the raw snapshot —
                // they're derived from applied history in reconcileRounds so they can never
                // run ahead of (or disagree with) the reveal the player is looking at.
                // Lock flags are only meaningful for the round currently open for selection.
                val roundOpen = state.currentRound == appliedRounds + 1
                // When the doc is already a round ahead of what's been applied, a resolve just
                // landed and reconcileRounds is about to show its reveal — leave lock flags alone
                // so the UI doesn't flicker back to an unlocked state in between.
                if (current.phase != ClashPhase.REVEALING && roundOpen) {
                    _uiState.update {
                        it.copy(
                            myLocked = myLocked,
                            opponentLocked = opponentLocked,
                            opponentDisconnected = opponentDisconnected
                        )
                    }
                } else {
                    _uiState.update { it.copy(opponentDisconnected = opponentDisconnected) }
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
                if (myLocked && opponentLocked && !state.roundRevealed
                    && revealWrittenRound != state.currentRound
                ) {
                    pendingCard?.let { card ->
                        val round = state.currentRound
                        revealWrittenRound = round
                        viewModelScope.launch {
                            var written = false
                            repeat(3) {
                                if (!written) {
                                    written = runCatching {
                                        repository.revealMyCard(matchId, isPlayer1, card.id, round)
                                    }.isSuccess
                                    if (!written) delay(1000L)
                                }
                            }
                            // Allow a later snapshot to retry if every attempt failed
                            if (!written && revealWrittenRound == round) revealWrittenRound = 0
                        }
                    }
                }

                // Resolving the round and showing its reveal is handled by reconcileRounds,
                // driven off the persisted round history rather than this transient snapshot.
            }

            "finished" -> {
                // Stop timer and heartbeat — no point continuing them after the match ends
                timerJob?.cancel(); timerJob = null
                heartbeatJob?.cancel(); heartbeatJob = null
                // The final reveal + result screen are sequenced by reconcileRounds.
            }
        }
    }

    // ─── Round sync (real matches) ────────────────────────────────────────────

    private fun ClashMatchState.roundEntries(): List<RoundEntry> =
        completedRounds.mapNotNull { m ->
            val round = (m["round"] as? Number)?.toInt() ?: return@mapNotNull null
            RoundEntry(
                round = round,
                p1CardId = (m["p1CardId"] as? Number)?.toInt() ?: return@mapNotNull null,
                p2CardId = (m["p2CardId"] as? Number)?.toInt() ?: return@mapNotNull null,
                winner = m["winner"] as? String ?: "draw",
                p1Total = (m["p1Total"] as? Number)?.toDouble(),
                p2Total = (m["p2Total"] as? Number)?.toDouble()
            )
        }.sortedBy { it.round }

    private data class RoundEntry(
        val round: Int,
        val p1CardId: Int,
        val p2CardId: Int,
        val winner: String,
        val p1Total: Double?,
        val p2Total: Double?
    )

    /**
     * Single-writer-at-a-time worker (fed by a conflated flow) that brings the UI in line with
     * the match document: (1) resolves the open round if both cards are in, (2) applies every
     * persisted round the UI hasn't shown yet, (3) sequences the result screen.
     */
    private suspend fun reconcileRounds(state: ClashMatchState) {
        if (isBotMatchActive) return

        if (state.status == "active"
            && state.roundRevealed
            && state.roundP1CardId != -1
            && state.roundP2CardId != -1
            && state.roundEntries().none { it.round == state.currentRound }
        ) {
            resolveOpenRound(state)
        }

        applyPersistedRounds(state)

        if (state.status == "finished") handleFinished(state)
    }

    private suspend fun resolveCard(id: Int): ClashPokemon? {
        pokemonCache[id]?.let { return it }
        val card = pendingCard?.takeIf { it.id == id } ?: repository.fetchPokemonById(id)
        if (card != null) pokemonCache[id] = card
        return card
    }

    /**
     * Both players run this; the repository transaction makes whichever lands second a no-op,
     * so there's no dependence on player 1 being online/fast and no double-counting.
     */
    private suspend fun resolveOpenRound(state: ClashMatchState) {
        val p1Card = resolveCard(state.roundP1CardId) ?: return
        val p2Card = resolveCard(state.roundP2CardId) ?: return

        val p1Eff = p1Card.bst * duelEngine.computeAdvantage(p1Card.toDuel(), p2Card.toDuel())
        val p2Eff = p2Card.bst * duelEngine.computeAdvantage(p2Card.toDuel(), p1Card.toDuel())
        val winner = when {
            p1Eff > p2Eff -> "player1"
            p2Eff > p1Eff -> "player2"
            else -> "draw"
        }

        repeat(3) { attempt ->
            val ok = runCatching {
                repository.resolveRound(
                    matchId = state.matchId,
                    roundNumber = state.currentRound,
                    p1CardId = state.roundP1CardId,
                    p2CardId = state.roundP2CardId,
                    roundWinner = winner,
                    isFinalRound = state.currentRound >= TOTAL_ROUNDS
                )
            }.isSuccess
            if (ok) return
            if (attempt < 2) delay(1000L)
        }
    }

    private suspend fun applyPersistedRounds(state: ClashMatchState) {
        val fresh = state.roundEntries().filter { it.round > appliedRounds }
        if (fresh.isEmpty()) return

        var lastRound: ClashRound? = null
        for (entry in fresh) {
            // Rounds must be applied strictly in order; a gap means we can't trust the tally.
            if (entry.round != appliedRounds + 1) break

            val myId = if (isPlayer1) entry.p1CardId else entry.p2CardId
            val oppId = if (isPlayer1) entry.p2CardId else entry.p1CardId
            val myCard = resolveCard(myId) ?: run {
                _uiState.update { it.copy(error = "Card data missing — please reconnect.") }
                break
            }
            val oppCard = resolveCard(oppId) ?: run {
                _uiState.update { it.copy(error = "Opponent card data missing.") }
                break
            }

            val myEff = myCard.bst * duelEngine.computeAdvantage(myCard.toDuel(), oppCard.toDuel())
            val oppEff = oppCard.bst * duelEngine.computeAdvantage(oppCard.toDuel(), myCard.toDuel())
            val winner = when (entry.winner) {
                "player1" -> if (isPlayer1) RoundWinner.ME else RoundWinner.OPPONENT
                "player2" -> if (isPlayer1) RoundWinner.OPPONENT else RoundWinner.ME
                else -> RoundWinner.DRAW
            }

            val clashRound = ClashRound(
                roundNumber = entry.round,
                myCard = myCard,
                opponentCard = oppCard,
                myScore = myEff,
                opponentScore = oppEff,
                winner = winner
            )

            appliedRounds = entry.round
            _uiState.update { s ->
                val p1Total = entry.p1Total
                val p2Total = entry.p2Total
                s.copy(
                    roundHistory = s.roundHistory + clashRound,
                    myUsedIds = s.myUsedIds + myId,
                    opponentRevealedCards = s.opponentRevealedCards + oppCard,
                    currentRound = minOf(entry.round + 1, TOTAL_ROUNDS),
                    myScore = (if (isPlayer1) p1Total else p2Total)?.toFloat() ?: s.myScore,
                    opponentScore = (if (isPlayer1) p2Total else p1Total)?.toFloat() ?: s.opponentScore
                )
            }
            lastRound = clashRound
        }

        // Show the reveal for the newest round only — if several were missed (e.g. after a
        // network blip) they're already in history and don't need replaying one by one.
        lastRound?.let { round ->
            timerJob?.cancel(); timerJob = null
            _uiState.update {
                it.copy(
                    phase = ClashPhase.REVEALING,
                    revealRound = round,
                    revealMyCard = round.myCard,
                    revealOpponentCard = round.opponentCard,
                    myLocked = true,
                    opponentLocked = true
                )
            }
        }
    }

    private fun handleFinished(state: ClashMatchState) {
        val phase = _uiState.value.phase
        if (phase == ClashPhase.MATCH_FINISHED) return
        // A "finished" doc is only a real result once a game is under way; ignore it while we're
        // still in the lobby/waiting/dealing phases (e.g. the bot-fallback cancel of an empty room).
        if (phase != ClashPhase.SELECTING && phase != ClashPhase.REVEALING) return
        // Everything the doc knows about must be on screen before we conclude.
        if (state.roundEntries().size > appliedRounds) return

        pendingOutcome = when (state.winner) {
            "player1" -> if (isPlayer1) MatchOutcome.WIN else MatchOutcome.LOSE
            "player2" -> if (!isPlayer1) MatchOutcome.WIN else MatchOutcome.LOSE
            else -> MatchOutcome.DRAW
        }
        // If the player is watching the final reveal, let them finish it (acknowledgeReveal
        // concludes the match); otherwise conclude right away (e.g. opponent forfeited).
        if (phase != ClashPhase.REVEALING) finalizeMatch()
    }

    private fun finalizeMatch() {
        val outcome = pendingOutcome ?: return
        if (_uiState.value.phase == ClashPhase.MATCH_FINISHED) return
        pendingOutcome = null

        timerJob?.cancel(); timerJob = null
        heartbeatJob?.cancel(); heartbeatJob = null

        val doc = latestState
        _uiState.update {
            it.copy(
                phase = ClashPhase.MATCH_FINISHED,
                revealMyCard = null,
                revealOpponentCard = null,
                revealRound = null,
                myScore = doc?.let { d -> (if (isPlayer1) d.p1Score else d.p2Score).toFloat() } ?: it.myScore,
                opponentScore = doc?.let { d -> (if (isPlayer1) d.p2Score else d.p1Score).toFloat() } ?: it.opponentScore,
                matchOutcome = outcome
            )
        }
        awardXp(outcome, _uiState.value.roundHistory.count { it.winner == RoundWinner.ME })
    }

    // ─── Bot round reveal (fully local, no Firestore) ─────────────────────────

    private fun triggerBotTurn(myCardId: Int) {
        viewModelScope.launch {
            // Simulate the bot "thinking" for a short natural-feeling delay
            delay(800L + Random.nextLong(0L, 700L))

            val botCardId = botHand.firstOrNull { it.id !in botUsedIds }?.id ?: return@launch
            botUsedIds = botUsedIds + botCardId

            _uiState.update { it.copy(opponentLocked = true) }
            processBotReveal(myCardId, botCardId)
        }
    }

    private suspend fun processBotReveal(myCardId: Int, botCardId: Int) {
        val myCard = pokemonCache[myCardId]
            ?: pendingCard?.takeIf { it.id == myCardId }
            ?: repository.fetchPokemonById(myCardId)
            ?: run {
                _uiState.update { it.copy(error = "Card data missing.") }
                return
            }
        pokemonCache[myCardId] = myCard

        val botCard = pokemonCache.getOrPut(botCardId) {
            repository.fetchPokemonById(botCardId) ?: run {
                _uiState.update { it.copy(error = "Bot card data missing.") }
                return
            }
        }

        val myEff = myCard.bst * duelEngine.computeAdvantage(myCard.toDuel(), botCard.toDuel())
        val botEff = botCard.bst * duelEngine.computeAdvantage(botCard.toDuel(), myCard.toDuel())

        val winner = when {
            myEff > botEff -> RoundWinner.ME
            botEff > myEff -> RoundWinner.OPPONENT
            else -> RoundWinner.DRAW
        }

        val (myDelta, botDelta) = when (winner) {
            RoundWinner.ME -> 1f to 0f
            RoundWinner.OPPONENT -> 0f to 1f
            RoundWinner.DRAW -> 0.5f to 0.5f
        }

        val clashRound = ClashRound(
            roundNumber = _uiState.value.currentRound,
            myCard = myCard,
            opponentCard = botCard,
            myScore = myEff,
            opponentScore = botEff,
            winner = winner
        )

        _uiState.update { s ->
            s.copy(
                phase = ClashPhase.REVEALING,
                revealMyCard = myCard,
                revealOpponentCard = botCard,
                revealRound = clashRound,
                myScore = s.myScore + myDelta,
                opponentScore = s.opponentScore + botDelta,
                myUsedIds = s.myUsedIds + myCardId,
                opponentRevealedCards = s.opponentRevealedCards + botCard,
                roundHistory = s.roundHistory + clashRound
            )
        }
        // No Firestore writes needed — the bot match is entirely local
    }

    // ─── In-round actions ─────────────────────────────────────────────────────

    fun selectCard(pokemonId: Int) {
        if (_uiState.value.myLocked) return
        _uiState.update { it.copy(selectedCardId = pokemonId) }
    }

    fun lockCard() {
        val state = _uiState.value
        val matchId = state.matchId
        val cardId = state.selectedCardId ?: return
        if (state.myLocked) return

        timerJob?.cancel()
        timerJob = null
        pendingCard = pokemonCache[cardId]
        _uiState.update { it.copy(myLocked = true) }

        if (state.isBotMatch) {
            triggerBotTurn(cardId)
        } else {
            if (matchId == null) return
            viewModelScope.launch {
                runCatching { repository.lockCard(matchId, isPlayer1) }
                    .onFailure { _uiState.update { s -> s.copy(myLocked = false) } }
            }
        }
    }

    /** Called from the UI once the reveal animation finishes — ready for next round. */
    fun acknowledgeReveal() {
        val state = _uiState.value

        if (!state.isBotMatch) {
            acknowledgeRealMatchReveal()
            return
        }

        // Guard: if all 6 rounds have been played (or hand is exhausted), end the match.
        val roundsPlayed = state.roundHistory.size
        val handEmpty = state.myUsedIds.size >= state.myHand.size && state.myHand.isNotEmpty()
        if (roundsPlayed >= 6 || handEmpty) {
            if (state.isBotMatch) {
                // Finish the bot match locally — no Firestore to deliver "finished" status
                val outcome = when {
                    state.myScore > state.opponentScore -> MatchOutcome.WIN
                    state.opponentScore > state.myScore -> MatchOutcome.LOSE
                    else -> MatchOutcome.DRAW
                }
                _uiState.update {
                    it.copy(
                        phase = ClashPhase.MATCH_FINISHED,
                        revealMyCard = null,
                        revealOpponentCard = null,
                        revealRound = null,
                        matchOutcome = outcome
                    )
                }
                awardXp(outcome, state.roundHistory.count { it.winner == RoundWinner.ME })
            } else {
                // For real matches, Firestore will deliver the "finished" status imminently.
                // Keep the user on the reveal screen until that update arrives.
                _uiState.update {
                    it.copy(
                        revealMyCard = null,
                        revealOpponentCard = null,
                        revealRound = null
                    )
                }
            }
            return
        }

        pendingCard = null
        _uiState.update {
            it.copy(
                phase = ClashPhase.SELECTING,
                // For bot matches, advance the round counter locally (Firestore does it for real matches)
                currentRound = if (state.isBotMatch) state.currentRound + 1 else state.currentRound,
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

    private fun acknowledgeRealMatchReveal() {
        // Match already concluded (final round, or opponent forfeited while we watched)
        if (pendingOutcome != null) {
            finalizeMatch()
            return
        }
        // Last round watched but the doc hasn't flipped to "finished" yet — resolveRound writes
        // both in one transaction so this is only a transient gap; handleFinished will conclude.
        if (appliedRounds >= TOTAL_ROUNDS) {
            _uiState.update { it.copy(revealMyCard = null, revealOpponentCard = null, revealRound = null) }
            return
        }

        pendingCard = null
        // The opponent may already have locked their next card while we were on the reveal
        // screen; seed the flag from the latest snapshot so it isn't wrongly shown as "choosing".
        val doc = latestState
        val roundOpen = doc != null && doc.status == "active" && doc.currentRound == appliedRounds + 1
        val oppLockedAlready = roundOpen && (if (isPlayer1) doc!!.roundP2Locked else doc!!.roundP1Locked)

        _uiState.update {
            it.copy(
                phase = ClashPhase.SELECTING,
                selectedCardId = null,
                myLocked = false,
                opponentLocked = oppLockedAlready,
                revealMyCard = null,
                revealOpponentCard = null,
                revealRound = null,
                timerSeconds = ROUND_TIMER_SECONDS,
                opponentDisconnected = false
            )
        }
        startRoundTimer()
    }

    /** Claims the win when opponent has been disconnected for over a minute. Not applicable to bot matches. */
    fun forfeitOpponent() {
        val state = _uiState.value
        if (state.isBotMatch) return
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
            // XP for each round won — emitted individually so the toast fires per round
            repeat(roundsWon) {
                val r = xpManager.awardGameXP(XPEvent.CardClashRoundWin)
                if (r.xpGained > 0) _xpResult.emit(r)
            }
            // Match-level XP based on outcome
            when (outcome) {
                MatchOutcome.WIN -> {
                    val winResult = xpManager.awardGameXP(XPEvent.CardClashWin)
                    if (winResult.xpGained > 0) _xpResult.emit(winResult)
                    if (roundsWon == 6) {
                        val perfectResult = xpManager.awardGameXP(XPEvent.CardClashPerfect)
                        if (perfectResult.xpGained > 0) _xpResult.emit(perfectResult)
                    }
                }
                MatchOutcome.DRAW -> {
                    val drawResult = xpManager.awardGameXP(XPEvent.CardClashDraw)
                    if (drawResult.xpGained > 0) _xpResult.emit(drawResult)
                }
                MatchOutcome.LOSE -> Unit // losing still earns per-round XP above
            }
        }
    }

    /**
     * Called when the user backs out of the waiting screen. Unlike [reset], this also marks
     * the Firestore room we created as no longer waiting — otherwise it lingers forever as a
     * dead entry that a future player can match into and then never hear from again. This was
     * previously the biggest reason matchmaking felt broken: `onCancelWait` only cleared local
     * state, so every abandoned wait (back button, backgrounding, force-close) left a permanent
     * zombie room behind.
     */
    fun cancelWait() {
        val state = _uiState.value
        val matchId = state.matchId
        if (matchId != null && !state.isBotMatch && state.phase == ClashPhase.WAITING_FOR_OPPONENT) {
            viewModelScope.launch {
                runCatching { repository.finishMatch(matchId, "cancelled", 0.0, 0.0) }
            }
        }
        reset()
    }

    // ─── Reset ────────────────────────────────────────────────────────────────

    fun reset() {
        observeJob?.cancel()
        syncJob?.cancel()
        timerJob?.cancel()
        heartbeatJob?.cancel()
        matchmakingTimerJob?.cancel()
        observeJob = null
        syncJob = null
        timerJob = null
        heartbeatJob = null
        matchmakingTimerJob = null
        pendingCard = null
        pokemonCache.clear()
        appliedRounds = 0
        latestState = null
        roundSync.value = null
        revealWrittenRound = 0
        pendingOutcome = null
        isBotMatchActive = false
        botHand = emptyList()
        botUsedIds = emptySet()
        _uiState.value = ClashUiState()
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
        syncJob?.cancel()
        timerJob?.cancel()
        heartbeatJob?.cancel()
        matchmakingTimerJob?.cancel()
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
        if (state.myLocked) return
        // For bot matches there's no matchId, so skip the null-matchId guard
        if (!state.isBotMatch && state.matchId == null) return
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
