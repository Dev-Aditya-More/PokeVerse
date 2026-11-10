package com.aditya1875.pokeverse.feature.game

import com.aditya1875.pokeverse.feature.game.battle.domain.BattleAction
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEngine
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEvent
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleMath
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleStats
import com.aditya1875.pokeverse.feature.game.battle.domain.BotBrain
import com.aditya1875.pokeverse.feature.game.battle.domain.Fighter
import com.aditya1875.pokeverse.feature.game.battle.domain.Who
import com.aditya1875.pokeverse.feature.game.battle.domain.mirrored
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineAction
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineBattleRules
import com.aditya1875.pokeverse.feature.game.battle.domain.online.Seat
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchSpawns
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.RecentEncounters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CatchSpawnsTest {

    private fun rolls(n: Int, encounter: Int = 10, seed: Int = 1): Map<CatchTier, Int> {
        val random = Random(seed)
        return (1..n).map { CatchSpawns.rollTier(encounter, random) }.groupingBy { it }.eachCount()
    }

    @Test
    fun `the weights add up to a hundred`() {
        assertEquals(100, CatchSpawns.WEIGHTS.values.sum())
    }

    @Test
    fun `every category follows its share, so commons are usual and legendaries rare`() {
        val n = 20_000
        val counts = rolls(n)
        CatchSpawns.WEIGHTS.forEach { (tier, pct) ->
            val seen = (counts[tier] ?: 0) * 100.0 / n
            assertEquals("$tier share", pct.toDouble(), seen, 1.5)
        }
        assertTrue(counts.getValue(CatchTier.COMMON) > counts.getValue(CatchTier.RARE))
        assertTrue(counts.getValue(CatchTier.RARE) > counts.getValue(CatchTier.EPIC))
        assertTrue(counts.getValue(CatchTier.EPIC) > counts.getValue(CatchTier.LEGENDARY))
    }

    @Test
    fun `the first encounters are only common or uncommon`() {
        for (encounter in 0 until CatchSpawns.FRIENDLY_ENCOUNTERS) {
            val seen = rolls(3_000, encounter).keys
            assertTrue("encounter $encounter rolled $seen", seen.all { it == CatchTier.COMMON || it == CatchTier.UNCOMMON })
        }
        // ...and after that, everything can appear.
        assertEquals(CatchTier.entries.toSet(), rolls(5_000, CatchSpawns.FRIENDLY_ENCOUNTERS).keys)
    }

    @Test
    fun `the nearest category is the fallback`() {
        assertTrue(CatchSpawns.distance(CatchTier.RARE, CatchTier.EPIC) < CatchSpawns.distance(CatchTier.COMMON, CatchTier.EPIC))
        assertEquals(0, CatchSpawns.distance(CatchTier.RARE, CatchTier.RARE))
    }

    @Test
    fun `the direct ultra rare list holds real, distinct, catchable ids`() {
        assertEquals(CatchSpawns.ULTRA_RARE_SEEDS.size, CatchSpawns.ULTRA_RARE_SEEDS.toSet().size)
        assertTrue(CatchSpawns.ULTRA_RARE_SEEDS.all { it in 1..809 })
    }

    // ── no immediate repeats ─────────────────────────────────────────────────

    @Test
    fun `a Pokemon seen lately is remembered, and forgotten after the memory runs out`() {
        val recent = RecentEncounters(capacity = 3)
        recent.remember(52)            // Meowth
        assertTrue(52 in recent)
        recent.remember(1); recent.remember(2)
        assertTrue(52 in recent)       // still inside the last three
        recent.remember(3)
        assertFalse(52 in recent)      // pushed out
    }

    @Test
    fun `meeting the same one again moves it to the front instead of filling the memory`() {
        val recent = RecentEncounters(capacity = 3)
        recent.remember(1); recent.remember(2); recent.remember(1)
        assertEquals(setOf(1, 2), recent.snapshot())
    }

    @Test
    fun `the default memory covers a dozen encounters`() {
        val recent = RecentEncounters()
        (1..CatchSpawns.RECENT_MEMORY).forEach { recent.remember(it) }
        assertEquals(CatchSpawns.RECENT_MEMORY, recent.snapshot().size)
        recent.remember(999)
        assertFalse(1 in recent)
    }
}

class GuardLimitTest {

    private fun mon(name: String, type: String = "normal", hp: Int = 200): Fighter {
        val s = BattleStats(BattleMath.hp(hp), BattleMath.stat(30), BattleMath.stat(200), BattleMath.stat(30), BattleMath.stat(200), BattleMath.stat(100))
        return Fighter(name.hashCode(), name, listOf(type), s, "")
    }

    private fun start() = BattleEngine.start(
        listOf(mon("A1"), mon("A2")),
        listOf(mon("B1"), mon("B2")),
    )

    private fun guardTurn(st: com.aditya1875.pokeverse.feature.game.battle.domain.BattleState, who: Who) =
        BattleEngine.resolveTurn(
            st,
            if (who == Who.PLAYER) BattleAction.Guard else BattleAction.Attack,
            if (who == Who.FOE) BattleAction.Guard else BattleAction.Attack,
            Random(1),
        ).state

    @Test
    fun `a side starts with three Guards`() {
        val st = start()
        assertEquals(3, BattleEngine.MAX_GUARDS)
        assertEquals(3, st.player.guardsLeft)
        assertEquals(3, st.foe.guardsLeft)
    }

