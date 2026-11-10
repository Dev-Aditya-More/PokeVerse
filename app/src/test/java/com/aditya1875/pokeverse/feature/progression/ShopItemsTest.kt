package com.aditya1875.pokeverse.feature.progression

import com.aditya1875.pokeverse.feature.evolution.domain.EvolutionPaths
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchOdds
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.ThrowAccuracy
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.Chain
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.EvolutionDetail
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.EvolutionNode
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.EvolvesTo
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.ItemRef
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.Trigger
import com.aditya1875.pokeverse.feature.pokemon.dex.DexProgress
import com.aditya1875.pokeverse.feature.progression.domain.ItemKind
import com.aditya1875.pokeverse.feature.progression.domain.ShopItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopItemsTest {

    // ── Catalogue ────────────────────────────────────────────────────────────

    @Test
    fun `every item id is unique and every price is positive`() {
        val ids = ShopItems.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ShopItems.all.all { it.price > 0 })
    }

    @Test
    fun `one purchase always fits in the bag`() {
        // Otherwise the item could never be bought at all.
        ShopItems.all.filter { it.isInventory }.forEach {
            assertTrue("${it.id} pack ${it.quantity} > max ${it.maxHeld}", it.quantity <= it.maxHeld)
        }
    }

    @Test
    fun `a shard pouch grants shards instead of an inventory item`() {
        val pouch = ShopItems.find(ShopItems.ID_SHARD_POUCH)!!
        assertFalse(pouch.isInventory)
        assertTrue(pouch.shards > 0)
    }

    @Test
    fun `only stones and held items satisfy an evolution`() {
        assertEquals("water-stone", ShopItems.forEvolution("water-stone")?.id)
        assertEquals(ItemKind.EVO_ITEM, ShopItems.forEvolution("metal-coat")?.kind)
        assertNull("a ball is not an evolution item", ShopItems.forEvolution(ShopItems.ID_GREAT_BALL))
        assertNull(ShopItems.forEvolution("oval-stone"))
        assertNull(ShopItems.forEvolution(null))
    }

    // ── Evolution requirements ───────────────────────────────────────────────

    private fun evolvesTo(name: String, vararg details: EvolutionDetail) =
        EvolvesTo(details.toList(), emptyList(), false, EvolutionNode(name, null))

    private fun stone(item: String) = EvolutionDetail(trigger = Trigger("use-item", ""), item = ItemRef(item))
    private fun levelUp() = EvolutionDetail(trigger = Trigger("level-up", ""))

    private val eevee = Chain(
        Any(),
        listOf(
            evolvesTo("vaporeon", stone("water-stone")),
            evolvesTo("umbreon", levelUp()),
            evolvesTo("glaceon", levelUp(), stone("ice-stone")),
        ),
        false,
        EvolutionNode("eevee", null),
    )

    @Test
    fun `a stone evolution asks for that stone and a friendship one for none`() {
        val steps = EvolutionPaths.steps(eevee, "eevee").associate { it.name to it.itemId }
        assertEquals("water-stone", steps["vaporeon"])
        assertNull("friendship evolutions are paid in shards", steps["umbreon"])
        assertEquals("ice-stone", steps["glaceon"])
    }

    @Test
    fun `a trade evolution with a held item asks for the item`() {
        val onix = Chain(
            Any(),
            listOf(
                evolvesTo(
                    "steelix",
                    EvolutionDetail(trigger = Trigger("trade", ""), held_item = ItemRef("metal-coat"))
                )
            ),
            false,
            EvolutionNode("onix", null),
        )
        assertEquals("metal-coat", EvolutionPaths.steps(onix, "onix").single().itemId)
    }

    @Test
    fun `an item the shop does not sell falls back to shards`() {
        val chain = Chain(
            Any(),
            listOf(evolvesTo("x", stone("some-unsold-item"))),
            false,
            EvolutionNode("w", null),
        )
        assertNull(EvolutionPaths.steps(chain, "w").single().itemId)
    }

    // ── Roster: evolving uses the old form up ────────────────────────────────

    @Test
    fun `an evolved-away form stays in the Pokedex but leaves the roster`() {
        val dex = DexProgress(caught = setOf(133, 134), consumed = setOf(133))
        // Caught means what you have now: Eevee (133) became Vaporeon (134), so only Vaporeon shows.
        assertEquals(1, dex.caughtCount)
        assertEquals(setOf(134), dex.roster)
        // The Pokedex record itself still holds both.
        assertEquals(setOf(133, 134), dex.caught)
    }

    // ── Catch odds ───────────────────────────────────────────────────────────

    private fun odds(ball: BallType, tier: CatchTier = CatchTier.LEGENDARY, acc: ThrowAccuracy = ThrowAccuracy.GREAT, n: Int = 15) =
        CatchOdds.chance(acc, n, tier, ball)

    @Test
    fun `a plain Poke Ball keeps the original odds`() {
        // base * difficulty * tier, exactly as before balls existed.
        val expected = 0.62f * com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.WildCatchDifficulty
            .catchChanceMultiplier(15) * CatchTier.RARE.catchFactor
        assertEquals(expected, odds(BallType.POKE, CatchTier.RARE), 1e-6f)
    }

    @Test
    fun `better balls always help and the Master Ball never fails`() {
        assertTrue(odds(BallType.GREAT) > odds(BallType.POKE))
        assertTrue(odds(BallType.ULTRA) > odds(BallType.GREAT))
        assertEquals(1f, odds(BallType.MASTER, acc = ThrowAccuracy.MISS), 0f)
    }

    @Test
    fun `balls help legendaries more than commons relative to the penalty they carry`() {
        // The tougher the tier, the more an Ultra Ball closes the gap to a common.
        val gapPoke = odds(BallType.POKE, CatchTier.COMMON) - odds(BallType.POKE, CatchTier.LEGENDARY)
        val gapUltra = odds(BallType.ULTRA, CatchTier.COMMON, n = 15).coerceAtMost(CatchOdds.MAX_CHANCE) -
            odds(BallType.ULTRA, CatchTier.LEGENDARY)
        assertTrue(gapUltra < gapPoke + 0.35f)
        assertTrue(odds(BallType.ULTRA, CatchTier.LEGENDARY) > odds(BallType.POKE, CatchTier.LEGENDARY) * 1.9f)
    }

    @Test
    fun `only a Master Ball can be a sure thing`() {
        ThrowAccuracy.entries.forEach { acc ->
            listOf(BallType.POKE, BallType.GREAT, BallType.ULTRA).forEach { ball ->
                assertTrue(CatchOdds.chance(acc, 0, CatchTier.COMMON, ball) <= CatchOdds.MAX_CHANCE)
            }
        }
        assertNotNull(BallType.MASTER.itemId)
    }

    @Test
    fun `a missed ring stays a long shot whatever the ball`() {
        assertTrue(odds(BallType.ULTRA, CatchTier.COMMON, ThrowAccuracy.MISS, n = 0) < 0.10f)
    }
}
