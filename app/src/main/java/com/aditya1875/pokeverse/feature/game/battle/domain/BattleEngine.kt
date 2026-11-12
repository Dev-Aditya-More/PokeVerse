package com.aditya1875.pokeverse.feature.game.battle.domain

import com.aditya1875.pokeverse.utils.pokemonTypeEffectiveness
import kotlin.math.floor
import kotlin.random.Random

/*
 * Team Battle: your own caught Pokémon against a bot, one-on-one up to three-on-three.
 *
 * The rules are deliberately small and readable, because the player has to understand them on first
 * contact. Everything here is pure Kotlin driven by a [Random], so a battle is reproducible from a
 * seed and every rule is unit-tested.
 *
 *  - Stats come from the real base stats (level 50), so the Pokémon you caught actually matters.
 *  - Each turn both sides pick: ATTACK (a quick hit that charges energy), SPECIAL (a heavy hit of the
 *    Pokémon's second type, needs full energy), GUARD (halves the next hit and charges energy fast), or
 *    SWITCH to a bench Pokémon.
 *  - Types work like the games: super effective, resisted, immune, plus a bonus for your own type.
 *  - Speed decides who strikes first, so a fast attacker can knock out a slower one before it acts.
 */

enum class Who { PLAYER, FOE }

data class BattleStats(val hp: Int, val atk: Int, val def: Int, val spa: Int, val spd: Int, val spe: Int)

object BattleMath {
    const val LEVEL = 50
    const val FULL_ENERGY = 100

    /** A non-HP stat at level 50 with a perfect-roll base: the games' formula, minus EVs and natures. */
    fun stat(base: Int): Int = ((2 * base + 31) * LEVEL) / 100 + 5

    fun hp(base: Int): Int = ((2 * base + 31) * LEVEL) / 100 + LEVEL + 10

    fun statsFromBase(hp: Int, atk: Int, def: Int, spa: Int, spd: Int, spe: Int) =
        BattleStats(hp(hp), stat(atk), stat(def), stat(spa), stat(spd), stat(spe))

    /** Multiplier of an [attackType] hit on a defender with [defenderTypes]; 0 means immune. */
    fun effectiveness(attackType: String, defenderTypes: List<String>): Float {
        val row = pokemonTypeEffectiveness[attackType.lowercase()] ?: return 1f
        return defenderTypes.fold(1f) { acc, t -> acc * (row[t.lowercase()] ?: 1f) }
    }
}

/**
 * Terastallize: once per online battle, one Pokémon turns into a single pure type (its Tera type) and
 * both its attack types follow. Every species has ONE fixed Tera type, like a set on Pokémon Showdown,
 * so a rival can read it off the team in advance and the choice is only ever *when*.
 */
object TeraTypes {
    val ALL = listOf(
        "normal", "fire", "water", "electric", "grass", "ice", "fighting", "poison", "ground",
        "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy",
    )

    /** The species' Tera type: always a type it does not already have, and the same on every phone. */
    fun forSpecies(id: Int, types: List<String>): String {
        val own = types.map { it.lowercase() }.toSet()
        val start = Math.floorMod(id * 7 + 3, ALL.size)
        return (0 until ALL.size).map { ALL[(start + it) % ALL.size] }.firstOrNull { it !in own } ?: ALL[start]
    }
}

data class Fighter(
    val id: Int,
    val name: String,
    val types: List<String>,
    val stats: BattleStats,
    val spriteUrl: String,
    val hp: Int = stats.hp,
    val energy: Int = 0,
    /** The type it becomes when it Terastallizes. Fixed per species. */
    val teraType: String = TeraTypes.forSpecies(id, types),
    val terastallized: Boolean = false,
) {
    val fainted: Boolean get() = hp <= 0
    val energyReady: Boolean get() = energy >= BattleMath.FULL_ENERGY
    val hpFraction: Float get() = (hp.toFloat() / stats.hp).coerceIn(0f, 1f)
    val primaryType: String get() = types.firstOrNull() ?: "normal"

    /** The type of the heavy move: the second type when there is one, so dual types feel different. */
    val specialType: String get() = types.lastOrNull() ?: "normal"
}

/**
 * One side of a battle. [guardsLeft] belongs to the side, not to any Pokémon on it: a team gets
 * [BattleEngine.MAX_GUARDS] Guards for the whole battle however many Pokémon it sends out.
 */
