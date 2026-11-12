package com.aditya1875.pokeverse.feature.game.battle

import com.aditya1875.pokeverse.feature.game.battle.domain.BattleAction
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEngine
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleMath
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleState
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleStats
import com.aditya1875.pokeverse.feature.game.battle.domain.Fighter
import com.aditya1875.pokeverse.feature.game.battle.domain.Who
import com.aditya1875.pokeverse.feature.game.battle.domain.mirrored
import com.aditya1875.pokeverse.feature.game.battle.domain.online.ActionLog
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineAction
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineBattleRules
import com.aditya1875.pokeverse.feature.game.battle.domain.online.Seat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The agreement both phones rely on: the same log always replays to the same battle, a missing choice
 * stops the replay instead of guessing, and nothing a player writes can wedge it.
 */
class OnlineBattleRulesTest {

    private fun mon(name: String, type: String, atk: Int = 100, spe: Int = 100, hp: Int = 100, now: Int? = null): Fighter {
        val s = BattleStats(BattleMath.hp(hp), BattleMath.stat(atk), BattleMath.stat(100), BattleMath.stat(atk), BattleMath.stat(100), BattleMath.stat(spe))
        return Fighter(name.hashCode(), name, listOf(type), s, "", hp = now ?: s.hp)
    }

    private fun start() = BattleEngine.start(
        listOf(mon("A1", "fire", spe = 120), mon("A2", "water"), mon("A3", "grass")),
        listOf(mon("B1", "grass", spe = 80), mon("B2", "normal"), mon("B3", "ice")),
    )

    private val attack = OnlineAction(OnlineAction.KIND_ATTACK)
    private val guard = OnlineAction(OnlineAction.KIND_ATTACK, guard = true)
    private fun both(h: OnlineAction, g: OnlineAction) = mapOf(Seat.HOST to h, Seat.GUEST to g)

    // ── who has to act ───────────────────────────────────────────────────────

    @Test
    fun `a normal turn needs both seats`() {
        assertEquals(setOf(Seat.HOST, Seat.GUEST), OnlineBattleRules.pendingSeats(start()))
    }

    @Test
    fun `after a knockout only the side that fainted must choose`() {
        val st = start()
        val hostDown = st.copy(
            player = st.player.copy(team = st.player.team.toMutableList().also { it[0] = it[0].copy(hp = 0) }),
            playerMustSwitch = true,
        )
        assertEquals(setOf(Seat.HOST), OnlineBattleRules.pendingSeats(hostDown))
        assertEquals(setOf(Seat.GUEST), OnlineBattleRules.pendingSeats(hostDown.mirrored()))
    }

    @Test
    fun `a finished battle needs nobody`() {
        assertTrue(OnlineBattleRules.pendingSeats(start().copy(winner = Who.PLAYER)).isEmpty())
    }

    // ── replay ───────────────────────────────────────────────────────────────

    /** A long exchange with a guard, so a knockout and a replacement round both happen. */
    private fun fullLog(): ActionLog = buildMap {
        for (step in 1..12) {
            put(step, both(if (step % 4 == 0) guard else attack, attack))
        }
    }

    @Test
    fun `the same log replays to the same battle every time`() {
        val a = OnlineBattleRules.replay(start(), 4242L, fullLog())
        val b = OnlineBattleRules.replay(start(), 4242L, fullLog())
        assertEquals(a, b)
    }

    @Test
    fun `a different seed gives a different battle`() {
        val a = OnlineBattleRules.replay(start(), 1L, fullLog())
        val b = OnlineBattleRules.replay(start(), 2L, fullLog())
        // Damage spread and crits come from the dice, so some step must differ.
        assertTrue(a.steps.map { it.result.events } != b.steps.map { it.result.events })
    }

    @Test
    fun `replay stops at the first step still missing a seat and resumes cleanly`() {
        val partial = mapOf(1 to both(attack, attack), 2 to mapOf(Seat.HOST to attack))
        val first = OnlineBattleRules.replay(start(), 7L, partial)
        assertEquals(2, first.nextStep)
        assertEquals(1, first.steps.size)

        // The guest's step-2 choice arrives later: the earlier steps come out exactly as before.
        val longer = partial + (2 to both(attack, attack))
        val second = OnlineBattleRules.replay(start(), 7L, longer)
        assertEquals(3, second.nextStep)
        assertEquals(first.steps.single(), second.steps.first())
    }