    @Test
    fun `each Guard used costs one and the fourth is not allowed`() {
        var st = start()
        repeat(3) { st = guardTurn(st, Who.PLAYER) }
        assertEquals(0, st.player.guardsLeft)
        assertEquals("the foe's Guards are its own", 3, st.foe.guardsLeft)
        assertFalse(BattleEngine.isLegal(st, Who.PLAYER, BattleAction.Guard))
        assertTrue(BattleEngine.isLegal(st, Who.FOE, BattleAction.Guard))
    }

    @Test
    fun `a Guard with none left becomes a plain attack`() {
        var st = start()
        repeat(3) { st = guardTurn(st, Who.PLAYER) }
        val r = BattleEngine.resolveTurn(st, BattleAction.Guard, BattleAction.Guard, Random(2))
        // It did not guard (nothing was spent or shown) and instead struck.
        assertTrue(r.events.none { it is BattleEvent.Guarded && it.by == Who.PLAYER })
        assertTrue(r.events.any { it is BattleEvent.Hit && it.by == Who.PLAYER })
        assertEquals(0, r.state.player.guardsLeft)
        // The foe's own Guard was still allowed.
        assertEquals(2, r.state.foe.guardsLeft)
    }

    @Test
    fun `Guards belong to the team, not to the Pokemon`() {
        var st = start()
        st = guardTurn(st, Who.PLAYER)
        // Switch to the second Pokemon: the team has still only two Guards left.
        st = BattleEngine.resolveTurn(st, BattleAction.Switch(1), BattleAction.Attack, Random(3)).state
        assertEquals(1, st.player.active)
        assertEquals(2, st.player.guardsLeft)
        st = guardTurn(st, Who.PLAYER)
        assertEquals(1, st.player.guardsLeft)
    }

    @Test
    fun `both sides can guard in the same turn and each pays one`() {
        val st = BattleEngine.resolveTurn(start(), BattleAction.Guard, BattleAction.Guard, Random(4)).state
        assertEquals(2, st.player.guardsLeft)
        assertEquals(2, st.foe.guardsLeft)
    }

    @Test
    fun `the bot never plays a Guard it does not have`() {
        val st = start()
        val noGuards = st.copy(foe = st.foe.copy(guardsLeft = 0), player = st.player.copy(
            team = st.player.team.toMutableList().also { it[0] = it[0].copy(energy = 100) }
        ))
        val random = Random(9)
        repeat(400) {
            val skill = if (it % 2 == 0) 0.0f else 1.0f // random play and smart play
            assertFalse(BotBrain(skill).choose(noGuards, random) == BattleAction.Guard)
        }
    }

    @Test
    fun `guards follow the seat when the battle is mirrored for the guest`() {
        var st = start()
        st = guardTurn(st, Who.PLAYER)
        val guestView = st.mirrored()
        assertEquals(3, guestView.player.guardsLeft) // the guest's own, untouched
        assertEquals(2, guestView.foe.guardsLeft)    // the host's, one used
    }

    @Test
    fun `an online replay enforces the limit the same way on both phones`() {
        val guard = OnlineAction(OnlineAction.KIND_GUARD)
        val attack = OnlineAction(OnlineAction.KIND_ATTACK)
        val log = (1..6).associateWith { mapOf(Seat.HOST to guard, Seat.GUEST to attack) }
        val a = OnlineBattleRules.replay(start(), 5L, log)
        val b = OnlineBattleRules.replay(start(), 5L, log)
        assertEquals(a, b)
        assertEquals(0, a.state.player.guardsLeft)
        assertEquals(3, a.state.foe.guardsLeft)
    }
}

private typealias TA = com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.ThrowAccuracy

class BallWeightTest {
    private fun chance(ball: com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType,
                       acc: com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.ThrowAccuracy,
                       tier: CatchTier, n: Int) =
        com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchOdds.chance(acc, n, tier, ball)

    @Test
    fun `every ball beats the one below it at every throw, category and point in the run`() {
        val balls = com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType.entries
        com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.ThrowAccuracy.entries.forEach { acc ->
            CatchTier.entries.forEach { tier ->
                listOf(0, 10, 40).forEach { n ->
                    balls.zipWithNext().forEach { (lower, higher) ->
                        assertTrue(
                            "$higher should beat $lower at $acc/$tier/#$n",
                            chance(higher, acc, tier, n) > chance(lower, acc, tier, n)
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `the Master Ball always catches and the others never quite do`() {
        com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.ThrowAccuracy.entries.forEach { acc ->
            assertEquals(1f, chance(com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType.MASTER, acc, CatchTier.LEGENDARY, 40), 0f)
            assertTrue(chance(com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType.ULTRA, acc, CatchTier.COMMON, 0) < 1f)
        }
    }

    @Test
    fun `a better throw is always worth more, whatever the ball`() {
        com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType.entries
            .filter { !it.guaranteed }
            .forEach { ball ->
                val c = listOf(TA.MISS, TA.GOOD, TA.GREAT, TA.EXCELLENT).map { chance(ball, it, CatchTier.RARE, 10) }
                assertEquals(c.sorted(), c)
                assertEquals(c.toSet().size, c.size)
            }
    }
}