data class Side(
    val team: List<Fighter>,
    val active: Int = 0,
    val guardsLeft: Int = BattleEngine.MAX_GUARDS,
    /** One Terastallize per side per battle; true once it has been spent. */
    val teraUsed: Boolean = false,
) {
    val current: Fighter get() = team[active]
    val hasFighters: Boolean get() = team.any { !it.fainted }
}

sealed interface BattleAction {
    data object Attack : BattleAction
    data object Special : BattleAction
    data class Switch(val index: Int) : BattleAction
}

sealed interface BattleEvent {
    data class Hit(
        val by: Who,
        val special: Boolean,
        val damage: Int,
        val effectiveness: Float,
        val critical: Boolean,
        val guarded: Boolean,
        val attackerName: String,
        val defenderName: String,
        /** The defender's HP and both sides' energy right after this blow, so the screen can show them then. */
        val defenderHpAfter: Int,
        val attackerEnergyAfter: Int,
        val defenderEnergyAfter: Int,
        val attackerType: String,
    ) : BattleEvent

    data class Guarded(val by: Who, val name: String, val energyAfter: Int) : BattleEvent

    /** [who]'s Pokémon turned into [teraType]. [fighter] is the Pokémon as it is afterwards. */
    data class Terastallized(val who: Who, val name: String, val teraType: String, val fighter: Fighter) : BattleEvent

    /** [fighter] is the Pokémon as it is the moment it comes in. */
    data class Switched(val who: Who, val name: String, val fighter: Fighter) : BattleEvent
    data class Fainted(val who: Who, val name: String) : BattleEvent
    data class EnergyReady(val who: Who) : BattleEvent
    data class Won(val who: Who) : BattleEvent
}

data class BattleState(
    val player: Side,
    val foe: Side,
    val turn: Int = 1,
    val winner: Who? = null,
    /** The player's Pokémon fainted and the player must choose who comes in; no turn is spent. */
    val playerMustSwitch: Boolean = false,
    /** Same for the foe. Only ever set in an online battle; the bot picks its own replacement. */
    val foeMustSwitch: Boolean = false,
) {
    val over: Boolean get() = winner != null
    fun side(who: Who): Side = if (who == Who.PLAYER) player else foe
    fun mustSwitch(who: Who): Boolean = if (who == Who.PLAYER) playerMustSwitch else foeMustSwitch

    /**
     * The same battle seen from the other seat: player and foe swap. Online, both phones compute the turn
     * from the host's seat (so the dice fall identically) and the guest shows the mirrored result.
     */
    fun mirrored(): BattleState = BattleState(
        player = foe, foe = player, turn = turn, winner = winner?.other(),
        playerMustSwitch = foeMustSwitch, foeMustSwitch = playerMustSwitch,
    )
}

fun Who.other(): Who = if (this == Who.PLAYER) Who.FOE else Who.PLAYER

/** The same events as the other seat saw them. */
fun BattleEvent.mirrored(): BattleEvent = when (this) {
    is BattleEvent.Hit -> copy(by = by.other())
    is BattleEvent.Guarded -> copy(by = by.other())
    is BattleEvent.Terastallized -> copy(who = who.other())
    is BattleEvent.Switched -> copy(who = who.other())
    is BattleEvent.Fainted -> copy(who = who.other())
    is BattleEvent.EnergyReady -> copy(who = who.other())
    is BattleEvent.Won -> copy(who = who.other())
}

fun TurnResult.mirrored(): TurnResult = TurnResult(state.mirrored(), events.map { it.mirrored() })

data class TurnResult(val state: BattleState, val events: List<BattleEvent>)

object BattleEngine {

    const val ATTACK_POWER = 45
    const val SPECIAL_POWER = 100
    const val ENERGY_ON_ATTACK = 35
    const val ENERGY_WHEN_HIT = 10
    const val CRIT_CHANCE = 0.0625f

    /** A Guard resists this share of the hit taken: it lands at 60%, so 40% less. */
    const val GUARD_FACTOR = 0.6f

    /** Guards a side may use in a whole battle, shared by its whole team. */
    const val MAX_GUARDS = 2

    /**
     * Whether [who] may add a Guard to [action] this turn: a Guard goes WITH an Attack or a Special (never a
     * Switch), and the side must have one left. The move still happens in full; the Guard only softens the
     * hit that lands on them this turn, so using it is a trade of a limited charge for 40% of the damage.
     */
    fun canGuard(state: BattleState, who: Who, action: BattleAction): Boolean =
        !state.over && state.side(who).guardsLeft > 0 &&
            (action == BattleAction.Attack || action == BattleAction.Special)

