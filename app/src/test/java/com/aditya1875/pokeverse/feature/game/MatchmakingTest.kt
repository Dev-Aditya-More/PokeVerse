package com.aditya1875.pokeverse.feature.game

import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineBattleRules
import com.aditya1875.pokeverse.feature.game.core.domain.Matchmaking
import com.aditya1875.pokeverse.feature.game.core.domain.Matchmaking.OpenRoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchmakingTest {

    private val now = 1_000_000L
    private val stale = 40_000L

    private fun room(id: String, host: String, ageMs: Long = 1_000L) = OpenRoom(id, host, now - ageMs)

    // ── who is a candidate ──────────────────────────────────────────────────────────

    @Test
    fun `your own room and abandoned rooms are never candidates`() {
        val rooms = listOf(room("a", "me"), room("b", "x", ageMs = 5_000), room("c", "y", ageMs = 50_000))
        assertEquals(listOf("b"), Matchmaking.live(rooms, "me", now, stale).map { it.id })
    }

    @Test
    fun `a room exactly as old as the limit is already stale`() {
        assertTrue(Matchmaking.live(listOf(room("a", "x", ageMs = stale)), "me", now, stale).isEmpty())
        assertEquals(1, Matchmaking.live(listOf(room("a", "x", ageMs = stale - 1)), "me", now, stale).size)
    }

    // ── the simultaneous-tap problem ────────────────────────────────────────────────

    @Test
    fun `of two waiting players exactly one yields`() {
        val a = room("aaa", "playerA")
        val b = room("bbb", "playerB")
        val aYields = Matchmaking.roomToYieldTo(a.id, listOf(b))
        val bYields = Matchmaking.roomToYieldTo(b.id, listOf(a))
        assertNull("the smaller room keeps waiting", aYields)
        assertEquals("the larger room joins the smaller", "aaa", bYields)
    }

    @Test
    fun `it never depends on which phone looks first`() {
        // Whatever the ids, the two answers are always one null and one id: never both, never neither.
        val ids = listOf("0Kd8", "zz91", "AAAA", "m2", "9", "Zq", "a", "A")
        for (x in ids) for (y in ids) {
            if (x == y) continue
            val xMoves = Matchmaking.roomToYieldTo(x, listOf(room(y, "y"))) != null
            val yMoves = Matchmaking.roomToYieldTo(y, listOf(room(x, "x"))) != null
            assertTrue("$x vs $y: exactly one must move", xMoves != yMoves)
        }
    }

    @Test
    fun `with many waiting they all drain towards the smallest room`() {
        val rooms = listOf(room("c", "pc"), room("a", "pa"), room("d", "pd"), room("b", "pb"))
        val moves = rooms.associate { mine -> mine.id to Matchmaking.roomToYieldTo(mine.id, rooms.filter { it.id != mine.id }) }
        assertEquals(mapOf("a" to null, "b" to "a", "c" to "a", "d" to "a"), moves)
    }

    @Test
    fun `nobody else waiting means keep waiting`() {
        assertNull(Matchmaking.roomToYieldTo("zzz", emptyList()))
    }

    @Test
    fun `a waiting player never yields to a room that would not be live for them`() {
        val rooms = listOf(room("a", "me"), room("b", "gone", ageMs = 99_000))
        val candidates = Matchmaking.live(rooms, "me", now, stale)
        assertNull(Matchmaking.roomToYieldTo("zzz", candidates))
    }

    // ── when to look ────────────────────────────────────────────────────────────────

    @Test
    fun `a waiting player looks every couple of seconds, not at the start`() {
        assertFalse(Matchmaking.shouldLookNow(0))
        assertFalse(Matchmaking.shouldLookNow(1))
        assertTrue(Matchmaking.shouldLookNow(2))
        assertFalse(Matchmaking.shouldLookNow(3))
        assertTrue(Matchmaking.shouldLookNow(30))
    }

    // ── the "players in the pool" count ─────────────────────────────────────────────

    @Test
    fun `the pool counts other live players, once each`() {
        val rooms = listOf(
            room("a", "me"),
            room("b", "p1"),
            room("c", "p1"),                       // the same player with two rooms is still one player
            room("d", "p2"),
            room("e", "p3", ageMs = 90_000),       // abandoned
        )
        assertEquals(2, Matchmaking.waitingCount(rooms, "me", now, stale))
    }

    @Test
    fun `an empty pool counts zero`() {
        assertEquals(0, Matchmaking.waitingCount(emptyList(), "me", now, stale))
    }
}

class RecordRulesTest {

    @Test
    fun `a rival who leaves before the battle is fought gives no win`() {
        assertFalse(OnlineBattleRules.countsForRecord(byLeaving = true, turns = 0))
        assertFalse(OnlineBattleRules.countsForRecord(byLeaving = true, turns = 1))
        assertFalse(OnlineBattleRules.countsForRecord(byLeaving = true, turns = OnlineBattleRules.MIN_TURNS_FOR_REWARD - 1))
    }

    @Test
    fun `leaving after a real fight still counts`() {
        assertTrue(OnlineBattleRules.countsForRecord(byLeaving = true, turns = OnlineBattleRules.MIN_TURNS_FOR_REWARD))
        assertTrue(OnlineBattleRules.countsForRecord(byLeaving = true, turns = 12))
    }

    @Test
    fun `a battle played to its end always counts, however short`() {
        assertTrue(OnlineBattleRules.countsForRecord(byLeaving = false, turns = 1))
        assertTrue(OnlineBattleRules.countsForRecord(byLeaving = false, turns = 0))
    }
}
