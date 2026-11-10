package com.aditya1875.pokeverse.feature.challenges

import com.aditya1875.pokeverse.feature.challenges.domain.BallReward
import com.aditya1875.pokeverse.feature.challenges.domain.ChallengeGoal
import com.aditya1875.pokeverse.feature.challenges.domain.ChallengeProgress
import com.aditya1875.pokeverse.feature.challenges.domain.ChallengeRules
import com.aditya1875.pokeverse.feature.challenges.domain.Difficulty
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeRulesTest {

    @Test
    fun `a day has one easy, one medium and one hard challenge, the same every time`() {
        val a = ChallengeRules.dailyFor("2026-11-11")
        assertEquals(listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD), a.map { it.difficulty })
        assertEquals(a, ChallengeRules.dailyFor("2026-11-11"))
        assertEquals(3, a.map { it.id }.toSet().size)
    }

    @Test
    fun `different days do not all give the same set`() {
        val sets = (1..28).map { d -> ChallengeRules.dailyFor("2026-11-%02d".format(d)).map { it.goal to it.target } }.toSet()
        assertTrue(sets.size > 5)
    }

    @Test
    fun `rewards grow with difficulty and are never empty`() {
        fun value(r: BallReward) = r.great * 20 + r.ultra * 60
        (1..60).forEach { d ->
            val (easy, medium, hard) = ChallengeRules.dailyFor("2026-11-%02d-%d".format(d % 28 + 1, d))
            assertTrue(!easy.reward.isEmpty && !medium.reward.isEmpty && !hard.reward.isEmpty)
            assertTrue("easy ${value(easy.reward)} < hard ${value(hard.reward)}", value(easy.reward) < value(hard.reward))
        }
    }

    @Test
    fun `a daily is hardly ever about legendaries, and no goal pays for one`() {
        val all = ChallengeRules.milestones + (1..60).flatMap { ChallengeRules.dailyFor("2026-12-%02d-%d".format(it % 28 + 1, it)) }
        assertTrue(all.none { it.minTier == CatchTier.LEGENDARY })
    }

    @Test
    fun `progress stops at the target and counts rarer catches towards a lower category`() {
        val c = ChallengeRules.milestones.first { it.id == "m:dex10" }
        assertEquals(10, ChallengeRules.progressOf(c, ChallengeProgress(dexCaught = 99)))
        val uncommonDaily = com.aditya1875.pokeverse.feature.challenges.domain.Challenge(
            "x", Difficulty.EASY, ChallengeGoal.CATCH_TIER, 3, BallReward(great = 1), CatchTier.UNCOMMON)
        val p = ChallengeProgress(tierCatches = mapOf(CatchTier.COMMON to 5, CatchTier.UNCOMMON to 1, CatchTier.EPIC to 1))
        assertEquals(2, ChallengeRules.progressOf(uncommonDaily, p))
        assertFalse(ChallengeRules.isComplete(uncommonDaily, p))
    }

    @Test
    fun `a collection goal counts only its own category`() {
        val c = ChallengeRules.milestones.first { it.id == "m:common20" }
        val p = ChallengeProgress(dexTiers = mapOf(CatchTier.COMMON to 7, CatchTier.RARE to 40))
        assertEquals(7, ChallengeRules.progressOf(c, p))
    }

    @Test
    fun `the all-done bonus opens only when every daily is claimed, and only once`() {
        val day = "2026-11-11"
        val ids = ChallengeRules.dailyFor(day).map { it.id }.toSet()
        assertFalse(ChallengeRules.sweepReady(day, ids.drop(1).toSet()))
        assertTrue(ChallengeRules.sweepReady(day, ids))
        assertFalse(ChallengeRules.sweepReady(day, ids + ChallengeRules.sweepId(day)))
    }

    @Test
    fun `the list shows the next goal of each track, not the whole ladder`() {
        val next = ChallengeRules.nextMilestones(emptySet(), ChallengeProgress())
        assertEquals(1, next.count { it.goal == ChallengeGoal.DEX_TOTAL })
        assertEquals("m:dex10", next.first { it.goal == ChallengeGoal.DEX_TOTAL }.id)
        val after = ChallengeRules.nextMilestones(setOf("m:dex10"), ChallengeProgress())
        assertEquals("m:dex25", after.first { it.goal == ChallengeGoal.DEX_TOTAL }.id)
    }

    @Test
    fun `old daily claims are forgotten but milestones never are`() {
        val claimed = setOf("m:dex10", "d:2026-01-01:0", "sweep:2026-01-01", "d:2026-11-10:1")
        val kept = ChallengeRules.prune(claimed, "2026-11-11")
        assertEquals(setOf("m:dex10", "d:2026-11-10:1"), kept)
    }

    @Test
    fun `date arithmetic crosses month and year ends`() {
        assertEquals("2026-12-31", ChallengeRules.daysBefore("2027-01-14", 14))
        assertEquals("2026-02-14", ChallengeRules.daysBefore("2026-02-28", 14))
    }

    @Test
    fun `legendary is known by id and the rest come from what was recorded`() {
        assertEquals(CatchTier.LEGENDARY, ChallengeRules.tierOfCaught(150, emptyMap()))
        assertEquals(CatchTier.RARE, ChallengeRules.tierOfCaught(1, mapOf(1 to CatchTier.RARE)))
        assertNull(ChallengeRules.tierOfCaught(1, emptyMap()))
        val counts = ChallengeRules.tierCounts(setOf(150, 1, 2, 3), mapOf(1 to CatchTier.COMMON, 2 to CatchTier.COMMON))
        assertEquals(mapOf(CatchTier.LEGENDARY to 1, CatchTier.COMMON to 2), counts)
    }
}