    fun start(player: List<Fighter>, foe: List<Fighter>): BattleState {
        require(player.isNotEmpty() && foe.isNotEmpty()) { "both sides need at least one Pokémon" }
        return BattleState(Side(player), Side(foe))
    }

    /**
     * The dice for one turn of an online battle. Seeded from the match seed and the turn number, so both
     * phones roll the same numbers for the same turn whatever happened on either before it.
     */
    fun turnRandom(matchSeed: Long, turn: Int): Random =
        Random(matchSeed * 6364136223846793005L + turn.toLong() * 1442695040888963407L)

    /** Whether [action] can be taken right now; the UI uses this to enable buttons. */
    fun isLegal(state: BattleState, who: Who, action: BattleAction): Boolean {
        val side = state.side(who)
        return when (action) {
            BattleAction.Attack -> true
            BattleAction.Special -> side.current.energyReady
            is BattleAction.Switch -> action.index in side.team.indices &&
                action.index != side.active && !side.team[action.index].fainted
        }
    }

    /** Whether [who] may Terastallize this turn: not yet spent, a healthy Pokémon on the field, not switching. */
    fun canTera(state: BattleState, who: Who, action: BattleAction = BattleAction.Attack): Boolean {
        val side = state.side(who)
        return !state.over && !side.teraUsed && !side.current.fainted && !side.current.terastallized &&
            action !is BattleAction.Switch
    }

    /** The player chooses a replacement after a knockout. Costs no turn. */
    fun forceSwitch(state: BattleState, index: Int): TurnResult = forceSwitch(state, Who.PLAYER, index)

    /** [who] chooses a replacement after a knockout. Costs no turn. */
    fun forceSwitch(state: BattleState, who: Who, index: Int): TurnResult {
        if (!state.mustSwitch(who) || state.over) return TurnResult(state, emptyList())
        val side = state.side(who)
        if (index !in side.team.indices || side.team[index].fainted) return TurnResult(state, emptyList())
        val moved = side.copy(active = index)
        val next = if (who == Who.PLAYER) state.copy(player = moved, playerMustSwitch = false)
        else state.copy(foe = moved, foeMustSwitch = false)
        return TurnResult(next, listOf(BattleEvent.Switched(who, side.team[index].name, side.team[index])))
    }

    /**
     * Both players of an online battle replace a knocked-out Pokémon at the same moment. Each choice is
     * checked on its own; a side that doesn't need one ignores its entry. Player first, then foe.
     */
    fun forceSwitchBoth(state: BattleState, playerIndex: Int?, foeIndex: Int?): TurnResult {
        var st = state
        val events = mutableListOf<BattleEvent>()
        if (playerIndex != null) forceSwitch(st, Who.PLAYER, playerIndex).let { st = it.state; events += it.events }
        if (foeIndex != null) forceSwitch(st, Who.FOE, foeIndex).let { st = it.state; events += it.events }
        return TurnResult(st, events)
    }

