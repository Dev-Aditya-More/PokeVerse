package com.aditya1875.pokeverse.feature.leaderboard

import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchSpawns
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEconomy
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The XP model, as numbers: a typical session lands in one band whatever the game, endless games cannot be
 * farmed, a real opponent beats the bot, a rarer catch is worth more, and showing up is worth less than playing.
 */
class XPBalanceTest {

    private val typicalMin = 25
    private val typicalMax = 90

    @Test
    fun `a typical session of every game is worth about the same`() {
        val sessions = mapOf(
            "quiz 7/10" to 7 * XPValues.QUIZ_CORRECT + XPValues.QUIZ_COMPLETE,
            "quiz perfect" to 10 * XPValues.QUIZ_CORRECT + XPValues.QUIZ_COMPLETE + XPValues.QUIZ_PERFECT,
            "match" to XPValues.MATCH_COMPLETE,
            "match under par" to XPValues.MATCH_COMPLETE + XPValues.MATCH_UNDER_PAR,
            "guess 10 right" to 10 * XPValues.GUESS_CORRECT + 4 * XPValues.GUESS_STREAK_3 + XPValues.GUESS_COMPLETE,
            "duel 10 right" to 10 * XPValues.DUEL_CORRECT + 4 * XPValues.GUESS_STREAK_3 + XPValues.DUEL_COMPLETE,
            "survivor 12 right" to 12 * XPValues.SURVIVOR_CORRECT + XPValues.SURVIVOR_COMPLETE,
            "rush 12 right" to 12 * XPValues.RUSH_CORRECT + XPValues.RUSH_COMPLETE,
            "wild catch 10 catches" to 10 * XPValues.CATCH_CAUGHT + 4 * XPValues.CATCH_STREAK_3 + XPValues.CATCH_COMPLETE,
            "chase 1000 m" to XPValues.CHASE_COMPLETE + (1000 / 100 * XPValues.CHASE_PER_100M).coerceAtMost(XPValues.CHASE_DISTANCE_CAP),
            "battle win (trainer)" to XPValues.BATTLE_WIN + 10,
            "online win" to XPValues.ONLINE_WIN,
            "clash win, 4 rounds" to XPValues.CLASH_WIN + 4 * XPValues.CLASH_ROUND_WIN,
        )
        sessions.forEach { (name, xp) -> assertTrue("$name = $xp", xp in typicalMin..typicalMax) }
    }

    @Test
    fun `endless games cannot be farmed, a very long run is capped`() {
        val cap = XPValues.ENDLESS_PAID_CAP
        val longestGuess = cap * (XPValues.GUESS_CORRECT + XPValues.GUESS_STREAK_6) + XPValues.GUESS_COMPLETE
        val longestSurvivor = cap * (XPValues.SURVIVOR_CORRECT + XPValues.SURVIVOR_STREAK_10) + XPValues.SURVIVOR_COMPLETE
        // What rarity adds on average over a run, from the real spawn odds (a lucky run can only be a little above).
        val avgRarity = Math.ceil(CatchSpawns.WEIGHTS.entries.sumOf { (tier, w) -> w * tier.xpBonus } / 100.0).toInt()
        val longestCatch = cap * (XPValues.CATCH_CAUGHT + XPValues.CATCH_STREAK_6 + avgRarity) + XPValues.CATCH_COMPLETE
        val longestRush = cap * XPValues.RUSH_CORRECT + XPValues.RUSH_COMPLETE + XPValues.RUSH_PERFECT
        listOf(longestGuess, longestSurvivor, longestCatch, longestRush).forEach { assertTrue("$it", it <= 135) }
    }

    @Test
    fun `chase is capped by distance and a short stumble pays nothing`() {
        assertTrue(XPValues.CHASE_COMPLETE + XPValues.CHASE_DISTANCE_CAP <= 100)
        assertTrue(XPValues.CHASE_MIN_METERS >= 100)
    }

    @Test
    fun `a real opponent pays double the bot`() {
        assertEquals(XPValues.CLASH_BOT_WIN * 2, XPValues.CLASH_WIN)
        assertEquals(XPValues.CLASH_BOT_ROUND_WIN * 2, XPValues.CLASH_ROUND_WIN)
        assertTrue(XPValues.CLASH_BOT_PERFECT < XPValues.CLASH_PERFECT)
        assertTrue(XPValues.CLASH_BOT_DRAW < XPValues.CLASH_DRAW)
        val botSweep = XPValues.CLASH_BOT_WIN + 6 * XPValues.CLASH_BOT_ROUND_WIN + XPValues.CLASH_BOT_PERFECT
        val humanPlainWin = XPValues.CLASH_WIN + 4 * XPValues.CLASH_ROUND_WIN
        assertTrue("a bot sweep ($botSweep) must not beat an ordinary human win ($humanPlainWin)", botSweep < humanPlainWin)
        assertTrue(XPValues.ONLINE_WIN > XPValues.BATTLE_WIN)
        assertTrue(XPValues.ONLINE_LOSS > XPValues.BATTLE_LOSS)
    }

