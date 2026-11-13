package com.aditya1875.pokeverse.feature.game.battle.domain.online

import com.aditya1875.pokeverse.feature.game.battle.domain.BattleAction
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEngine
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleState
import com.aditya1875.pokeverse.feature.game.battle.domain.BotBrain
import com.aditya1875.pokeverse.feature.game.battle.domain.TurnResult
import com.aditya1875.pokeverse.feature.game.battle.domain.Who

/*
 * Online Team Battle, the rules both phones agree on.
 *
 * Nothing is "resolved" on a server. Each player writes ONE immutable action per step to a log, and both
 * phones replay that log through the same seeded [BattleEngine], so they always land on the same battle.
 * Rebuilding from the whole log (rather than reacting to each snapshot) is what makes a missed or
 * out-of-order update harmless.
 *
 * Steps: 0 is the "ready" handshake; every later step is either an ACTION round (both players act at once)
 * or a SWITCH round (only the side(s) whose Pokémon fainted pick a replacement). Which one it is follows
 * from the battle state alone, so nobody has to say it.
 *
 * Everything is computed from the HOST's seat (host = [Who.PLAYER]) so the dice fall identically; the guest
 * displays the mirrored result.
 */

/** Which chair a player sits in. The host created the room. */
enum class Seat(val code: String, val who: Who) {
    HOST("h", Who.PLAYER),
    GUEST("g", Who.FOE);

    val other: Seat get() = if (this == HOST) GUEST else HOST

    companion object {
        fun fromCode(code: String?): Seat? = entries.firstOrNull { it.code == code }
    }
}

/**
 * One recorded choice. For a switch, [index] is the bench slot; for everything else it is unused.
 * [auto] marks a choice the clock made because the player ran out of time: those are the "missed turns".
 */
data class OnlineAction(
    val kind: String,
    val index: Int = 0,
    val auto: Boolean = false,
    /** Terastallize this turn as well. Only meaningful with an attack or special. */
    val tera: Boolean = false,
    /**
     * Add a Guard to this attack or special: the move happens in full and the hit taken this turn is resisted by
     * 40%. Only meaningful with an attack or special; the engine ignores it otherwise.
     */
    val guard: Boolean = false,
) {

    fun toBattleAction(): BattleAction = when (kind) {
        KIND_SPECIAL -> BattleAction.Special
        KIND_SWITCH -> BattleAction.Switch(index)
        else -> BattleAction.Attack
    }

    companion object {
        const val KIND_ATTACK = "attack"
        const val KIND_SPECIAL = "special"
        const val KIND_SWITCH = "switch"

        fun of(action: BattleAction, tera: Boolean = false, guard: Boolean = false): OnlineAction = when (action) {
            BattleAction.Attack -> OnlineAction(KIND_ATTACK, tera = tera, guard = guard)
            BattleAction.Special -> OnlineAction(KIND_SPECIAL, tera = tera, guard = guard)
            is BattleAction.Switch -> OnlineAction(KIND_SWITCH, action.index)
        }

        fun replacement(index: Int) = OnlineAction(KIND_SWITCH, index)

        val KINDS = setOf(KIND_ATTACK, KIND_SPECIAL, KIND_SWITCH)
    }
}

/** Everything recorded so far, by step. Step 0 holds the ready marks. */
typealias ActionLog = Map<Int, Map<Seat, OnlineAction>>

/** The result of replaying a log: the battle now, the next step to play, and what each step did, in order. */
data class Replay(
    val state: BattleState,
    val nextStep: Int,
    val steps: List<StepResult>,
    /** The seat that missed [OnlineBattleRules.MAX_MISSED_TURNS] turns, if any. The replay stops there. */
    val disqualified: Seat? = null,
)

data class StepResult(val step: Int, val result: TurnResult)

/**
 * How an online battle ended. [byLeaving] is a forfeit: the loser quit, stopped answering or was disqualified.
 * [disqualified] is the forfeit for missing too many turns.
 */
data class OnlineOutcome(val winner: Seat, val byLeaving: Boolean, val disqualified: Boolean = false)

object OnlineBattleRules {