    fun resolveTurn(
        state: BattleState,
        playerAction: BattleAction,
        foeAction: BattleAction,
        random: Random,
        foeSkill: Float = 1f,
        /** True against the bot, which picks its own replacement. False online: the foe player is asked. */
        botReplaces: Boolean = true,
        /** Terastallize this turn (online only). Ignored when not allowed, so a bad flag can never stall a step. */
        playerTera: Boolean = false,
        foeTera: Boolean = false,
        /**
         * Add a Guard to this turn's Attack or Special. The move happens in full AND the hit that lands on them
         * this turn is resisted by 40%. It costs one of their limited Guards, so the other side can answer with a
         * Guard of their own or take the damage and save theirs. Ignored when it is not allowed (a Switch, or no
         * Guards left), so a bad flag can never stall a turn.
         */
        playerGuard: Boolean = false,
        foeGuard: Boolean = false,
    ): TurnResult {
        if (state.over || state.playerMustSwitch || state.foeMustSwitch) return TurnResult(state, emptyList())

        val events = mutableListOf<BattleEvent>()
        var st = state

        // An illegal choice (Special without a full meter, a bad switch) falls back to a plain attack.
        val pAct = if (isLegal(st, Who.PLAYER, playerAction)) playerAction else BattleAction.Attack
        val fAct = if (isLegal(st, Who.FOE, foeAction)) foeAction else BattleAction.Attack

        // 1. Switches happen first, player then foe; the incoming Pokémon takes the hit.
        st = applySwitch(st, Who.PLAYER, pAct, events)
        st = applySwitch(st, Who.FOE, fAct, events)

        // 1b. Terastallize, before anyone strikes: the new type is what this turn's blows are made of.
        if (playerTera && canTera(st, Who.PLAYER, pAct)) st = terastallize(st, Who.PLAYER, events)
        if (foeTera && canTera(st, Who.FOE, fAct)) st = terastallize(st, Who.FOE, events)

        // 1c. Guards: spent now, and they soften whatever hits that side this turn. The move itself is unchanged.
        val guarding = buildSet {
            if (playerGuard && canGuard(st, Who.PLAYER, pAct)) add(Who.PLAYER)
            if (foeGuard && canGuard(st, Who.FOE, fAct)) add(Who.FOE)
        }
        guarding.forEach { who ->
            st = useGuard(st, who)
            events += BattleEvent.Guarded(who, st.side(who).current.name, st.side(who).current.energy)
        }

        // 2. Strikes, fastest first. A Pokémon that faints before its turn never acts.
        val strikers = buildList {
            if (pAct == BattleAction.Attack || pAct == BattleAction.Special) add(Who.PLAYER to pAct)
            if (fAct == BattleAction.Attack || fAct == BattleAction.Special) add(Who.FOE to fAct)
        }
        val order = strikers.sortedWith(
            compareByDescending<Pair<Who, BattleAction>> { st.side(it.first).current.stats.spe }
                .thenBy { random.nextInt() } // a speed tie is a coin flip
        )
        // Who is on the field when the turn starts: a replacement that comes in mid-turn doesn't strike.
        val startedWith = mapOf(Who.PLAYER to st.player.active, Who.FOE to st.foe.active)

        for ((who, action) in order) {
            if (st.over) break
            val attackerSide = st.side(who)
            if (attackerSide.active != startedWith[who] || attackerSide.current.fainted) continue
            st = strike(st, who, action == BattleAction.Special, who.other() in guarding, random, events)
        }

        // 3. Knockouts: the bot replaces itself at once; the player is asked.
        st = settleKnockouts(st, events, random, foeSkill, botReplaces)

        return TurnResult(if (st.over) st else st.copy(turn = st.turn + 1), events)
    }

    // ── Internals ───────────────────────────────────────────────────────────────

    private fun replace(st: BattleState, who: Who, side: Side): BattleState =
        if (who == Who.PLAYER) st.copy(player = side) else st.copy(foe = side)

    private fun applySwitch(st: BattleState, who: Who, action: BattleAction, events: MutableList<BattleEvent>): BattleState {
        if (action !is BattleAction.Switch) return st
        val side = st.side(who)
        events += BattleEvent.Switched(who, side.team[action.index].name, side.team[action.index])
        return replace(st, who, side.copy(active = action.index))
    }

    private fun terastallize(st: BattleState, who: Who, events: MutableList<BattleEvent>): BattleState {
        val side = st.side(who)
        val after = side.current.copy(types = listOf(side.current.teraType), terastallized = true)
        events += BattleEvent.Terastallized(who, after.name, after.teraType, after)
        return replace(st, who, side.copy(
            team = side.team.toMutableList().also { it[side.active] = after },
            teraUsed = true,
        ))
    }

    /** Spends one of the side's Guards. */
    private fun useGuard(st: BattleState, who: Who): BattleState {
        val side = st.side(who)
        return replace(st, who, side.copy(guardsLeft = (side.guardsLeft - 1).coerceAtLeast(0)))
    }

    private fun giveEnergy(st: BattleState, who: Who, amount: Int, events: MutableList<BattleEvent>): BattleState {
        val side = st.side(who)
        val cur = side.current
        if (cur.fainted) return st
        val wasReady = cur.energyReady
        val updated = cur.copy(energy = (cur.energy + amount).coerceAtMost(BattleMath.FULL_ENERGY))
        if (!wasReady && updated.energyReady) events += BattleEvent.EnergyReady(who)
        return replace(st, who, side.copy(team = side.team.toMutableList().also { it[side.active] = updated }))
    }

