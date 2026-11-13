package com.aditya1875.pokeverse.feature.game.battle.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.evolution.domain.EvolutionEconomy
import com.aditya1875.pokeverse.feature.game.battle.data.FirestoreOnlineBattleRepository
import com.aditya1875.pokeverse.feature.game.battle.data.OnlineBattleRepository
import com.aditya1875.pokeverse.feature.game.battle.data.OnlineMatch
import com.aditya1875.pokeverse.feature.game.battle.data.OnlineRewardLedger
import com.aditya1875.pokeverse.feature.game.battle.data.OnlineSnapshot
import com.aditya1875.pokeverse.feature.game.core.domain.Matchmaking
import com.aditya1875.pokeverse.feature.game.core.domain.YieldResult
import com.aditya1875.pokeverse.feature.game.battle.data.loadFighter
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleAction
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEngine
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEvent
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleState
import com.aditya1875.pokeverse.feature.game.battle.domain.Who
import com.aditya1875.pokeverse.feature.game.battle.domain.mirrored
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineAction
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineBattleRules
import com.aditya1875.pokeverse.feature.game.battle.domain.online.Seat
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.pokemon.dex.DexProgress
import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexRepository
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.utils.LoadErrorKind
import com.aditya1875.pokeverse.utils.toLoadErrorKind
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Online Team Battle against another trainer, in real time.
 *
 * The room, both teams (as Pokémon ids) and every choice live in Firestore; the battle itself is replayed
 * on each phone from that log (see [OnlineBattleRules]). This class runs the matchmaking, keeps the replay
 * in step with the log, runs your turn clock, watches your rival's presence, and pays the rewards.
 *
 * A quick match that finds nobody within [OnlineBattleRules.QUEUE_SECONDS] hands over to a practice battle
 * against the bot ([fallbackToBot]); the screen starts it with the same team.
 */
