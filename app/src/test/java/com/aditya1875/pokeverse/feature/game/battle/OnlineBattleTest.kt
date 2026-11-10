package com.aditya1875.pokeverse.feature.game.battle

import com.aditya1875.pokeverse.feature.game.battle.domain.BattleAction
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEngine
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEvent
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleMath
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleState
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleStats
import com.aditya1875.pokeverse.feature.game.battle.domain.Fighter
import com.aditya1875.pokeverse.feature.game.battle.domain.Who
import com.aditya1875.pokeverse.feature.game.battle.domain.mirrored
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * What online Team Battle relies on: two phones that start from the same state, the same seed and the
 * same two actions must land on exactly the same battle, whichever seat they sit in.
 */
class OnlineBattleTest {

    private fun mon(name: String, type: String, atk: Int = 100, spe: Int = 100, hp: Int = 100, now: Int? = null) =
        BattleStats(BattleMath.hp(hp), BattleMath.stat(atk), BattleMath.stat(100), BattleMath.stat(atk), BattleMath.stat(100), BattleMath.stat(spe))
            .let { s -> Fighter(name.hashCode(), name, listOf(type), s, "", hp = now ?: s.hp) }

    private fun start() = BattleEngine.start(
        listOf(mon("A1", "fire", spe = 120), mon("A2", "water")),
        listOf(mon("B1", "grass", spe = 80), mon("B2", "normal")),
    )

    // ── the dice ─────────────────────────────────────────────────────────────

    @Test
    fun `the same seed and turn give the same dice, a different turn does not`() {
        val a = BattleEngine.turnRandom(42L, 3)
        val b = BattleEngine.turnRandom(42L, 3)
        assertEquals((0 until 5).map { a.nextInt() }, (0 until 5).map { b.nextInt() })
        assertNotEquals(BattleEngine.turnRandom(42L, 3).nextInt(), BattleEngine.turnRandom(42L, 4).nextInt())
        assertNotEquals(BattleEngine.turnRandom(42L, 3).nextInt(), BattleEngine.turnRandom(43L, 3).nextInt())
    }

    @Test
    fun `two phones resolving the same turn get the identical result`() {
        var host = start()
        var guest = start()
        val moves = listOf(
            BattleAction.Attack to BattleAction.Guard,
            BattleAction.Attack to BattleAction.Attack,
            BattleAction.Guard to BattleAction.Attack,
            BattleAction.Attack to BattleAction.Attack,
        )
        moves.forEach { (p, f) ->
            if (host.over) return@forEach
            val h = BattleEngine.resolveTurn(host, p, f, BattleEngine.turnRandom(99L, host.turn), botReplaces = false)
            val g = BattleEngine.resolveTurn(guest, p, f, BattleEngine.turnRandom(99L, guest.turn), botReplaces = false)
            assertEquals(h, g)
            host = h.state
            guest = g.state
        }
    }

    // ── mirroring ────────────────────────────────────────────────────────────

    @Test
    fun `mirroring twice gives the original back`() {
        val result = BattleEngine.resolveTurn(start(), BattleAction.Attack, BattleAction.Special, Random(1), botReplaces = false)
        assertEquals(result.state, result.state.mirrored().mirrored())
        assertEquals(result.events, result.events.map { it.mirrored().mirrored() })
    }

    @Test
    fun `the guest sees its own side as the player and the host as the foe`() {
        val s = start()
        val seenByGuest = s.mirrored()
        assertEquals("B1", seenByGuest.player.current.name)
        assertEquals("A1", seenByGuest.foe.current.name)
    }

    @Test
    fun `a blow the host dealt is a blow the guest took`() {
        val host = BattleEngine.resolveTurn(start(), BattleAction.Attack, BattleAction.Guard, Random(5), botReplaces = false)
        val hit = host.events.filterIsInstance<BattleEvent.Hit>().first { it.by == Who.PLAYER }
        val guestHit = host.mirrored().events.filterIsInstance<BattleEvent.Hit>().first { it.damage == hit.damage }
        assertEquals(Who.FOE, guestHit.by)
    }

    @Test
    fun `a winner flips with the seat`() {
        val won = BattleState(
            BattleEngine.start(listOf(mon("A", "fire")), listOf(mon("B", "grass"))).player,
            BattleEngine.start(listOf(mon("A", "fire")), listOf(mon("B", "grass"))).foe,
            winner = Who.PLAYER,
        )
        assertEquals(Who.FOE, won.mirrored().winner)
    }

    // ── replacements chosen by both players ──────────────────────────────────

    /** The faster side (A1) is one hit from nothing and the foe (B1) is one hit from fainting: A1 strikes first. */
    private fun foeOnTheEdge() = BattleEngine.start(
        listOf(mon("A1", "fire", spe = 120), mon("A2", "water")),
        listOf(mon("B1", "grass", spe = 80, now = 1), mon("B2", "normal")),
    )

    /** Both fronts already down, as after a double knockout. */
    private fun bothDown(): BattleState {
        val s = start()
        fun down(side: com.aditya1875.pokeverse.feature.game.battle.domain.Side) =
            side.copy(team = side.team.toMutableList().also { it[0] = it[0].copy(hp = 0) })
        return s.copy(player = down(s.player), foe = down(s.foe), playerMustSwitch = true, foeMustSwitch = true)
    }

    @Test
    fun `online, a knocked out foe is not replaced by the bot, the other player is asked`() {
        val r = BattleEngine.resolveTurn(foeOnTheEdge(), BattleAction.Attack, BattleAction.Guard, Random(3), botReplaces = false)
        assertTrue(r.state.foeMustSwitch)
        assertFalse(r.state.playerMustSwitch)
        assertEquals("B1 stays down until its trainer picks", 0, r.state.foe.current.hp)
        assertFalse(r.state.over)
    }

    @Test
    fun `against the bot nothing changes, it still replaces itself`() {
        val r = BattleEngine.resolveTurn(foeOnTheEdge(), BattleAction.Attack, BattleAction.Guard, Random(3))
        assertFalse(r.state.foeMustSwitch)
        assertTrue(r.state.foe.current.hp > 0)
    }

    @Test
    fun `no one acts again until both replacements are in`() {
        val down = bothDown()
        val again = BattleEngine.resolveTurn(down, BattleAction.Attack, BattleAction.Attack, Random(3), botReplaces = false)
        assertEquals(down, again.state)

        val both = BattleEngine.forceSwitchBoth(down, playerIndex = 1, foeIndex = 1)
        assertEquals(1, both.state.player.active)
        assertEquals(1, both.state.foe.active)
        assertFalse(both.state.playerMustSwitch)
        assertFalse(both.state.foeMustSwitch)
        assertEquals(listOf(Who.PLAYER, Who.FOE), both.events.filterIsInstance<BattleEvent.Switched>().map { it.who })
    }

    @Test
    fun `a fainted or missing replacement is refused`() {
        val st = bothDown()
        // Index 0 is the Pokémon that just fainted; 9 does not exist.
        assertEquals(st, BattleEngine.forceSwitch(st, Who.FOE, 0).state)
        assertEquals(st, BattleEngine.forceSwitch(st, Who.FOE, 9).state)
        // Only the side that needs one may switch.
        val onlyPlayer = st.copy(foeMustSwitch = false)
        assertEquals(onlyPlayer, BattleEngine.forceSwitch(onlyPlayer, Who.FOE, 1).state)
    }
}
