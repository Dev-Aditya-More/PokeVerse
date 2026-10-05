package com.aditya1875.pokeverse.feature.leaderboard

import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEconomy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XPEconomyTest {

    @Test
    fun `early in the day xp pays in full`() {
        val award = XPEconomy.award(raw = 40, rawEarnedToday = 0, restedPool = 0)
        assertEquals(40, award.total)
        assertEquals(1f, award.rate)
    }

    @Test
    fun `an award straddling the full-rate cap is split across tiers`() {
        // 20 XP at full rate + 20 XP at half rate.
        val award = XPEconomy.award(raw = 40, rawEarnedToday = XPEconomy.FULL_RATE_RAW - 20, restedPool = 0)
        assertEquals(30, award.total)
        assertEquals(0.5f, award.rate)
    }

    @Test
    fun `marathon sessions drop to the trickle rate but never to zero`() {
        val big = XPEconomy.award(raw = 50, rawEarnedToday = 5_000, restedPool = 0)
        assertEquals(10, big.total)
        val tiny = XPEconomy.award(raw = 2, rawEarnedToday = 5_000, restedPool = 0)
        assertEquals(1, tiny.total)
    }

    @Test
    fun `a full marathon day earns far less than raw`() {
        var earnedToday = 0
        var total = 0
        repeat(100) { // 100 sessions × 40 raw = 4,000 raw XP in one day
            total += XPEconomy.award(40, earnedToday, 0).total
            earnedToday += 40
        }
        assertTrue("got $total", total in 1_200..1_400)
    }

    @Test
    fun `rested xp doubles awards until the pool runs out`() {
        val award = XPEconomy.award(raw = 40, rawEarnedToday = 0, restedPool = 25)
        assertEquals(40, award.base)
        assertEquals(25, award.restedBonus)
        assertEquals(65, award.total)
    }

    @Test
    fun `rested pool only builds after missing at least a day, and is capped`() {
        assertEquals(0, XPEconomy.restedGain(daysAway = 0, currentPool = 0))
        assertEquals(0, XPEconomy.restedGain(daysAway = 1, currentPool = 0))
        assertEquals(XPEconomy.RESTED_PER_DAY_AWAY, XPEconomy.restedGain(daysAway = 2, currentPool = 0))
        assertEquals(XPEconomy.RESTED_MAX, XPEconomy.restedGain(daysAway = 30, currentPool = 0))
        assertEquals(50, XPEconomy.restedGain(daysAway = 30, currentPool = XPEconomy.RESTED_MAX - 50))
        assertEquals(0, XPEconomy.restedGain(daysAway = 30, currentPool = XPEconomy.RESTED_MAX))
    }

    @Test
    fun `zero raw xp awards nothing`() {
        assertEquals(0, XPEconomy.award(0, 0, 100).total)
    }
}
