package com.aditya1875.pokeverse.feature.game.wildcatch

import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.ThrowAccuracy
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.WildCatchDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WildCatchDifficultyTest {

    @Test
    fun `timing windows only ever tighten as the run goes on`() {
        var previous = WildCatchDifficulty.ringThresholds(0)
        for (count in 1..40) {
            val now = WildCatchDifficulty.ringThresholds(count)
            assertTrue(now.excellent <= previous.excellent)
            assertTrue(now.great <= previous.great)
            assertTrue(now.good <= previous.good)
            previous = now
        }
    }

    @Test
    fun `windows stay nested so excellent is always the hardest`() {
        for (count in 0..40) {
            val t = WildCatchDifficulty.ringThresholds(count)
            assertTrue(t.excellent < t.great && t.great < t.good && t.good < 1f)
        }
    }

    @Test
    fun `the ramp is fully applied by the twentieth Pokémon`() {
        assertEquals(WildCatchDifficulty.ringThresholds(20), WildCatchDifficulty.ringThresholds(60))
        assertEquals(0.6f, WildCatchDifficulty.catchChanceMultiplier(20), 0.0001f)
    }

    @Test
    fun `a miss is almost never a catch, and a perfect throw is never a sure thing late on`() {
        assertTrue(ThrowAccuracy.MISS.baseCatchChance <= 0.05f)
        val latePerfectOnLegendary = ThrowAccuracy.EXCELLENT.baseCatchChance *
            WildCatchDifficulty.catchChanceMultiplier(30) * CatchTier.LEGENDARY.catchFactor
        assertTrue("was $latePerfectOnLegendary", latePerfectOnLegendary < 0.4f)
    }

    @Test
    fun `a clean throw early in the run is still very likely to land`() {
        val early = ThrowAccuracy.EXCELLENT.baseCatchChance *
            WildCatchDifficulty.catchChanceMultiplier(1) * CatchTier.COMMON.catchFactor
        assertTrue("was $early", early > 0.85f)
    }

    @Test
    fun `better throws always catch more often`() {
        val odds = listOf(ThrowAccuracy.MISS, ThrowAccuracy.GOOD, ThrowAccuracy.GREAT, ThrowAccuracy.EXCELLENT)
            .map { it.baseCatchChance }
        assertEquals(odds.sorted(), odds)
    }
}