    @Test
    fun `replay in a fresh phone matches a phone that played it step by step`() {
        val log = fullLog()
        val whole = OnlineBattleRules.replay(start(), 99L, log)

        var live = start()
        var step = 1
        while (!live.over && log[step] != null && OnlineBattleRules.pendingSeats(live).all { it in log[step]!! }) {
            live = OnlineBattleRules.resolve(live, step, 99L, log[step]!!).state
            step++
        }
        assertEquals(whole.state, live)
        assertEquals(whole.nextStep, step)
    }

    // ── bad input can't wedge a step ─────────────────────────────────────────

    @Test
    fun `an illegal choice falls back to a plain attack`() {
        // A Special with an empty meter is not allowed; the step must still resolve.
        val special = OnlineAction(OnlineAction.KIND_SPECIAL)
        val viaIllegal = OnlineBattleRules.resolve(start(), 1, 5L, both(special, attack))
        val viaAttack = OnlineBattleRules.resolve(start(), 1, 5L, both(attack, attack))
        assertEquals(viaAttack, viaIllegal)
    }

    @Test
    fun `an unknown kind is treated as an attack`() {
        val viaJunk = OnlineBattleRules.resolve(start(), 1, 5L, both(OnlineAction("hack"), attack))
        val viaAttack = OnlineBattleRules.resolve(start(), 1, 5L, both(attack, attack))
        assertEquals(viaAttack, viaJunk)
    }

    /** The guest's front Pokémon is down and the guest must choose. */
    private fun guestMustSwitch(): BattleState {
        val st = start()
        return st.copy(
            foe = st.foe.copy(team = st.foe.team.toMutableList().also { it[0] = it[0].copy(hp = 0) }),
            foeMustSwitch = true,
        )
    }

    @Test
    fun `a replacement round honours a good pick`() {
        val r = OnlineBattleRules.resolve(guestMustSwitch(), 3, 1L, mapOf(Seat.GUEST to OnlineAction.replacement(2)))
        assertEquals(2, r.state.foe.active)
        assertFalse(r.state.foeMustSwitch)
    }

    @Test
    fun `a replacement round with a fainted, missing or non-switch pick still moves on`() {
        listOf(OnlineAction.replacement(0), OnlineAction.replacement(9), attack).forEach { bad ->
            val r = OnlineBattleRules.resolve(guestMustSwitch(), 3, 1L, mapOf(Seat.GUEST to bad))
            assertFalse("pick $bad left the guest stuck", r.state.foeMustSwitch)
            assertTrue(r.state.foe.current.hp > 0)
        }
    }

    @Test
    fun `replay never gets stuck on a knockout`() {
        // Whatever the log holds, if a seat must switch and the log has its entry, the replay passes it.
        val log = fullLog().toMutableMap()
        val result = OnlineBattleRules.replay(start(), 31L, log)
        assertTrue("replay made progress", result.steps.isNotEmpty())
        // Every step it played was one the log could complete.
        assertEquals(result.steps.size + 1, result.nextStep)
    }

    // ── time-outs ────────────────────────────────────────────────────────────

    @Test
    fun `the default move is a plain attack, or the heavy move once the meter is full`() {
        assertEquals(OnlineAction.KIND_ATTACK, OnlineBattleRules.defaultAction(start(), Seat.HOST).kind)
        val st = start()
        val charged = st.copy(player = st.player.copy(team = st.player.team.toMutableList().also { it[0] = it[0].copy(energy = 100) }))
        assertEquals(OnlineAction.KIND_SPECIAL, OnlineBattleRules.defaultAction(charged, Seat.HOST).kind)
    }

    @Test
    fun `the default for a knocked out side is a legal replacement`() {
        val st = guestMustSwitch()
        val pick = OnlineBattleRules.defaultAction(st, Seat.GUEST)
        assertEquals(OnlineAction.KIND_SWITCH, pick.kind)
        assertFalse(st.foe.team[pick.index].fainted)
    }