class OnlineBattleViewModel(
    private val repo: OnlineBattleRepository,
    private val details: PokemonDetailRepo,
    private val pokedex: PokedexRepository,
    private val xpManager: XPManager,
    private val userRepository: UserProfileRepository,
    private val ledger: OnlineRewardLedger,
    private val imageLoader: ImageLoader,
    private val appContext: Context,
) : ViewModel(), BattleHost {

    enum class Stage { IDLE, SEARCHING, PREPARING, BATTLING, FAILED }

    data class Lobby(
        val stage: Stage = Stage.IDLE,
        /** True for a friend's room (waits for whoever has the code), false for a quick match. */
        val friendRoom: Boolean = false,
        val roomCode: String = "",
        /** Quick match: seconds until a bot takes the other seat. */
        val secondsLeft: Int = 0,
        val enteredCode: String = "",
        /** A request is in flight (joining or creating). */
        val busy: Boolean = false,
        val opponentName: String? = null,
        val team: List<Int> = emptyList(),
        @StringRes val errorRes: Int? = null,
        /** The number the error message mentions (a room's required team size), if it has one. */
        val errorArg: Int? = null,
        val loadError: LoadErrorKind? = null,
    )

    private val _lobby = MutableStateFlow(Lobby())
    val lobby: StateFlow<Lobby> = _lobby.asStateFlow()

    private val _battle = MutableStateFlow<BattleState?>(null)
    /** The battle from your own seat: you are always the player, whichever seat you sit in. */
    val battle: StateFlow<BattleState?> = _battle.asStateFlow()

    private val _events = MutableSharedFlow<List<BattleEvent>>(extraBufferCapacity = 64)
    override val events: SharedFlow<List<BattleEvent>> = _events.asSharedFlow()

    private val _battleKey = MutableStateFlow(0)
    override val battleKey: StateFlow<Int> = _battleKey.asStateFlow()

    private val _shards = MutableStateFlow(0)
    override val shardsGained: StateFlow<Int> = _shards.asStateFlow()

    private val _shardsCapped = MutableStateFlow(false)
    override val shardsCapped: StateFlow<Boolean> = _shardsCapped.asStateFlow()

    private val _hud = MutableStateFlow(ArenaHud(online = true))
    override val hud: StateFlow<ArenaHud> = _hud.asStateFlow()

    private val _rivalReactions = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    override val rivalReactions: kotlinx.coroutines.flow.Flow<Int> = _rivalReactions.asSharedFlow()

    /** Counter of the rival's last reaction already shown; null until the room's first snapshot sets it. */
    private var rivalReactSeen: Long? = null
    private var lastReactSentAt = 0L

    override fun react(index: Int) {
        val id = matchId ?: return
        if (index !in com.aditya1875.pokeverse.feature.game.battle.domain.Reactions.ALL.indices) return
        val now = System.currentTimeMillis()
        if (now - lastReactSentAt < com.aditya1875.pokeverse.feature.game.battle.domain.Reactions.SEND_GAP_MS) return
        lastReactSentAt = now
        val mySeat = seat
        viewModelScope.launch { repo.react(id, mySeat, index, now) }
    }

    private val _xp = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpResult: SharedFlow<XPResult> = _xp.asSharedFlow()

    private val _fallback = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Nobody was online in time: the screen should start a practice battle with this team instead. */
    val fallbackToBot: SharedFlow<Unit> = _fallback.asSharedFlow()

    /** Writes that must outlive the screen (leaving a battle, closing a room) run here, not in viewModelScope. */
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The name the player set in their Dexverse profile: what a rival sees, never the email or Google name. */
    private var profileName: String? = null

    init {
        viewModelScope.launch { userRepository.profileFlow.collect { profileName = it.username } }
    }

    // ── Match bookkeeping ────────────────────────────────────────────────────────
    private var matchId: String? = null
    private var seat = Seat.HOST
    private var uid = ""
    private var myName = ""
    private var myTeam: List<Int> = emptyList()

    private var latest: OnlineSnapshot? = null
    private var initial: BattleState? = null
    private var canonical: BattleState? = null
    private var seed = 0L
    private var nextStep = 1
    private var preparing = false
    private var prepared = false
    private var ended = false
    private var submittedStep = -1
    private var timerStep = -1
    /** Turns the clock has played for each seat, counted from the log exactly as the replay does. */
    private val misses = mutableMapOf<Seat, Int>()

    private var observeJob: Job? = null
    private var countdownJob: Job? = null
    private var prepTimeoutJob: Job? = null
    private var timerJob: Job? = null
    private var heartbeatJob: Job? = null
    private var presenceJob: Job? = null

    // ── Lobby actions ────────────────────────────────────────────────────────────

    fun setCode(code: String) = _lobby.update { it.copy(enteredCode = code.uppercase().filter(Char::isLetterOrDigit).take(6), errorRes = null) }

    /** Looks for a real trainer; after [OnlineBattleRules.QUEUE_SECONDS] with nobody, falls back to a bot. */
    fun quickMatch(team: List<Int>) {
        // The standard format: exactly the same number on both sides, with at most one Legendary.
        if (!OnlineBattleRules.quickTeamOk(team)) return
        if (!begin(team, friend = false)) return
        viewModelScope.launch {
            val joined = runCatching { repo.joinQuick(uid, myName, team) }.logged("joinQuick")
            if (joined.isFailure) return@launch failLobby(R.string.battle_online_err_generic)
            val roomId = joined.getOrNull()
            if (roomId != null) {
                // Someone was already waiting: take the empty seat and go straight to the battlefield.
                return@launch enterAsGuest(roomId)
            }
            // Nobody waiting: open a room and wait. While waiting, the countdown keeps looking for other
            // waiting players, so two people who tapped at the same moment still find each other.
            runCatching { repo.createRoom(uid, myName, OnlineMatch.MODE_QUICK, team) }.logged("createRoom(quick)")
                .onSuccess { room ->
                    openMatch(room.id, Seat.HOST)
                    _lobby.update { it.copy(busy = false, secondsLeft = OnlineBattleRules.QUEUE_SECONDS) }
                    startQueueCountdown(team)
                }
                .onFailure { failLobby(R.string.battle_online_err_generic) }
        }
    }

    /**
     * How many other trainers are waiting for a quick match of [teamSize] right now, live. 3 vs 3 and 6 vs 6
     * are separate pools. Collect it only while it is on screen.
     */
    fun poolFlow(teamSize: Int): Flow<Int> = flow {
        val me = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        emitAll(repo.observeQuickPool(me, teamSize))
    }.distinctUntilChanged()

    /**
     * The "couldn't reach the battle server" message covers every failure to open or join a room (no network,
     * permission denied, a missing index...), and used to throw the real reason away. This keeps it: in the
     * log, and as a non-fatal in Crashlytics tagged with what was being attempted.
     */
    private fun <T> Result<T>.logged(what: String): Result<T> = onFailure { e ->
        android.util.Log.w("OnlineBattle", "$what failed: ${e::class.java.simpleName}: ${e.message}", e)
        runCatching { com.aditya1875.pokeverse.utils.NetworkCrashGuard.reporter?.invoke(RuntimeException("OnlineBattle $what failed", e)) }
    }

    /** Takes the guest seat of [roomId], which this phone has already claimed, and moves on to the battlefield. */
    private fun enterAsGuest(roomId: String) {
        openMatch(roomId, Seat.GUEST)
        _lobby.update { it.copy(stage = Stage.PREPARING, busy = false) }
        armPrepTimeout()
    }

    /** Opens a private room and waits for a friend to enter its code. */
    fun createFriendRoom(team: List<Int>) {
        if (!begin(team, friend = true)) return
        viewModelScope.launch {
            runCatching { repo.createRoom(uid, myName, OnlineMatch.MODE_FRIEND, team) }.logged("createRoom(friend)")
                .onSuccess { room ->
                    openMatch(room.id, Seat.HOST)
                    _lobby.update { it.copy(busy = false, roomCode = room.roomCode) }
                }
                .onFailure { failLobby(R.string.battle_online_err_generic) }
        }
    }

    fun joinFriendRoom(team: List<Int>) {
        val code = _lobby.value.enteredCode
        if (code.length != 6 || !begin(team, friend = true)) return
        viewModelScope.launch {
            val result = runCatching { repo.joinByCode(code, uid, myName, team) }.logged("joinByCode")
            (result.exceptionOrNull() as? com.aditya1875.pokeverse.feature.game.battle.data.TeamSizeMismatch)?.let {
                return@launch failLobby(R.string.battle_online_err_size, it.required)
            }
            if (result.isFailure) return@launch failLobby(R.string.battle_online_err_generic)
            val roomId = result.getOrNull() ?: return@launch failLobby(R.string.battle_online_err_room)
            openMatch(roomId, Seat.GUEST)
            _lobby.update { it.copy(stage = Stage.PREPARING, busy = false) }
            armPrepTimeout()
        }
    }

    /** Backs out of the waiting room. Closes the room so nobody can match into an empty seat. */
    fun cancelSearch() {
        val id = matchId
        val mySeat = seat
        if (id != null) bgScope.launch {
            // If someone slipped in at the last moment, leaving counts as quitting that battle.
            if (!repo.cancelWaitingIfUnjoined(id)) repo.quit(id, mySeat)
        }
        reset()
    }

    /** Leaves the screen. A battle in progress is forfeited; a waiting room is closed. */
    fun leave() {
        val id = matchId
        if (id != null) when (_lobby.value.stage) {
            Stage.BATTLING -> if (!ended) bgScope.launch { repo.quit(id, seat) }
            Stage.SEARCHING, Stage.PREPARING -> {
                val mySeat = seat
                bgScope.launch { if (!repo.cancelWaitingIfUnjoined(id)) repo.quit(id, mySeat) }
            }
            else -> Unit
        }
        reset()
    }

    fun reset() {
        cancelJobs()
        matchId = null
        latest = null; initial = null; canonical = null
        nextStep = 1; preparing = false; prepared = false; ended = false
        submittedStep = -1; timerStep = -1
        misses.clear()
        _battle.value = null
        _hud.value = ArenaHud(online = true)
        _shards.value = 0
        _shardsCapped.value = false
        _lobby.value = Lobby()
    }

    // ── Starting ─────────────────────────────────────────────────────────────────

    /** Common start: checks sign-in and clears the last match. Returns false (with an error shown) if it can't go. */
    private fun begin(team: List<Int>, friend: Boolean): Boolean {
        if (_lobby.value.busy || _lobby.value.stage != Stage.IDLE) return false
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _lobby.update { it.copy(errorRes = R.string.battle_online_err_signin) }
            return false
        }
        if (team.isEmpty()) return false
        uid = user.uid
        myName = (profileName?.takeIf { it.isNotBlank() } ?: user.displayName?.takeIf { it.isNotBlank() } ?: "Trainer").take(30)
        myTeam = team
        val code = _lobby.value.enteredCode
        _lobby.value = Lobby(stage = Stage.SEARCHING, friendRoom = friend, busy = true, team = team, enteredCode = code)
        return true
    }

    private fun failLobby(@StringRes error: Int, arg: Int? = null) {
        cancelJobs()
        matchId = null
        _lobby.update { Lobby(enteredCode = it.enteredCode, errorRes = error, errorArg = arg) }
    }

    private fun openMatch(id: String, mySeat: Seat) {
        matchId = id
        seat = mySeat
        ended = false; preparing = false; prepared = false
        nextStep = 1; submittedStep = -1; timerStep = -1
        rivalReactSeen = null
        latest = null; initial = null; canonical = null
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repo.observe(id).collect { snap -> onSnapshot(snap) }
        }
        startHeartbeat(id)
    }

    private fun startQueueCountdown(team: List<Int>) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val total = OnlineBattleRules.QUEUE_SECONDS
            for (elapsed in 0 until total) {
                _lobby.update { it.copy(secondsLeft = total - elapsed) }
                delay(1000L)
                // Two players who tapped "play" together each opened a room and cannot see the other's.
                // Look again every couple of seconds; if this search ends here (matched or failed), stop.
                if (Matchmaking.shouldLookNow(elapsed + 1) && !lookForAnotherWaitingTrainer(team)) return@launch
            }
            _lobby.update { it.copy(secondsLeft = 0) }
            // Close the empty room atomically. If a real trainer joined in the meantime this returns false
            // and the normal snapshot updates carry that match forward instead.
            val room = matchId ?: return@launch
            if (repo.cancelWaitingIfUnjoined(room)) {
                cancelJobs()
                matchId = null
                val keep = _lobby.value.enteredCode
                _lobby.value = Lobby(enteredCode = keep)
                _fallback.tryEmit(Unit)
            }
        }
    }

    /**
     * One look for another waiting trainer. Only the player whose room id is larger gives way, so of two
     * players waiting together exactly one moves and they cannot join each other's rooms at once.
     * Returns true to keep waiting, false when this search is finished (matched, or it failed).
     */
    private suspend fun lookForAnotherWaitingTrainer(team: List<Int>): Boolean {
        val mine = matchId ?: return false
        val other = repo.findRoomToYieldTo(mine, uid, team.size) ?: return true
        // Closing our room and taking the other seat must not be cut in half by the player leaving.
        val result = withContext(NonCancellable) {
            runCatching { repo.yieldToRoom(mine, other, uid, myName, team) }.getOrDefault(YieldResult.STAYED)
        }
        if (!currentCoroutineContext().isActive) {
            // They left while this ran: don't bring the screen back, and give the seat up.
            if (result == YieldResult.JOINED) bgScope.launch { repo.quit(other, Seat.GUEST) }
            return false
        }
        return when (result) {
            YieldResult.JOINED -> { enterAsGuest(other); false }
            // Someone joined our own room first: that is the real match, and its snapshot carries it on.
            YieldResult.STAYED -> true
            // Our room is closed but the other one was taken: search again with the time that is left.
            YieldResult.LOST -> reopenSearch(team)
        }
    }

    private suspend fun reopenSearch(team: List<Int>): Boolean {
        val joined = runCatching { repo.joinQuick(uid, myName, team) }.logged("reopen joinQuick").getOrNull()
        if (joined != null) { enterAsGuest(joined); return false }
        val room = runCatching { repo.createRoom(uid, myName, OnlineMatch.MODE_QUICK, team) }.logged("reopen createRoom").getOrNull()
        if (room == null) { failLobby(R.string.battle_online_err_generic); return false }
        openMatch(room.id, Seat.HOST)
        return true
    }

    /** A joined player who never shows up (or never loads) must not leave this phone spinning forever. */
    private fun armPrepTimeout() {
        prepTimeoutJob?.cancel()
        prepTimeoutJob = viewModelScope.launch {
            delay(PREP_TIMEOUT_MS)
            if (_lobby.value.stage == Stage.PREPARING) failBattle(null, R.string.battle_online_err_prep)
        }
    }

    // ── The room, live ───────────────────────────────────────────────────────────

    private fun onSnapshot(s: OnlineSnapshot) {
        latest = s
        val m = s.match
        // A rival's reaction: show only ones made after we arrived, once each.
        val rivalReact = m.reactionOf(seat.other)
        val seen = rivalReactSeen
        if (seen == null) rivalReactSeen = rivalReact?.second ?: 0L
        else if (rivalReact != null && rivalReact.second != seen) {
            rivalReactSeen = rivalReact.second
            if (_lobby.value.stage == Stage.BATTLING) _rivalReactions.tryEmit(rivalReact.first)
        }
        val stage = _lobby.value.stage

        if (stage == Stage.SEARCHING && m.hasGuest) {
            // A trainer took the empty seat.
            countdownJob?.cancel()
            _lobby.update { it.copy(stage = Stage.PREPARING, opponentName = m.nameOf(seat.other).ifBlank { null }) }
            armPrepTimeout()
        }
        if (_lobby.value.stage == Stage.PREPARING) {
            _lobby.update { it.copy(opponentName = m.nameOf(seat.other).ifBlank { it.opponentName }) }
            // The room closed before the battle began (the other side left): nothing to play. This holds even
            // when this phone was already ready, otherwise it would sit here until the preparation timeout.
            if (m.finished) return failBattle(null, R.string.battle_online_err_prep)
            prepareFighters(s)
            if (prepared && s.actions[0]?.keys?.containsAll(Seat.entries) == true) startBattle()
        }
        if (_lobby.value.stage == Stage.BATTLING) reconcile(s)
    }

    /** Builds both teams from their ids, warms the sprites, and tells the rival this phone is ready. */
    private fun prepareFighters(s: OnlineSnapshot) {
        if (preparing) return
        val host = s.teams[Seat.HOST]
        val guest = s.teams[Seat.GUEST]
        if (host == null || guest == null) return
        preparing = true
        viewModelScope.launch {
            try {
                if (!OnlineBattleRules.validTeam(host, DexProgress.TOTAL) || !OnlineBattleRules.validTeam(guest, DexProgress.TOTAL) ||
                    !OnlineBattleRules.evenTeams(host, guest)
                ) {
                    return@launch failBattle(null, R.string.battle_online_err_prep)
                }
                val (hostFighters, guestFighters) = coroutineScope {
                    val h = host.map { id -> async { details.loadFighter(id).first } }
                    val g = guest.map { id -> async { details.loadFighter(id).first } }
                    h.awaitAll() to g.awaitAll()
                }
                withTimeoutOrNull(6_000) {
                    coroutineScope {
                        (hostFighters + guestFighters).map { f ->
                            async { runCatching { imageLoader.execute(ImageRequest.Builder(appContext).data(f.spriteUrl).build()) } }
                        }.awaitAll()
                    }
                }
                initial = BattleEngine.start(hostFighters, guestFighters)
                seed = s.match.seed
                repo.submitAction(s.match.id, uid, seat, 0, OnlineAction(FirestoreOnlineBattleRepository.KIND_READY))
                prepared = true
                latest?.let { onSnapshot(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failBattle(e.toLoadErrorKind(), null)
            }
        }
    }

    private fun startBattle() {
        val start = initial ?: return
        if (_lobby.value.stage == Stage.BATTLING) return
        prepTimeoutJob?.cancel()
        canonical = start
        nextStep = 1
        ended = false
        submittedStep = -1
        timerStep = -1
        misses.clear()
        _shards.value = 0
        _shardsCapped.value = false
        _battleKey.value += 1
        _battle.value = view(start)
        _hud.value = ArenaHud(online = true, opponentName = _lobby.value.opponentName, waiting = false)
        _lobby.update { it.copy(stage = Stage.BATTLING) }
        startPresence()
        latest?.let { reconcile(it) }
    }

    // ── Replaying the log ────────────────────────────────────────────────────────

    /** Brings the battle up to date with the log: plays every step it can complete, then checks for a forfeit. */
    private fun reconcile(s: OnlineSnapshot) {
        var state = canonical ?: return
        if (ended) return

        var disqualified: Seat? = null
        while (!state.over) {
            val entries = s.actions[nextStep] ?: break
            if (!OnlineBattleRules.pendingSeats(state).all { it in entries }) break
            // Two turns played by the clock and you are out: the step is not played, the rival wins.
            disqualified = OnlineBattleRules.countMisses(misses, entries)
            if (disqualified != null) break
            val result = OnlineBattleRules.resolve(state, nextStep, seed, entries)
            state = result.state
            nextStep++
            // The step is done: its clock stops, and the next one starts once the animation has played.
            timerJob?.cancel(); timerStep = -1
            _hud.update { it.copy(secondsLeft = null) }
            canonical = state
            _battle.value = view(state)
            _events.tryEmit(if (seat == Seat.HOST) result.events else result.events.map { it.mirrored() })
        }

        _hud.update { it.copy(myMissedTurns = misses[seat] ?: 0) }
        val outcome = OnlineBattleRules.outcome(state, s.match.quitSeat, s.match.abandonedSeat, disqualified)
        if (outcome != null) {
            finishBattle(state, outcome.winner, outcome.byLeaving, outcome.disqualified)
        } else {
            refreshHud(s)
        }
    }

    private fun view(state: BattleState): BattleState = if (seat == Seat.HOST) state else state.mirrored()

    private fun refreshHud(s: OnlineSnapshot) {
        val state = canonical ?: return
        val pending = OnlineBattleRules.pendingSeats(state)
        val entries = s.actions[nextStep].orEmpty()
        val iNeed = seat in pending
        val mySubmitted = seat in entries || submittedStep == nextStep
        _hud.update {
            it.copy(
                waiting = !state.over && (!iNeed || mySubmitted),
                opponentLocked = seat.other in entries,
                // Shown to both players for as long as anyone still has to choose.
                secondsLeft = if (!state.over && pending.isNotEmpty()) it.secondsLeft else null,
            )
        }
    }

    // ── Choosing ─────────────────────────────────────────────────────────────────

    override fun act(action: BattleAction, tera: Boolean, guard: Boolean) {
        val mine = _battle.value ?: return
        if (mine.over || mine.playerMustSwitch) return
        if (!BattleEngine.isLegal(mine, Who.PLAYER, action)) return
        submit(
            OnlineAction.of(
                action,
                tera = tera && BattleEngine.canTera(mine, Who.PLAYER, action),
                // A Guard only goes with an Attack or a Special, and only while there is one left.
                guard = guard && BattleEngine.canGuard(mine, Who.PLAYER, action),
            )
        )
    }

    override fun chooseReplacement(index: Int) {
        val mine = _battle.value ?: return
        if (!mine.playerMustSwitch || mine.over) return
        val side = mine.player
        if (index !in side.team.indices || side.team[index].fainted) return
        submit(OnlineAction.replacement(index))
    }

    private fun submit(action: OnlineAction) {
        val id = matchId ?: return
        val state = canonical ?: return
        val step = nextStep
        if (ended || state.over || seat !in OnlineBattleRules.pendingSeats(state) || submittedStep == step) return
        submittedStep = step
        // The clock keeps running after you lock in: it is the rival's remaining time, and both players see it.
        _hud.update { it.copy(waiting = true) }
        viewModelScope.launch {
            var sent = false
            repeat(3) { attempt ->
                if (!sent) {
                    sent = runCatching { repo.submitAction(id, uid, seat, step, action) }.isSuccess
                    if (!sent && attempt < 2) delay(1000L)
                }
            }
            if (!sent && nextStep == step) {
                // The network ate it: let the player choose again instead of freezing the battle.
                submittedStep = -1
                timerStep = -1
                _hud.update { it.copy(waiting = false) }
                maybeStartTimer()
            }
        }
    }

    // ── Turn clock ───────────────────────────────────────────────────────────────

    override fun playbackFinished() {
        // The arena is idle again, so the next choice's clock can start without eating the animation time.
        latest?.let { refreshHud(it) }
        maybeStartTimer()
    }

    private fun maybeStartTimer() {
        val state = canonical ?: return
        if (_lobby.value.stage != Stage.BATTLING || ended || state.over) return
        val step = nextStep
        // The clock runs for whoever still has to choose, and both players watch it (yours, or your rival's).
        if (timerStep == step) return
        if (OnlineBattleRules.pendingSeats(state).isEmpty()) return
        timerStep = step
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (left in OnlineBattleRules.TURN_SECONDS downTo 1) {
                _hud.update { it.copy(secondsLeft = left) }
                delay(1000L)
            }
            // Out of time: the sensible move is played for you, so a stalled player never stalls the match.
            val now = canonical
            if (now != null && nextStep == step && submittedStep != step && seat in OnlineBattleRules.pendingSeats(now)) {
                // Marked as the clock's move: two of these and the player is disqualified.
                submit(OnlineBattleRules.defaultAction(now, seat).copy(auto = true))
            }
        }
    }

    // ── Presence ─────────────────────────────────────────────────────────────────

    private fun startHeartbeat(id: String) {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                repo.heartbeat(id, seat)
                delay(OnlineBattleRules.HEARTBEAT_MS)
            }
        }
    }

    /** Watches the rival. If the match is stuck on them and they've gone quiet, claim the win (the server checks). */
    private fun startPresence() {
        presenceJob?.cancel()
        presenceJob = viewModelScope.launch {
            while (!ended) {
                delay(PRESENCE_TICK_MS)
                val s = latest ?: continue
                val state = canonical ?: continue
                if (state.over) continue
                val last = s.match.heartbeatOf(seat.other)
                val silentFor = if (last > 0L) System.currentTimeMillis() - last else 0L
                val away = last > 0L && silentFor > OnlineBattleRules.AWAY_AFTER_MS
                _hud.update { it.copy(opponentAway = away) }

                val stuckOnThem = seat.other in OnlineBattleRules.pendingSeats(state) && seat.other !in s.actions[nextStep].orEmpty()
                if (stuckOnThem && silentFor > OnlineBattleRules.CLAIM_AFTER_MS + 5_000L) {
                    repo.claimAbandoned(s.match.id, seat.other)
                }
            }
        }
    }

    // ── Ending ───────────────────────────────────────────────────────────────────

    private fun finishBattle(state: BattleState, winner: Seat, byLeaving: Boolean, disqualified: Boolean = false) {
        if (ended) return
        ended = true
        timerJob?.cancel(); presenceJob?.cancel()
        val won = winner == seat
        if (!state.over) {
            // Nobody landed the last blow: the rival left, so the battle ends here with you ahead.
            _battle.value = view(state).copy(winner = if (won) Who.PLAYER else Who.FOE)
        }
        // A rival who walked out before the battle was really fought did not lose to anyone: it is not a win
        // for this player, and it is not a battle played, for either of them.
        val counts = OnlineBattleRules.countsForRecord(byLeaving, state.turn) || disqualified
        _hud.update {
            it.copy(
                waiting = false,
                secondsLeft = null,
                opponentLeft = byLeaving && won && !disqualified,
                opponentDisqualified = disqualified && won,
                iWasDisqualified = disqualified && !won,
                notCounted = !counts,
            )
        }
        val id = matchId
        if (id != null) bgScope.launch { repo.finish(id) }
        if (!counts) return
        // The record counts every finished battle, even a short one or another game with the same rival.
        // Only the shards and XP below are limited; the wins and losses never are.
        viewModelScope.launch {
            userRepository.incrementBattlesPlayed()
            if (won) {
                val wins = userRepository.profileFlow.first().bestBattleScore
                userRepository.updateBestScore("battle", wins + 1)
            }
        }
        // Someone who let the clock play for them twice does not earn from the battle.
        if (disqualified && !won) return
        payRewards(won, state.turn)
    }

    /** Shards and XP for a battle that was really fought, unless this rival has already paid out enough today. */
    private fun payRewards(won: Boolean, turns: Int) {
        val rival = latest?.match?.let { if (seat == Seat.HOST) it.guestId else it.hostId }.orEmpty()
        val paid = if (rival.isNotEmpty()) ledger.paidToday(rival) else Int.MAX_VALUE
        if (!OnlineBattleRules.paysRewards(turns, paid)) {
            _hud.update { it.copy(rewardsLimited = turns >= OnlineBattleRules.MIN_TURNS_FOR_REWARD) }
            return
        }
        ledger.recordPaid(rival)
        viewModelScope.launch {
            val wanted = EvolutionEconomy.forOnlineBattle(won)
            _shards.value = pokedex.awardShards(wanted)
            _shardsCapped.value = _shards.value < wanted
        }
        viewModelScope.launch {
            val result = xpManager.awardGameXP(XPEvent.OnlineBattleComplete(won))
            if (result.xpGained > 0) {
                _hud.update { it.copy(xpGained = result.xpGained) }
                _xp.emit(result)
            }
        }
    }

    /** Something went wrong before the battle began. The room is closed so the rival isn't left waiting. */
    private fun failBattle(kind: LoadErrorKind?, @StringRes message: Int?) {
        val id = matchId
        val mySeat = seat
        if (id != null) bgScope.launch { repo.quit(id, mySeat) }
        cancelJobs()
        matchId = null
        _lobby.update { Lobby(stage = Stage.FAILED, enteredCode = it.enteredCode, errorRes = message, loadError = kind, team = it.team) }
    }

    private fun cancelJobs() {
        observeJob?.cancel(); countdownJob?.cancel(); prepTimeoutJob?.cancel()
        timerJob?.cancel(); heartbeatJob?.cancel(); presenceJob?.cancel()
        observeJob = null; countdownJob = null; prepTimeoutJob = null
        timerJob = null; heartbeatJob = null; presenceJob = null
    }

    override fun onCleared() {
        // Closing the screen mid-battle is leaving it.
        val id = matchId
        if (id != null && !ended) {
            val st = _lobby.value.stage
            val mySeat = seat
            if (st == Stage.BATTLING) bgScope.launch { repo.quit(id, mySeat) }
            else if (st == Stage.SEARCHING || st == Stage.PREPARING) bgScope.launch {
                if (!repo.cancelWaitingIfUnjoined(id)) repo.quit(id, mySeat)
            }
        }
        cancelJobs()
        super.onCleared()
    }

    private companion object {
        const val PREP_TIMEOUT_MS = 25_000L
        const val PRESENCE_TICK_MS = 5_000L
    }
}
