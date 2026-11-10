package com.aditya1875.pokeverse.feature

import com.aditya1875.pokeverse.feature.game.chase.presentation.components.ChaseBiome
import com.aditya1875.pokeverse.feature.progression.data.ProgressionState
import com.aditya1875.pokeverse.feature.progression.domain.BagRules
import com.aditya1875.pokeverse.feature.progression.domain.ShopItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BagRulesTest {
    private val ball = ShopItems.find(ShopItems.ID_GREAT_BALL)!!
    private val stone = ShopItems.find("fire-stone")!!
    private val master = ShopItems.find(ShopItems.ID_MASTER_BALL)!!

    @Test
    fun `each upgrade multiplies what the bag holds`() {
        assertEquals(ball.maxHeld, BagRules.capacity(ball, 0))
        assertEquals(ball.maxHeld * 2, BagRules.capacity(ball, 1))
        assertEquals(ball.maxHeld * 4, BagRules.capacity(ball, BagRules.MAX_LEVEL))
        assertEquals("never above the top level", ball.maxHeld * 4, BagRules.capacity(ball, 99))
    }

    @Test
    fun `the Master Ball stays at one whatever the bag`() {
        (0..BagRules.MAX_LEVEL).forEach { assertEquals(1, BagRules.capacity(master, it)) }
    }

    @Test
    fun `a full bag always has a way out until the top level`() {
        // A pack that doesn't fit at level 0 fits after an upgrade.
        val level0Cap = BagRules.capacity(ball, 0)
        assertTrue(level0Cap - ball.quantity + ball.quantity <= BagRules.capacity(ball, 1))
        assertTrue(BagRules.upgradePrice(0) != null)
        assertNull(BagRules.upgradePrice(BagRules.MAX_LEVEL))
    }

    @Test
    fun `upgrades get dearer and one purchase of anything still fits an upgraded bag`() {
        assertEquals(BagRules.UPGRADE_PRICES.sorted(), BagRules.UPGRADE_PRICES)
        assertEquals(BagRules.MAX_LEVEL, BagRules.UPGRADE_PRICES.size)
        ShopItems.all.filter { it.isInventory }.forEach { assertTrue(it.quantity <= BagRules.capacity(it, 0)) }
        assertTrue(stone.maxHeld < BagRules.capacity(stone, 1))
    }

    @Test
    fun `the state reads capacity and holdings from the bag level`() {
        val s = ProgressionState(bagLevel = 2, itemsBought = mapOf(ShopItems.ID_GREAT_BALL to 14), itemsUsed = mapOf(ShopItems.ID_GREAT_BALL to 4))
        assertEquals(10, s.held(ShopItems.ID_GREAT_BALL))
        assertEquals(ball.maxHeld * 3, s.capacityOf(ball))
    }
}

class ChasePlacesTest {
    private val places = ChaseBiome.entries

    @Test
    fun `there are several places, in order, each starting after the last`() {
        assertTrue(places.size >= 6)
        assertEquals(0f, places.first().startMeters, 0f)
        places.zipWithNext().forEach { (a, b) -> assertTrue(b.startMeters > a.startMeters) }
        assertTrue(ChaseBiome.CYCLE_METERS > places.last().startMeters)
    }

    @Test
    fun `places are different lengths so the changes never fall on a beat`() {
        val lengths = places.mapIndexed { i, p ->
            (places.getOrNull(i + 1)?.startMeters ?: ChaseBiome.CYCLE_METERS) - p.startMeters
        }
        assertTrue("lengths $lengths", lengths.toSet().size >= lengths.size - 1)
        assertTrue(lengths.all { it >= 300f })
    }

    @Test
    fun `the place follows the distance and the run loops`() {
        assertEquals(ChaseBiome.MEADOW, ChaseBiome.at(10f))
        assertEquals(ChaseBiome.COAST, ChaseBiome.at(460f))
        assertEquals(ChaseBiome.HIDEOUT, ChaseBiome.at(4000f))
        assertEquals(ChaseBiome.MEADOW, ChaseBiome.at(ChaseBiome.CYCLE_METERS + 10f))
    }

    @Test
    fun `the colours blend smoothly into the next place instead of jumping`() {
        places.forEach { p ->
            val end = (places.getOrNull(p.ordinal + 1)?.startMeters ?: ChaseBiome.CYCLE_METERS)
            val before = ChaseBiome.paletteAt(end - 1f)
            val after = ChaseBiome.paletteAt(if (end >= ChaseBiome.CYCLE_METERS) 0f else end)
            // One metre either side of the border: the sky colour barely moves.
            val d = kotlin.math.abs(before.haze.red - after.haze.red) + kotlin.math.abs(before.haze.green - after.haze.green)
            assertTrue("jump at $end ($p): $d", d < 0.2f)
        }
    }
}