    // ── ending ───────────────────────────────────────────────────────────────

    @Test
    fun `the battle's own winner decides, mapped to a seat`() {
        assertEquals(Seat.HOST, OnlineBattleRules.outcome(start().copy(winner = Who.PLAYER), null, null)?.winner)
        assertEquals(Seat.GUEST, OnlineBattleRules.outcome(start().copy(winner = Who.FOE), null, null)?.winner)
        assertFalse(OnlineBattleRules.outcome(start().copy(winner = Who.FOE), null, null)!!.byLeaving)
    }

    @Test
    fun `quitting or vanishing hands the win to the other seat`() {
        assertEquals(Seat.GUEST, OnlineBattleRules.outcome(start(), quit = Seat.HOST, abandoned = null)?.winner)
        assertEquals(Seat.HOST, OnlineBattleRules.outcome(start(), quit = null, abandoned = Seat.GUEST)?.winner)
        assertTrue(OnlineBattleRules.outcome(start(), Seat.HOST, null)!!.byLeaving)
    }

    @Test
    fun `no result while the battle is still on`() {
        assertNull(OnlineBattleRules.outcome(start(), null, null))
    }

    @Test
    fun `a real result beats a late quit`() {
        // The battle ended on the board first; a quit written afterwards must not flip it.
        val o = OnlineBattleRules.outcome(start().copy(winner = Who.PLAYER), quit = Seat.HOST, abandoned = null)
        assertEquals(Seat.HOST, o?.winner)
        assertFalse(o!!.byLeaving)
    }

    // ── rewards ──────────────────────────────────────────────────────────────

    @Test
    fun `a very short battle or a farmed rival pays nothing`() {
        assertFalse(OnlineBattleRules.paysRewards(turns = 2, paidWithThisRivalToday = 0))
        assertTrue(OnlineBattleRules.paysRewards(turns = OnlineBattleRules.MIN_TURNS_FOR_REWARD, paidWithThisRivalToday = 0))
        assertTrue(OnlineBattleRules.paysRewards(5, OnlineBattleRules.REWARDED_PER_RIVAL_PER_DAY - 1))
        assertFalse(OnlineBattleRules.paysRewards(5, OnlineBattleRules.REWARDED_PER_RIVAL_PER_DAY))
    }

    // ── teams from the wire ──────────────────────────────────────────────────

    @Test
    fun `only a real, bounded, duplicate-free team is trusted`() {
        assertTrue(OnlineBattleRules.validTeam(listOf(25, 6, 9), 1025))
        assertFalse(OnlineBattleRules.validTeam(null, 1025))
        assertFalse(OnlineBattleRules.validTeam(emptyList(), 1025))
        assertFalse(OnlineBattleRules.validTeam(listOf(1, 2, 3, 4, 5, 6, 7), 1025))
        assertFalse(OnlineBattleRules.validTeam(listOf(0), 1025))
        assertFalse(OnlineBattleRules.validTeam(listOf(2000), 1025))
        assertFalse(OnlineBattleRules.validTeam(listOf(25, 25), 1025))
    }

    @Test
    fun `actions map to and from the engine`() {
        listOf<BattleAction>(BattleAction.Attack, BattleAction.Special, BattleAction.Switch(3)).forEach {
            assertEquals(it, OnlineAction.of(it).toBattleAction())
        }
        assertNotNull(Seat.fromCode("h"))
        assertNull(Seat.fromCode("x"))
        assertEquals(Seat.GUEST, Seat.HOST.other)
    }

    @Test
    fun `online teams must be the same size`() {
        assertTrue(OnlineBattleRules.evenTeams(listOf(1, 2, 3), listOf(4, 5, 6)))
        assertFalse(OnlineBattleRules.evenTeams(listOf(1, 2, 3), listOf(4, 5, 6, 7, 8, 9)))
        assertFalse(OnlineBattleRules.evenTeams(null, listOf(1)))
        // The quick formats are 3 vs 3 and 6 vs 6, and nothing in between.
        assertEquals(listOf(3, 6), com.aditya1875.pokeverse.feature.game.battle.domain.TeamFormat.entries.mapNotNull { it.size })
    }
}