    @Test
    fun `a rarer catch is worth more, and a legendary is worth a lot`() {
        val bonuses = CatchTier.entries.map { it.xpBonus }
        assertEquals(bonuses.sorted(), bonuses)
        assertEquals(bonuses.size, bonuses.toSet().size)
        assertEquals(0, CatchTier.COMMON.xpBonus)
        assertTrue(CatchTier.LEGENDARY.xpBonus >= 3 * CatchTier.RARE.xpBonus)
    }

    @Test
    fun `showing up is worth less than playing`() {
        val bestShowUp = XPValues.DAILY_LOGIN + XPValues.DAILY_STREAK_CAP
        val goodSession = 45
        assertTrue(bestShowUp < goodSession * 2)
        assertTrue(XPValues.FIRST_GAME_OF_DAY < goodSession)
        assertTrue(XPValues.FIRST_EXPLORATION_OF_DAY < XPValues.FIRST_GAME_OF_DAY)
        assertTrue("the streak bonus stops growing", 60 * XPValues.DAILY_STREAK_BONUS > XPValues.DAILY_STREAK_CAP)
    }

    @Test
    fun `community bonuses stay off the weekly leaderboard, playing counts`() {
        assertFalse(XPEvent.ReferralWelcome.countsForWeekly)
        assertFalse(XPEvent.ReferralReward(3).countsForWeekly)
        assertFalse(XPEvent.FeedbackThanks.countsForWeekly)
        assertTrue(XPEvent.DailyLogin.countsForWeekly)
        assertTrue(XPEvent.FirstGameOfDay.countsForWeekly)
        assertTrue(XPEvent.WildCatchCaught(1).countsForWeekly)
        assertTrue(XPEvent.OnlineBattleComplete(true).countsForWeekly)
    }

    @Test
    fun `the trickle rate is exact over a long day`() {
        // 1000 events of 4 raw each, all in the trickle tier, must add up to 800 XP, not 1000 (the old round-up-to-1).
        var earned = XPEconomy.HALF_RATE_RAW
        var total = 0
        repeat(1000) { total += XPEconomy.award(4, earned, 0).total; earned += 4 }
        assertEquals((1000 * 4 * XPEconomy.TRICKLE_RATE).toInt(), total)
    }

    @Test
    fun `cumulative xp only ever grows and never pays more than raw`() {
        var last = 0.0
        for (raw in 0..3000 step 7) {
            val now = XPEconomy.cumulative(raw)
            assertTrue(now >= last)
            assertTrue(now <= raw + 1e-9)
            last = now
        }
        assertEquals(XPEconomy.FULL_RATE_RAW.toDouble(), XPEconomy.cumulative(XPEconomy.FULL_RATE_RAW), 1e-9)
    }
}

class LegendaryRarityTest {
    private fun legendaryShare(allowed: Boolean, n: Int = 50_000): Double {
        val random = Random(11)
        return (1..n).count { CatchSpawns.rollTier(20, random, allowed) == CatchTier.LEGENDARY } * 100.0 / n
    }

    @Test
    fun `a legendary is about one encounter in a hundred`() {
        assertEquals(1, CatchSpawns.WEIGHTS.getValue(CatchTier.LEGENDARY))
        assertEquals(1.0, legendaryShare(true), 0.35)
    }

    @Test
    fun `during the cooldown a legendary cannot come up by chance`() {
        assertEquals(0.0, legendaryShare(false), 0.0)
    }

    @Test
    fun `every rarer category is rarer than the one below it`() {
        val w = CatchSpawns.WEIGHTS
        CatchTier.entries.zipWithNext().forEach { (a, b) -> assertTrue(w.getValue(a) > w.getValue(b)) }
        assertEquals(100, w.values.sum())
    }

    @Test
    fun `a new run starts partway through the cooldown, so the first stretch is legendary free`() {
        assertTrue(CatchSpawns.LEGENDARY_START_HEAD < CatchSpawns.LEGENDARY_COOLDOWN)
        val encountersUntilFirstPossible = CatchSpawns.LEGENDARY_COOLDOWN - (CatchSpawns.LEGENDARY_COOLDOWN - CatchSpawns.LEGENDARY_START_HEAD)
        assertTrue(encountersUntilFirstPossible >= 10)
    }
}
