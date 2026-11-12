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

    /** One turn where only [who] adds a Guard to a plain Attack. */
    private fun guardTurn(st: com.aditya1875.pokeverse.feature.game.battle.domain.BattleState, who: Who) =
        BattleEngine.resolveTurn(
            st, BattleAction.Attack, BattleAction.Attack, Random(1),
            playerGuard = who == Who.PLAYER, foeGuard = who == Who.FOE,
        ).state

    @Test
    fun `a side starts with two Guards`() {
        val st = start()
        assertEquals(2, BattleEngine.MAX_GUARDS)
        assertEquals(2, st.player.guardsLeft)
        assertEquals(2, st.foe.guardsLeft)
    }

    @Test
    fun `each Guard used costs one and the third is not allowed`() {
        var st = start()
        repeat(2) { st = guardTurn(st, Who.PLAYER) }
        assertEquals(0, st.player.guardsLeft)
        assertEquals("the foe's Guards are its own", 2, st.foe.guardsLeft)
        assertFalse(BattleEngine.canGuard(st, Who.PLAYER, BattleAction.Attack))
        assertTrue(BattleEngine.canGuard(st, Who.FOE, BattleAction.Attack))
    }

    @Test
    fun `a Guard goes with an Attack or a Special, never a Switch`() {
        val st = start()
        assertTrue(BattleEngine.canGuard(st, Who.PLAYER, BattleAction.Attack))
        assertTrue(BattleEngine.canGuard(st, Who.PLAYER, BattleAction.Special))
        assertFalse(BattleEngine.canGuard(st, Who.PLAYER, BattleAction.Switch(1)))
    }

    @Test
    fun `a Guard with none left is ignored and the move still happens`() {
        var st = start()
        repeat(2) { st = guardTurn(st, Who.PLAYER) }
        val r = BattleEngine.resolveTurn(st, BattleAction.Attack, BattleAction.Attack, Random(2), playerGuard = true, foeGuard = true)
        // It did not guard (nothing was spent or shown) and still struck.
        assertTrue(r.events.none { it is BattleEvent.Guarded && it.by == Who.PLAYER })
        assertTrue(r.events.any { it is BattleEvent.Hit && it.by == Who.PLAYER })
        assertEquals(0, r.state.player.guardsLeft)
        // The foe's own Guard was still allowed.
        assertEquals(1, r.state.foe.guardsLeft)
    }

    @Test
    fun `a Guard added to a Switch is ignored and costs nothing`() {
        val r = BattleEngine.resolveTurn(start(), BattleAction.Switch(1), BattleAction.Attack, Random(3), playerGuard = true)
        assertEquals(2, r.state.player.guardsLeft)
        assertTrue(r.events.none { it is BattleEvent.Guarded && it.by == Who.PLAYER })
    }

    @Test
    fun `Guards belong to the team, not to the Pokemon`() {
        var st = start()
        st = guardTurn(st, Who.PLAYER)
        // Switch to the second Pokemon: the team has still only one Guard left.
        st = BattleEngine.resolveTurn(st, BattleAction.Switch(1), BattleAction.Attack, Random(3)).state
        assertEquals(1, st.player.active)
        assertEquals(1, st.player.guardsLeft)
        st = guardTurn(st, Who.PLAYER)
        assertEquals(0, st.player.guardsLeft)
    }

    @Test
    fun `both sides can guard in the same turn and each pays one`() {
        val st = BattleEngine.resolveTurn(start(), BattleAction.Attack, BattleAction.Attack, Random(4), playerGuard = true, foeGuard = true).state
        assertEquals(1, st.player.guardsLeft)
        assertEquals(1, st.foe.guardsLeft)
    }

    @Test
    fun `a Guard resists forty percent of the hit and the move still lands`() {
        val plain = BattleEngine.resolveTurn(start(), BattleAction.Attack, BattleAction.Attack, Random(5))
        val guarded = BattleEngine.resolveTurn(start(), BattleAction.Attack, BattleAction.Attack, Random(5), playerGuard = true)
        fun hitOn(r: com.aditya1875.pokeverse.feature.game.battle.domain.TurnResult, who: Who) =
            r.events.filterIsInstance<BattleEvent.Hit>().first { it.by == who }
        // The player's own blow is unchanged by the player's Guard...
        assertEquals(hitOn(plain, Who.PLAYER).damage, hitOn(guarded, Who.PLAYER).damage)
        // ...and the blow the player takes lands at 60%, give or take the rounding down of one point.
        val full = hitOn(plain, Who.FOE).damage
        val soft = hitOn(guarded, Who.FOE).damage
        assertTrue("full=$full soft=$soft", soft in (full * 0.6).toInt() - 1..(full * 0.6).toInt() + 1)
        assertTrue(soft < full)
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
            assertFalse(BotBrain(skill).wantsGuard(noGuards, BattleAction.Attack, random))
        }
    }

    @Test
    fun `the bot never guards a switch`() {
        repeat(100) { assertFalse(BotBrain(1f).wantsGuard(start(), BattleAction.Switch(1), Random(it))) }
    }

    @Test
    fun `guards follow the seat when the battle is mirrored for the guest`() {
        var st = start()
        st = guardTurn(st, Who.PLAYER)
        val guestView = st.mirrored()
        assertEquals(2, guestView.player.guardsLeft) // the guest's own, untouched
        assertEquals(1, guestView.foe.guardsLeft)    // the host's, one used
    }

    @Test
    fun `an online replay enforces the limit the same way on both phones`() {
        val guard = OnlineAction(OnlineAction.KIND_ATTACK, guard = true)
        val attack = OnlineAction(OnlineAction.KIND_ATTACK)
        val log = (1..6).associateWith { mapOf(Seat.HOST to guard, Seat.GUEST to attack) }
        val a = OnlineBattleRules.replay(start(), 5L, log)
        val b = OnlineBattleRules.replay(start(), 5L, log)
        assertEquals(a, b)
        assertEquals(0, a.state.player.guardsLeft)
        assertEquals(2, a.state.foe.guardsLeft)
    }

    @Test
    fun `the guard flag changes the online result, so a phone that ignores it would disagree`() {
        val withGuard = mapOf(1 to mapOf(Seat.HOST to OnlineAction(OnlineAction.KIND_ATTACK, guard = true), Seat.GUEST to OnlineAction(OnlineAction.KIND_ATTACK)))
        val without = mapOf(1 to mapOf(Seat.HOST to OnlineAction(OnlineAction.KIND_ATTACK), Seat.GUEST to OnlineAction(OnlineAction.KIND_ATTACK)))
        val a = OnlineBattleRules.replay(start(), 5L, withGuard).state
        val b = OnlineBattleRules.replay(start(), 5L, without).state
        assertTrue("the Guard softened the hit the host took", a.player.current.hp > b.player.current.hp)
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