    const val TURN_SECONDS = 30
    /** How long a quick match looks for a real trainer before a stand-in rival takes the other seat. */
    const val QUEUE_SECONDS = 20
    /** A friend room waits this long for someone to join before it is abandoned. */
    const val FRIEND_ROOM_SECONDS = 600
    /** A rival counts as gone after this long without a heartbeat (the server rule uses [CLAIM_AFTER_MS]). */
    const val AWAY_AFTER_MS = 45_000L
    /** The most the server allows between heartbeats before a forfeit claim is accepted. Keep in step with firestore.rules. */
    const val CLAIM_AFTER_MS = 60_000L
    const val HEARTBEAT_MS = 15_000L

    /** How many paid battles one rival can give you in a day, so two accounts can't farm each other. */
    const val REWARDED_PER_RIVAL_PER_DAY = 3
    /** A battle that ends before this step pays nothing, so leaving early can't farm rewards. */
    const val MIN_TURNS_FOR_REWARD = 3

    /** Turns a player may let the clock play for them before they are disqualified and the rival wins. */
    const val MAX_MISSED_TURNS = 2

    /** The seats that must record something before the battle can move on. Empty once it is over. */
    fun pendingSeats(state: BattleState): Set<Seat> = when {
        state.over -> emptySet()
        state.playerMustSwitch || state.foeMustSwitch -> buildSet {
            if (state.playerMustSwitch) add(Seat.HOST)
            if (state.foeMustSwitch) add(Seat.GUEST)
        }
        else -> setOf(Seat.HOST, Seat.GUEST)
    }

    /**
     * What a player gets if their clock runs out: the sensible replacement when one is needed, otherwise
     * the heavy move when the meter is full, otherwise a plain attack.
     */
    fun defaultAction(state: BattleState, seat: Seat): OnlineAction {
        val side = state.side(seat.who)
        if (state.mustSwitch(seat.who)) {
            val against = state.side(seat.who.opposite()).current
            return OnlineAction.replacement(BotBrain.bestReplacement(side, against))
        }
        return if (side.current.energyReady) OnlineAction(OnlineAction.KIND_SPECIAL) else OnlineAction(OnlineAction.KIND_ATTACK)
    }

    /**
     * Plays one step. [actions] must hold every seat in [pendingSeats]. A choice that is not allowed
     * (a Special without a full meter, a bad switch) falls back to something legal, so a step can never
     * get stuck on a bad entry.
     */
    fun resolve(state: BattleState, step: Int, matchSeed: Long, actions: Map<Seat, OnlineAction>): TurnResult {
        val pending = pendingSeats(state)
        if (pending.isEmpty()) return TurnResult(state, emptyList())

        if (pending.size == 2) {
            val host = actions[Seat.HOST]?.toBattleAction() ?: BattleAction.Attack
            val guest = actions[Seat.GUEST]?.toBattleAction() ?: BattleAction.Attack
            return BattleEngine.resolveTurn(
                state, host, guest, BattleEngine.turnRandom(matchSeed, step), botReplaces = false,
                playerTera = actions[Seat.HOST]?.tera == true,
                foeTera = actions[Seat.GUEST]?.tera == true,
                playerGuard = actions[Seat.HOST]?.guard == true,
                foeGuard = actions[Seat.GUEST]?.guard == true,
            )
        }

        // A replacement round: pick the asked-for bench slot, or the best one if that is not possible.
        fun slotFor(seat: Seat): Int? {
            if (seat !in pending) return null
            val side = state.side(seat.who)
            val wanted = actions[seat]?.takeIf { it.kind == OnlineAction.KIND_SWITCH }?.index
            return wanted?.takeIf { it in side.team.indices && !side.team[it].fainted && it != side.active }
                ?: BotBrain.bestReplacement(side, state.side(seat.who.opposite()).current)
        }
        return BattleEngine.forceSwitchBoth(state, slotFor(Seat.HOST), slotFor(Seat.GUEST))
    }

    /**
     * Counts the turns the clock played for each seat. Call once per step, in order, with that step's
     * entries. Returns the seat that has now missed [MAX_MISSED_TURNS] turns and must be disqualified, or
     * null. If both seats reach the limit on the same step neither is disqualified: both are put back one
     * short of it, so a double time-out (say, a dropped connection on both sides) never decides a battle.
     */
    fun countMisses(misses: MutableMap<Seat, Int>, entries: Map<Seat, OnlineAction>): Seat? {
        for ((seat, action) in entries) if (action.auto) misses[seat] = (misses[seat] ?: 0) + 1
        val over = Seat.entries.filter { (misses[it] ?: 0) >= MAX_MISSED_TURNS }
        return when (over.size) {
            0 -> null
            1 -> over.single()
            else -> { over.forEach { misses[it] = MAX_MISSED_TURNS - 1 }; null }
        }
    }

