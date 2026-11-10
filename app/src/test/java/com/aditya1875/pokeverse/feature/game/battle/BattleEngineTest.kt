package com.aditya1875.pokeverse.feature.game.battle

import com.aditya1875.pokeverse.feature.game.battle.domain.BattleAction
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEngine
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEvent
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleMatchmaker
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleMath
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleState
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleStats
import com.aditya1875.pokeverse.feature.game.battle.domain.BotBrain
import com.aditya1875.pokeverse.feature.game.battle.domain.BotTier
import com.aditya1875.pokeverse.feature.game.battle.domain.Fighter
import com.aditya1875.pokeverse.feature.game.battle.domain.Who
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BattleEngineTest {

    private fun stats(hp: Int = 100, atk: Int = 100, def: Int = 100, spa: Int = 100, spd: Int = 100, spe: Int = 100) =
        BattleStats(
            BattleMath.hp(hp), BattleMath.stat(atk), BattleMath.stat(def),
            BattleMath.stat(spa), BattleMath.stat(spd), BattleMath.stat(spe)
        )

    private fun mon(
        name: String,
        vararg types: String,
        s: BattleStats = stats(),
        energy: Int = 0,
        hp: Int? = null,
    ) = Fighter(id = name.hashCode(), name = name, types = types.toList(), stats = s, spriteUrl = "", hp = hp ?: s.hp, energy = energy)

    /** Same seed → same crit and spread, so two rolls differ only by what the test changes. */
    private fun dmg(a: Fighter, d: Fighter, special: Boolean = false, guarding: Boolean = false) =
        BattleEngine.rollDamage(a, d, special, guarding, Random(7)).damage

    // ── stats ─────────────────────────────────────────────────────────────

    @Test
    fun `level 50 stats follow the games' formula`() {
        assertEquals(((2 * 100 + 31) * 50) / 100 + 5, BattleMath.stat(100))
        assertEquals(((2 * 100 + 31) * 50) / 100 + 60, BattleMath.hp(100))
        assertTrue(BattleMath.stat(130) > BattleMath.stat(60))
    }

    // ── damage ────────────────────────────────────────────────────────────

    @Test
    fun `super effective beats neutral beats resisted, and immune does nothing`() {
        val fire = mon("Ember", "fire")
        val grass = mon("Leaf", "grass")
        val water = mon("Pool", "water")
        val plain = mon("Plain", "normal")
        val ghost = mon("Boo", "ghost")

        val superEffective = dmg(fire, grass)
        val neutral = dmg(fire, plain)
        val resisted = dmg(fire, water)
        assertTrue("$superEffective > $neutral", superEffective > neutral)
        assertTrue("$neutral > $resisted", neutral > resisted)
        assertEquals(0, dmg(plain, ghost))
    }

    @Test
    fun `the special hits much harder than the quick attack`() {
        val a = mon("Mixed", "fire", "dragon")
        val d = mon("Wall", "normal")
        assertTrue(dmg(a, d, special = true) > dmg(a, d, special = false) * 2)
    }

    @Test
    fun `guarding halves the hit`() {
        val a = mon("A", "fire")
        val d = mon("D", "normal")
        val full = dmg(a, d)
        val guarded = dmg(a, d, guarding = true)
        assertTrue(guarded in (full / 2 - 1)..(full / 2 + 1))
    }

    @Test
    fun `a hit always does at least one point unless the target is immune`() {
        val weak = mon("Weak", "normal", s = stats(atk = 5, spa = 5))
        val tank = mon("Tank", "normal", s = stats(def = 250, spd = 250))
        assertTrue(dmg(weak, tank) >= 1)
    }

    // ── turns ─────────────────────────────────────────────────────────────

    private fun duel(p: Fighter, f: Fighter) = BattleEngine.start(listOf(p), listOf(f))

    @Test
    fun `the faster Pokémon strikes first and can knock out the slower one before it acts`() {
        val fast = mon("Fast", "fire", s = stats(atk = 200, spe = 150))
        val slow = mon("Slow", "grass", s = stats(hp = 10, def = 20, spe = 30))
        val result = BattleEngine.resolveTurn(duel(fast, slow), BattleAction.Attack, BattleAction.Attack, Random(1))
        val hits = result.events.filterIsInstance<BattleEvent.Hit>()
        assertEquals("the knocked-out Pokémon never got to strike", 1, hits.size)
        assertEquals(Who.PLAYER, hits.single().by)
        assertEquals(Who.PLAYER, result.state.winner)
    }

    @Test
    fun `attacking charges energy and the special needs a full meter`() {
        val a = mon("A", "fire")
        val b = mon("B", "water", s = stats(hp = 255, def = 200, spd = 200))
        var st = duel(a, b)
        assertFalse(BattleEngine.isLegal(st, Who.PLAYER, BattleAction.Special))

        // The foe guards twice, then hits back, so its three Guards are not used up before the Special turn below.
        repeat(3) { st = BattleEngine.resolveTurn(st, BattleAction.Attack, if (it < 2) BattleAction.Guard else BattleAction.Attack, Random(it)).state }
        assertEquals(105.coerceAtMost(100), st.player.current.energy)
        assertTrue(BattleEngine.isLegal(st, Who.PLAYER, BattleAction.Special))

        st = BattleEngine.resolveTurn(st, BattleAction.Special, BattleAction.Guard, Random(9)).state
        assertEquals("the special spends the whole meter", 0, st.player.current.energy)
    }

    @Test
    fun `an illegal special becomes a plain attack instead of doing nothing`() {
        val st = duel(mon("A", "fire"), mon("B", "grass"))
        val result = BattleEngine.resolveTurn(st, BattleAction.Special, BattleAction.Guard, Random(3))
        val hit = result.events.filterIsInstance<BattleEvent.Hit>().single { it.by == Who.PLAYER }
        assertFalse(hit.special)
    }

    @Test
    fun `guard charges energy quickly`() {
        val st = duel(mon("A", "fire"), mon("B", "water"))
        val after = BattleEngine.resolveTurn(st, BattleAction.Guard, BattleAction.Guard, Random(2)).state
        assertEquals(BattleEngine.ENERGY_ON_GUARD, after.player.current.energy)
    }

    @Test
    fun `switching puts the new Pokémon in front of the hit`() {
        val lead = mon("Lead", "fire")
        val bench = mon("Bench", "water")
        val foe = mon("Foe", "normal")
        val st = BattleEngine.start(listOf(lead, bench), listOf(foe))
        val result = BattleEngine.resolveTurn(st, BattleAction.Switch(1), BattleAction.Attack, Random(4))
        assertEquals(1, result.state.player.active)
        assertTrue(result.state.player.team[1].hp < bench.hp)
        assertEquals("the lead was never hit", lead.hp, result.state.player.team[0].hp)
    }

    @Test
    fun `a switch to a fainted or missing Pokémon is not allowed`() {
        val st = BattleEngine.start(listOf(mon("A", "fire"), mon("B", "water", hp = 0)), listOf(mon("F", "grass")))
        assertFalse(BattleEngine.isLegal(st, Who.PLAYER, BattleAction.Switch(1)))
        assertFalse(BattleEngine.isLegal(st, Who.PLAYER, BattleAction.Switch(0))) // already active
        assertFalse(BattleEngine.isLegal(st, Who.PLAYER, BattleAction.Switch(7)))
    }

    @Test
    fun `when the player's Pokémon faints they choose the next one for free`() {
        val glass = mon("Glass", "normal", s = stats(hp = 5, def = 5, spd = 5, spe = 10))
        val bench = mon("Bench", "water")
        val foe = mon("Foe", "fire", s = stats(atk = 200, spe = 200))
        var st = BattleEngine.start(listOf(glass, bench), listOf(foe))
        st = BattleEngine.resolveTurn(st, BattleAction.Guard, BattleAction.Attack, Random(5)).state
        // The guard may or may not have saved it; force a clear knockout instead.
        st = BattleEngine.resolveTurn(st.copy(player = st.player.copy(team = listOf(glass.copy(hp = 1), bench))), BattleAction.Attack, BattleAction.Attack, Random(6)).state
        assertTrue(st.playerMustSwitch)
        // No turns happen while a choice is pending.
        val frozen = BattleEngine.resolveTurn(st, BattleAction.Attack, BattleAction.Attack, Random(1))
        assertEquals(st, frozen.state)
        val after = BattleEngine.forceSwitch(st, 1)
        assertFalse(after.state.playerMustSwitch)
        assertEquals(1, after.state.player.active)
    }

    @Test
    fun `the foe sends in its best matchup after a knockout`() {
        val strong = mon("Strong", "electric", s = stats(atk = 250, spe = 200))
        val feeble = mon("Feeble", "water", s = stats(hp = 5, def = 5, spd = 5))
        val ground = mon("Ground", "ground")
        val flyer = mon("Flyer", "flying")
        val st = BattleEngine.start(listOf(strong), listOf(feeble, ground, flyer))
        val after = BattleEngine.resolveTurn(st, BattleAction.Attack, BattleAction.Guard, Random(2)).state
        // Electric can't touch Ground, so that is the Pokémon the foe should bring in.
        assertEquals("Ground", after.foe.current.name)
    }

    @Test
    fun `the battle ends when one side has nobody left`() {
        val killer = mon("Killer", "fire", s = stats(atk = 250, spe = 250))
        val victim = mon("Victim", "grass", s = stats(hp = 5, def = 5))
        val result = BattleEngine.resolveTurn(duel(killer, victim), BattleAction.Attack, BattleAction.Attack, Random(1))
        assertTrue(result.state.over)
        assertEquals(Who.PLAYER, result.state.winner)
        assertTrue(result.events.any { it is BattleEvent.Won })
        // Nothing happens once it's over.
        assertEquals(result.state, BattleEngine.resolveTurn(result.state, BattleAction.Attack, BattleAction.Attack, Random(1)).state)
    }

    @Test
    fun `the same seed always gives the same battle`() {
        fun play(seed: Int): BattleState {
            val rng = Random(seed)
            val bot = BotBrain(0.7f)
            var st = BattleEngine.start(
                listOf(mon("A", "fire"), mon("B", "water")),
                listOf(mon("C", "grass"), mon("D", "electric"))
            )
            var turns = 0
            while (!st.over && turns++ < 200) {
                if (st.playerMustSwitch) st = BattleEngine.forceSwitch(st, st.player.team.indexOfFirst { !it.fainted }).state
                else st = BattleEngine.resolveTurn(st, BattleAction.Attack, bot.choose(st, rng), rng).state
            }
            return st
        }
        assertEquals(play(11), play(11))
    }

    @Test
    fun `every battle ends`() {
        repeat(40) { seed ->
            val rng = Random(seed)
            val bot = BotBrain(0.5f)
            var st = BattleEngine.start(listOf(mon("A", "fire"), mon("B", "water"), mon("C", "grass")), listOf(mon("D", "ice"), mon("E", "rock"), mon("F", "bug")))
            var turns = 0
            while (!st.over && turns++ < 500) {
                st = if (st.playerMustSwitch) BattleEngine.forceSwitch(st, st.player.team.indexOfFirst { !it.fainted }).state
                else BattleEngine.resolveTurn(st, BattleAction.Attack, bot.choose(st, rng), rng).state
            }
            assertTrue("seed $seed did not finish", st.over)
        }
    }

    // ── the bot ───────────────────────────────────────────────────────────

    @Test
    fun `a skilled bot uses a full meter and never asks for a special it can't afford`() {
        val bot = BotBrain(1f)
        val ready = BattleEngine.start(listOf(mon("P", "normal")), listOf(mon("B", "fire", energy = 100)))
        assertEquals(BattleAction.Special, bot.choose(ready, Random(1)))

        val empty = BattleEngine.start(listOf(mon("P", "normal")), listOf(mon("B", "fire")))
        repeat(50) { assertNotEquals(BattleAction.Special, BotBrain(0f).choose(empty, Random(it))) }
    }

    @Test
    fun `a skilled bot walks away from a losing matchup`() {
        // Fire against Water is a bad day; Grass on the bench beats Water.
        val st = BattleEngine.start(
            listOf(mon("Pool", "water")),
            listOf(mon("Ember", "fire"), mon("Leaf", "grass"))
        )
        val picks = (0 until 30).map { BotBrain(1f).choose(st, Random(it)) }
        assertTrue("never switched", picks.any { it == BattleAction.Switch(1) })
    }

    @Test
    fun `a skilled bot stays in when it is winning`() {
        val st = BattleEngine.start(
            listOf(mon("Leaf", "grass")),
            listOf(mon("Ember", "fire"), mon("Pool", "water"))
        )
        val picks = (0 until 30).map { BotBrain(1f).choose(st, Random(it)) }
        assertTrue(picks.none { it is BattleAction.Switch })
    }

    @Test
    fun `a bot only ever picks legal actions`() {
        val st = BattleEngine.start(listOf(mon("P", "normal")), listOf(mon("B", "fire"), mon("C", "water", hp = 0)))
        repeat(200) { seed ->
            val action = BotBrain(0.3f).choose(st, Random(seed))
            assertTrue("$action", BattleEngine.isLegal(st, Who.FOE, action))
        }
    }

    @Test
    fun `tougher bots field stronger teams`() {
        val avg = 450
        assertTrue(BotTier.ROOKIE.targetBst(avg) < avg)
        assertTrue(BotTier.CHAMPION.targetBst(avg) > avg)
        assertTrue(BotTier.ROOKIE.skill < BotTier.TRAINER.skill && BotTier.TRAINER.skill < BotTier.CHAMPION.skill)
    }

    @Test
    fun `matchmaking never fields a foe the player's team cannot hurt`() {
        val electric = mon("Spark", "electric")
        val ground = mon("Rhyhorn", "ground", "rock")
        // Electric can't touch Ground, so a lone Electric Pokémon must not be given this foe...
        assertFalse(BattleMatchmaker.canDamage(listOf(electric), ground))
        // ...but a team that includes a Ground or Water type can answer it.
        assertTrue(BattleMatchmaker.canDamage(listOf(electric, mon("Pool", "water")), ground))
        assertTrue(BattleMatchmaker.canDamage(listOf(electric), mon("Bird", "flying")))
    }
}