    private fun strike(
        st: BattleState,
        who: Who,
        special: Boolean,
        defenderGuarding: Boolean,
        random: Random,
        events: MutableList<BattleEvent>,
    ): BattleState {
        val attacker = st.side(who).current
        val defenderWho = who.other()
        val defenderSide = st.side(defenderWho)
        val defender = defenderSide.current
        if (defender.fainted) return st

        val roll = rollDamage(attacker, defender, special, defenderGuarding, random)

        // Energy: the attacker charges on a quick hit and spends the meter on a heavy one; being hit charges a little.
        var next = st
        val attackerSide = next.side(who)
        val spent = if (special) 0 else (attacker.energy + ENERGY_ON_ATTACK).coerceAtMost(BattleMath.FULL_ENERGY)
        val attackerAfter = attacker.copy(energy = spent)
        next = replace(next, who, attackerSide.copy(team = attackerSide.team.toMutableList().also { it[attackerSide.active] = attackerAfter }))

        val hitAfter = defender.copy(
            hp = (defender.hp - roll.damage).coerceAtLeast(0),
            energy = if (defender.hp - roll.damage <= 0) defender.energy
            else (defender.energy + ENERGY_WHEN_HIT).coerceAtMost(BattleMath.FULL_ENERGY),
        )
        events += BattleEvent.Hit(
            who, special, roll.damage, roll.effectiveness, roll.critical, defenderGuarding,
            attackerName = attacker.name, defenderName = defender.name,
            defenderHpAfter = hitAfter.hp,
            attackerEnergyAfter = attackerAfter.energy,
            defenderEnergyAfter = hitAfter.energy,
            attackerType = if (special) attacker.specialType else attacker.primaryType,
        )
        if (!attacker.energyReady && !special && attackerAfter.energyReady) events += BattleEvent.EnergyReady(who)
        if (!defender.energyReady && hitAfter.energyReady && !hitAfter.fainted) events += BattleEvent.EnergyReady(defenderWho)
        val dSide = next.side(defenderWho)
        next = replace(next, defenderWho, dSide.copy(team = dSide.team.toMutableList().also { it[dSide.active] = hitAfter }))

        if (hitAfter.fainted) events += BattleEvent.Fainted(defenderWho, hitAfter.name)
        return next
    }

    data class DamageRoll(val damage: Int, val effectiveness: Float, val critical: Boolean)

    fun rollDamage(
        attacker: Fighter,
        defender: Fighter,
        special: Boolean,
        defenderGuarding: Boolean,
        random: Random,
    ): DamageRoll {
        val type = if (special) attacker.specialType else attacker.primaryType
        val power = if (special) SPECIAL_POWER else ATTACK_POWER
        val effectiveness = BattleMath.effectiveness(type, defender.types)
        val critical = random.nextFloat() < CRIT_CHANCE
        val spread = 0.85f + 0.15f * random.nextFloat()
        if (effectiveness == 0f) return DamageRoll(0, 0f, false)

        // The attacker's stronger side decides physical or special, like picking its best move.
        val physical = attacker.stats.atk >= attacker.stats.spa
        val a = if (physical) attacker.stats.atk else attacker.stats.spa
        val d = if (physical) defender.stats.def else defender.stats.spd

        val base = ((2f * BattleMath.LEVEL / 5f + 2f) * power * a / d) / 50f + 2f
        val stab = if (type in attacker.types) 1.5f else 1f
        val raw = base * stab * effectiveness * (if (critical) 1.5f else 1f) * spread *
            (if (defenderGuarding) GUARD_FACTOR else 1f)
        return DamageRoll(floor(raw).toInt().coerceAtLeast(1), effectiveness, critical)
    }

    private fun settleKnockouts(
        state: BattleState,
        events: MutableList<BattleEvent>,
        random: Random,
        foeSkill: Float,
        botReplaces: Boolean,
    ): BattleState {
        var st = state
        if (!st.foe.hasFighters) {
            events += BattleEvent.Won(Who.PLAYER)
            return st.copy(winner = Who.PLAYER)
        }
        if (!st.player.hasFighters) {
            events += BattleEvent.Won(Who.FOE)
            return st.copy(winner = Who.FOE)
        }
        if (st.foe.current.fainted && !botReplaces) {
            st = st.copy(foeMustSwitch = true)
        } else if (st.foe.current.fainted) {
            // A skilled bot sends in its best matchup; a weaker one just sends in whoever is still standing.
            val standing = st.foe.team.withIndex().filter { !it.value.fainted }
            val next = if (random.nextFloat() < foeSkill) BotBrain.bestReplacement(st.foe, st.player.current)
            else standing.random(random).index
            events += BattleEvent.Switched(Who.FOE, st.foe.team[next].name, st.foe.team[next])
            st = st.copy(foe = st.foe.copy(active = next))
        }
        if (st.player.current.fainted) st = st.copy(playerMustSwitch = true)
        return st
    }
}