    /**
     * Replays every step the [log] can complete, from [initial]. Stops at the first step still missing a
     * seat's action, or where a seat is disqualified for missing turns. Safe to call again with a longer
     * log: the earlier steps come out identical.
     */
    fun replay(initial: BattleState, matchSeed: Long, log: ActionLog): Replay {
        var state = initial
        var step = 1
        val done = mutableListOf<StepResult>()
        val misses = mutableMapOf<Seat, Int>()
        while (!state.over) {
            val entries = log[step] ?: break
            if (!pendingSeats(state).all { it in entries }) break
            countMisses(misses, entries)?.let { return Replay(state, step, done, disqualified = it) }
            val result = resolve(state, step, matchSeed, entries)
            done += StepResult(step, result)
            state = result.state
            step++
        }
        return Replay(state, step, done)
    }

    /**
     * The winner, if there is one: whoever the battle itself says won, otherwise whoever is left after the
     * other quit or was found gone.
     */
    fun outcome(state: BattleState, quit: Seat?, abandoned: Seat?, disqualified: Seat? = null): OnlineOutcome? {
        state.winner?.let { w -> return OnlineOutcome(if (w == Who.PLAYER) Seat.HOST else Seat.GUEST, byLeaving = false) }
        disqualified?.let { return OnlineOutcome(it.other, byLeaving = true, disqualified = true) }
        val gone = quit ?: abandoned ?: return null
        return OnlineOutcome(gone.other, byLeaving = true)
    }

    /**
     * Whether a finished battle goes on the player's record. A battle someone walked out of before it was
     * really fought is not a win for the other side: nothing was decided, so neither a win nor a played
     * battle is counted. Anything played to its end, or left after a few turns, still counts.
     */
    fun countsForRecord(byLeaving: Boolean, turns: Int): Boolean = !byLeaving || turns >= MIN_TURNS_FOR_REWARD

    /** Whether a finished battle pays: it must have been fought, and not be one rival farmed over and over. */
    fun paysRewards(turns: Int, paidWithThisRivalToday: Int): Boolean =
        turns >= MIN_TURNS_FOR_REWARD && paidWithThisRivalToday < REWARDED_PER_RIVAL_PER_DAY

    /**
     * Online battles are always even: both players bring the same number of Pokémon. A quick match is one of
     * the online [com.aditya1875.pokeverse.feature.game.battle.domain.TeamFormat]s, 3 vs 3 or 6 vs 6, and rooms
     * only pair teams of the same size, so the two formats are separate pools; a friend's room uses whatever
     * size its host chose, and the guest has to match it. Without this, someone with six Pokémon could be
     * matched with someone who has three.
     *
     * Each format caps Legendaries on a player's OWN team before they queue (one in 3 vs 3, two in 6 vs 6): in a
     * small team the general limit of three means "no limit at all". An opponent's team is still judged by the
     * looser [validTeam], so a player on an older version, who can still bring more, is not rejected mid-match.
     */
    fun quickFormatOf(team: List<Int>): com.aditya1875.pokeverse.feature.game.battle.domain.TeamFormat? =
        com.aditya1875.pokeverse.feature.game.battle.domain.TeamFormat.quick(team.size)

    /** Whether [team] is a legal quick-match team: exactly a format's size, with no more legends than it allows. */
    fun quickTeamOk(team: List<Int>): Boolean {
        val format = quickFormatOf(team) ?: return false
        return com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.legendaryCount(team) <= format.maxLegendaries
    }

    fun evenTeams(host: List<Int>?, guest: List<Int>?): Boolean =
        host != null && guest != null && host.size == guest.size

    /** Team ids are checked before they are trusted: a real, bounded roster in the dex. */
    fun validTeam(ids: List<Int>?, maxId: Int): Boolean =
        ids != null && ids.size in 1..6 && ids.all { it in 1..maxId } && ids.toSet().size == ids.size &&
            com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.legendaryCount(ids) <=
            com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.MAX_LEGENDARIES

    private fun Who.opposite() = if (this == Who.PLAYER) Who.FOE else Who.PLAYER
}
