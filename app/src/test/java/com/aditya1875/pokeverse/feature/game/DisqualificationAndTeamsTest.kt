package com.aditya1875.pokeverse.feature.game

import com.aditya1875.pokeverse.feature.game.battle.data.SavedTeam
import com.aditya1875.pokeverse.feature.game.battle.data.SavedTeamsRepository
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEngine
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleMath
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleStats
import com.aditya1875.pokeverse.feature.game.battle.domain.Fighter
import com.aditya1875.pokeverse.feature.game.battle.domain.Who
import com.aditya1875.pokeverse.feature.game.battle.domain.online.ActionLog
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineAction
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineBattleRules
import com.aditya1875.pokeverse.feature.game.battle.domain.online.Seat
import com.aditya1875.pokeverse.feature.progression.domain.ShopItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisqualificationTest {

    private fun mon(name: String): Fighter {
        val s = BattleStats(BattleMath.hp(100), BattleMath.stat(10), BattleMath.stat(250), BattleMath.stat(10), BattleMath.stat(250), BattleMath.stat(100))
        return Fighter(name.hashCode(), name, listOf("normal"), s, "")
    }

    private fun start() = BattleEngine.start(listOf(mon("A1"), mon("A2")), listOf(mon("B1"), mon("B2")))

    private val attack = OnlineAction(OnlineAction.KIND_ATTACK)
    private val missed = OnlineAction(OnlineAction.KIND_ATTACK, auto = true)

    private fun step(host: OnlineAction, guest: OnlineAction) = mapOf(Seat.HOST to host, Seat.GUEST to guest)

    @Test
    fun `the limit is two missed turns`() {
        assertEquals(2, OnlineBattleRules.MAX_MISSED_TURNS)
    }

    @Test
    fun `one missed turn is forgiven, the second disqualifies`() {
        val misses = mutableMapOf<Seat, Int>()
        assertNull(OnlineBattleRules.countMisses(misses, step(missed, attack)))
        assertEquals(1, misses[Seat.HOST])
        assertEquals(Seat.HOST, OnlineBattleRules.countMisses(misses, step(missed, attack)))
    }

    @Test
    fun `turns that were not missed never count, and misses need not be in a row`() {
        val misses = mutableMapOf<Seat, Int>()
        assertNull(OnlineBattleRules.countMisses(misses, step(attack, attack)))
        assertNull(OnlineBattleRules.countMisses(misses, step(attack, missed)))
        assertNull(OnlineBattleRules.countMisses(misses, step(attack, attack)))
        assertNull(OnlineBattleRules.countMisses(misses, step(attack, attack)))
        // The second miss comes three turns later and still counts.
        assertEquals(Seat.GUEST, OnlineBattleRules.countMisses(misses, step(attack, missed)))
    }

    @Test
    fun `a double time-out on the same step disqualifies nobody`() {
        val misses = mutableMapOf(Seat.HOST to 1, Seat.GUEST to 1)
        assertNull(OnlineBattleRules.countMisses(misses, step(missed, missed)))
        // Both are put back one short of the limit, so the next miss by either still decides it.
        assertEquals(1, misses[Seat.HOST])
        assertEquals(1, misses[Seat.GUEST])
        assertEquals(Seat.GUEST, OnlineBattleRules.countMisses(misses, step(attack, missed)))
    }

    @Test
    fun `a replay stops at the disqualifying step and the other seat wins`() {
        val log: ActionLog = mapOf(
            1 to step(attack, attack),
            2 to step(missed, attack),
            3 to step(attack, attack),
            4 to step(missed, attack),   // the host's second missed turn
            5 to step(attack, attack),
        )
        val replay = OnlineBattleRules.replay(start(), 3L, log)
        assertEquals(Seat.HOST, replay.disqualified)
        assertEquals("steps 1-3 were played, step 4 was not", 4, replay.nextStep)
        assertFalse(replay.state.over)
        val outcome = OnlineBattleRules.outcome(replay.state, null, null, replay.disqualified)!!
        assertEquals(Seat.GUEST, outcome.winner)
        assertTrue(outcome.disqualified)
        assertTrue(outcome.byLeaving)
    }

    @Test
    fun `both phones agree on who was disqualified`() {
        val log: ActionLog = mapOf(
            1 to step(missed, attack),
            2 to step(attack, missed),
            3 to step(missed, attack),
        )
        val a = OnlineBattleRules.replay(start(), 8L, log)
        val b = OnlineBattleRules.replay(start(), 8L, log)
        assertEquals(a, b)
        assertEquals(Seat.HOST, a.disqualified)
    }

    @Test
    fun `a real result beats a later disqualification`() {
        val won = start().copy(winner = Who.PLAYER)
        val outcome = OnlineBattleRules.outcome(won, null, null, disqualified = Seat.HOST)!!
        assertEquals(Seat.HOST, outcome.winner)
        assertFalse(outcome.disqualified)
    }

    @Test
    fun `the clock's move is marked, a chosen move is not`() {
        val auto = OnlineBattleRules.defaultAction(start(), Seat.HOST).copy(auto = true)
        assertTrue(auto.auto)
        assertFalse(OnlineBattleRules.defaultAction(start(), Seat.HOST).auto)
        assertFalse(OnlineAction.of(com.aditya1875.pokeverse.feature.game.battle.domain.BattleAction.Attack).auto)
    }
}