/**
 * The bot. [skill] (0..1) is how often it plays the sensible move instead of a random legal one:
 * low skill makes a forgiving Rookie, high skill a Champion that punishes bad matchups.
 */
class BotBrain(private val skill: Float) {

    /**
     * Whether the bot adds a Guard to [action]. A rookie does it on a whim; a skilled bot saves its two Guards for
     * hits that matter: when the player's Special is ready, or the one that would hurt is aimed at a weak Pokémon.
     */
    fun wantsGuard(state: BattleState, action: BattleAction, random: Random): Boolean {
        if (!BattleEngine.canGuard(state, Who.FOE, action)) return false
        if (random.nextFloat() > skill) return random.nextFloat() < 0.25f
        val me = state.foe.current
        val you = state.player.current
        // A fixed roll, so the estimate is the same every time it is asked.
        val worst = BattleEngine.rollDamage(you, me, you.energyReady, false, Random(0)).damage
        return when {
            worst >= me.hp -> true                                   // it would be knocked out: guard it
            you.energyReady && worst >= me.hp * 0.4f -> random.nextFloat() < 0.8f
            me.hpFraction < 0.35f -> random.nextFloat() < 0.6f
            else -> false
        }
    }

    fun choose(state: BattleState, random: Random): BattleAction {
        val legal = legalActions(state)
        if (random.nextFloat() > skill) return legal.random(random)

        val me = state.foe.current
        val you = state.player.current

        // Walk away from a matchup that is clearly losing, if the bench has something better.
        val bench = benchCandidates(state.foe)
        val myScore = matchup(me, you)
        val best = bench.maxByOrNull { (_, f) -> matchup(f, you) }
        if (best != null && myScore < -0.5f && matchup(best.second, you) > myScore + 1f && random.nextFloat() < 0.7f) {
            return BattleAction.Switch(best.first)
        }

        // A full meter is a hit waiting to land, unless it would do nothing.
        if (me.energyReady && BattleMath.effectiveness(me.specialType, you.types) > 0f) return BattleAction.Special

        return BattleAction.Attack
    }

    private fun legalActions(state: BattleState): List<BattleAction> = buildList {
        add(BattleAction.Attack)
        if (state.foe.current.energyReady) add(BattleAction.Special)
        benchCandidates(state.foe).forEach { (i, _) -> add(BattleAction.Switch(i)) }
    }

    private fun benchCandidates(side: Side): List<Pair<Int, Fighter>> =
        side.team.withIndex().filter { (i, f) -> i != side.active && !f.fainted }.map { it.index to it.value }

    companion object {
        /** Positive when [a] does better against [b] than [b] does against [a]. Roughly -4..+4. */
        fun matchup(a: Fighter, b: Fighter): Float {
            val offence = maxOf(
                BattleMath.effectiveness(a.primaryType, b.types),
                BattleMath.effectiveness(a.specialType, b.types)
            )
            val defence = maxOf(
                BattleMath.effectiveness(b.primaryType, a.types),
                BattleMath.effectiveness(b.specialType, a.types)
            )
            return offence - defence
        }

        /** The healthy teammate that matches up best against [opponent]; the first healthy one if all tie. */
        fun bestReplacement(side: Side, opponent: Fighter): Int =
            side.team.withIndex()
                .filter { !it.value.fainted }
                .maxByOrNull { matchup(it.value, opponent) }
                ?.index ?: side.active
    }
}

/**
 * How hard each bot tier is: its skill, and how its team's strength compares with the player's.
 * Matching strength to the player's own team is what keeps a battle fair whatever they caught.
 */
enum class BotTier(val skill: Float, val strengthFactor: Float, val xpBonus: Int) {
    ROOKIE(0.15f, 0.8f, 0),
    TRAINER(0.4f, 0.95f, 10),
    CHAMPION(0.7f, 1.05f, 25);

    /** The base-stat total a foe should have for a player whose team averages [playerAverageBst]. */
    fun targetBst(playerAverageBst: Int): Int = (playerAverageBst * strengthFactor).toInt()
}

/** Matchmaking rules that keep a battle winnable whatever the player caught. */
object BattleMatchmaker {
    /**
     * True when at least one of [team]'s attack types can damage [foe]. A lone Electric Pokémon against a
     * Ground foe could never win, so such a foe is never picked.
     */
    fun canDamage(team: List<Fighter>, foe: Fighter): Boolean = team.any { mine ->
        maxOf(
            BattleMath.effectiveness(mine.primaryType, foe.types),
            BattleMath.effectiveness(mine.specialType, foe.types)
        ) > 0f
    }
}
