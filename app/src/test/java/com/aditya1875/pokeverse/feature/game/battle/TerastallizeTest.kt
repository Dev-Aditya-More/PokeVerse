package com.aditya1875.pokeverse.feature.game.battle

import com.aditya1875.pokeverse.feature.game.battle.domain.BattleAction
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEngine
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEvent
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleMath
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleStats
import com.aditya1875.pokeverse.feature.game.battle.domain.Fighter
import com.aditya1875.pokeverse.feature.game.battle.domain.TeraTypes
import com.aditya1875.pokeverse.feature.game.battle.domain.Who
import com.aditya1875.pokeverse.feature.game.battle.domain.mirrored
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineAction
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineBattleRules
import com.aditya1875.pokeverse.feature.game.battle.domain.online.Seat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Terastallize: once per side, one fixed type per species, moves follow the new type, both phones agree. */
class TerastallizeTest {

    private fun mon(id: Int, type: String, spe: Int = 100): Fighter {
        val s = BattleStats(BattleMath.hp(100), BattleMath.stat(100), BattleMath.stat(100), BattleMath.stat(100), BattleMath.stat(100), BattleMath.stat(spe))
        return Fighter(id, "M$id", listOf(type), s, "")
    }

    private fun start() = BattleEngine.start(
        listOf(mon(1, "fire", spe = 120), mon(2, "water")),
        listOf(mon(3, "grass", spe = 80), mon(4, "normal")),
    )

    private fun turn(st: com.aditya1875.pokeverse.feature.game.battle.domain.BattleState, p: BattleAction, f: BattleAction, pt: Boolean = false, ft: Boolean = false) =
        BattleEngine.resolveTurn(st, p, f, Random(7), botReplaces = false, playerTera = pt, foeTera = ft)

    @Test
    fun `a species always gets the same tera type and never one it already has`() {
        for (id in 1..1025) {
            val types = listOf("fire", "flying")
            val t = TeraTypes.forSpecies(id, types)
            assertTrue(t in TeraTypes.ALL)
            assertFalse(t in types)
            assertEquals(t, TeraTypes.forSpecies(id, types))
        }
    }

    @Test
    fun `terastallizing swaps the type and both moves follow`() {
        val r = turn(start(), BattleAction.Attack, BattleAction.Attack, pt = true)
        val me = r.state.player.current
        assertTrue(me.terastallized)
        assertEquals(listOf(me.teraType), me.types)
        assertEquals(me.teraType, me.primaryType)
        assertEquals(me.teraType, me.specialType)
        assertTrue(r.state.player.teraUsed)
        assertFalse(r.state.foe.teraUsed)
        val hit = r.events.filterIsInstance<BattleEvent.Hit>().first { it.by == Who.PLAYER }
        assertEquals(me.teraType, hit.attackerType)
    }

    @Test
    fun `the tera event comes before any blow`() {
        val r = turn(start(), BattleAction.Attack, BattleAction.Attack, pt = true, ft = true)
        val firstHit = r.events.indexOfFirst { it is BattleEvent.Hit }
        assertEquals(2, r.events.count { it is BattleEvent.Terastallized })
        assertTrue(r.events.indexOfFirst { it is BattleEvent.Terastallized } < firstHit)
    }

    @Test
    fun `only one use per side`() {
        val first = turn(start(), BattleAction.Attack, BattleAction.Attack, pt = true).state
        assertFalse(BattleEngine.canTera(first, Who.PLAYER))
        val second = turn(first, BattleAction.Attack, BattleAction.Attack, pt = true)
        assertEquals(0, second.events.count { it is BattleEvent.Terastallized })
    }

    @Test
    fun `switching cannot terastallize`() {
        val r = turn(start(), BattleAction.Switch(1), BattleAction.Attack, pt = true)
        assertFalse(r.state.player.teraUsed)
        assertFalse(r.state.player.team.any { it.terastallized })
    }

    @Test
    fun `a terastallized pokemon keeps its new type when it comes back`() {
        var st = turn(start(), BattleAction.Attack, BattleAction.Attack, pt = true).state
        st = turn(st, BattleAction.Switch(1), BattleAction.Attack).state
        st = turn(st, BattleAction.Switch(0), BattleAction.Attack).state
        assertTrue(st.player.current.terastallized)
        assertEquals(listOf(st.player.current.teraType), st.player.current.types)
    }

    @Test
    fun `both seats compute the same battle from a log with tera in it`() {
        val host = OnlineAction(OnlineAction.KIND_ATTACK, tera = true)
        val guest = OnlineAction(OnlineAction.KIND_SPECIAL)
        val entries = mapOf(Seat.HOST to host, Seat.GUEST to guest)
        val a = OnlineBattleRules.resolve(start(), 1, 99L, entries)
        val b = OnlineBattleRules.resolve(start(), 1, 99L, entries)
        assertEquals(a.state, b.state)
        // The guest sees the same turn from the other chair.
        val seenByGuest = a.mirrored()
        assertEquals(Who.FOE, seenByGuest.events.filterIsInstance<BattleEvent.Terastallized>().single().who)
        assertTrue(seenByGuest.state.foe.teraUsed)
    }

    @Test
    fun `tera on a switch or a spent side in the log is ignored not fatal`() {
        val entries = mapOf(
            Seat.HOST to OnlineAction(OnlineAction.KIND_SWITCH, 1, tera = true),
            Seat.GUEST to OnlineAction(OnlineAction.KIND_ATTACK),
        )
        val r = OnlineBattleRules.resolve(start(), 1, 5L, entries)
        assertFalse(r.state.player.teraUsed)
    }
}