class SavedTeamsStorageTest {

    @Test
    fun `a collection survives being written and read back`() {
        val teams = listOf(SavedTeam("Fire squad", listOf(6, 38, 59)), SavedTeam("Water", listOf(9)))
        assertEquals(teams, SavedTeamsRepository.decode(SavedTeamsRepository.encode(teams)))
    }

    @Test
    fun `names are cleaned so they cannot break the format`() {
        assertEquals("a b", SavedTeamsRepository.cleanName("a|b"))
        assertEquals("a b", SavedTeamsRepository.cleanName(" a\nb "))
        assertEquals(SavedTeamsRepository.MAX_NAME, SavedTeamsRepository.cleanName("x".repeat(80)).length)
        val tricky = listOf(SavedTeam(SavedTeamsRepository.cleanName("a|b\nc"), listOf(1, 2)))
        assertEquals(tricky, SavedTeamsRepository.decode(SavedTeamsRepository.encode(tricky)))
    }

    @Test
    fun `malformed storage is dropped instead of crashing`() {
        assertEquals(emptyList<SavedTeam>(), SavedTeamsRepository.decode(""))
        assertEquals(emptyList<SavedTeam>(), SavedTeamsRepository.decode("no separator\n|1,2\nname|\nname|x,y"))
        assertEquals(listOf(SavedTeam("ok", listOf(1, 2))), SavedTeamsRepository.decode("bad\nok|1,x,2,-5"))
    }

    @Test
    fun `a team never holds more than six or a duplicate`() {
        val read = SavedTeamsRepository.decode("big|1,1,2,3,4,5,6,7,8")
        assertEquals(listOf(1, 2, 3, 4, 5, 6), read.single().ids)
    }

    @Test
    fun `the collection is capped`() {
        val raw = (1..20).joinToString("\n") { "t$it|$it" }
        assertEquals(SavedTeamsRepository.MAX_TEAMS, SavedTeamsRepository.decode(raw).size)
    }
}

class MasterBallPriceTest {
    @Test
    fun `the Master Ball is a long-term goal at 5000 coins`() {
        assertEquals(5_000, ShopItems.MASTER_BALL_PRICE)
        assertEquals(5_000, ShopItems.find(ShopItems.ID_MASTER_BALL)!!.price)
        assertEquals(1, ShopItems.find(ShopItems.ID_MASTER_BALL)!!.maxHeld)
    }
}

class TeamRulesTest {
    // 150 Mewtwo, 151 Mew, 249 Lugia, 250 Ho-Oh are Legendary/Mythical; 25 Pikachu, 6 Charizard, 9 Blastoise are not.
    private val legends = listOf(150, 151, 249, 250)
    private val others = listOf(25, 6, 9, 3, 1)

    @Test
    fun `three legendaries fit and a fourth does not`() {
        val team = legends.take(3)
        assertTrue(com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.canAdd(team.take(2), 249))
        assertFalse(com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.canAdd(team, 250))
        assertTrue("an ordinary Pokemon is always fine", com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.canAdd(team, 25))
    }

    @Test
    fun `a team is capped at six and has no repeats`() {
        val full = listOf(25, 6, 9, 3, 1, 4)
        assertFalse(com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.canAdd(full, 7))
        assertFalse(com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.canAdd(listOf(25), 25))
    }

    @Test
    fun `trim keeps the order, drops the fourth legendary and repeats`() {
        val trimmed = com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.trim(listOf(150, 25, 151, 25, 249, 250, 6))
        assertEquals(listOf(150, 25, 151, 249, 6), trimmed)
    }

    @Test
    fun `quick fill gives a full legal team from the roster only`() {
        val roster = legends + others + listOf(2, 5, 7)
        repeat(50) { seed ->
            val team = com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.quickFill(roster, kotlin.random.Random(seed))
            assertEquals(6, team.size)
            assertTrue(team.all { it in roster })
            assertEquals(team.size, team.toSet().size)
            assertTrue(com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.legendaryCount(team) <= 3)
        }
    }

    @Test
    fun `quick fill of a small roster just takes what there is`() {
        val team = com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.quickFill(listOf(25, 6), kotlin.random.Random(1))
        assertEquals(setOf(25, 6), team.toSet())
    }

    @Test
    fun `an online team with four legendaries is refused`() {
        assertTrue(OnlineBattleRules.validTeam(legends.take(3) + 25, 1025))
        assertFalse(OnlineBattleRules.validTeam(legends, 1025))
    }

    @Test
    fun `a saved team never stores more than three legendaries`() {
        val saved = SavedTeamsRepository.decode("big|150,151,249,250,25")
        // Reading is lenient (storage), but trimming on load enforces the rule.
        assertEquals(listOf(150, 151, 249, 25), com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules.trim(saved.single().ids))
    }

    @Test
    fun `the collection holds twenty teams`() {
        assertEquals(20, SavedTeamsRepository.MAX_TEAMS)
    }
}
