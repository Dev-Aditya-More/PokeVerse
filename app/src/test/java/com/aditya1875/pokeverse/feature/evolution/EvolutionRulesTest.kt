package com.aditya1875.pokeverse.feature.evolution

import com.aditya1875.pokeverse.feature.evolution.domain.EvolutionEconomy
import com.aditya1875.pokeverse.feature.evolution.domain.EvolutionPaths
import com.aditya1875.pokeverse.feature.evolution.domain.EvolutionRules
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.ThrowAccuracy
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.Chain
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.EvolutionNode
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.EvolvesTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvolutionRulesTest {

    private fun node(name: String, vararg next: EvolvesTo) =
        EvolvesTo(emptyList(), next.toList(), false, EvolutionNode(name, null))

    private fun chain(name: String, vararg next: EvolvesTo) =
        Chain(Any(), next.toList(), false, EvolutionNode(name, null))

    // Pichu -> Pikachu -> Raichu
    private val pikachuLine = chain("pichu", node("pikachu", node("raichu")))

    // Eevee branches into several
    private val eeveeLine = chain("eevee", node("vaporeon"), node("jolteon"), node("flareon"))

    @Test
    fun `a first stage evolves into the second, a middle stage into the third`() {
        assertEquals(listOf("pikachu" to 1), EvolutionPaths.next(pikachuLine, "pichu"))
        assertEquals(listOf("raichu" to 2), EvolutionPaths.next(pikachuLine, "pikachu"))
    }

    @Test
    fun `a final form has nothing to evolve into`() {
        assertTrue(EvolutionPaths.next(pikachuLine, "raichu").isEmpty())
    }

    @Test
    fun `a branching line offers every branch`() {
        assertEquals(
            listOf("vaporeon" to 1, "jolteon" to 1, "flareon" to 1),
            EvolutionPaths.next(eeveeLine, "eevee")
        )
    }

    @Test
    fun `an unknown species yields no options instead of crashing`() {
        assertTrue(EvolutionPaths.next(pikachuLine, "mew").isEmpty())
    }

    @Test
    fun `later stages cost more`() {
        assertTrue(EvolutionEconomy.cost(1) < EvolutionEconomy.cost(2))
        assertTrue(EvolutionEconomy.cost(2) <= EvolutionEconomy.cost(3))
    }

    @Test
    fun `a clean throw on a hard Pokémon earns more than a sloppy one on an easy one`() {
        val sloppy = EvolutionEconomy.forCatch(ThrowAccuracy.GOOD, newEntry = false, tier = CatchTier.COMMON)
        val clean = EvolutionEconomy.forCatch(ThrowAccuracy.EXCELLENT, newEntry = false, tier = CatchTier.EPIC)
        assertTrue(clean > sloppy)
        assertTrue("every catch pays something", sloppy > 0)
    }

    @Test
    fun `meeting a Pokémon again pays the most`() {
        val repeat = EvolutionEconomy.forCatch(ThrowAccuracy.GOOD, false, CatchTier.COMMON)
        val fresh = EvolutionEconomy.forCatch(ThrowAccuracy.GOOD, true, CatchTier.COMMON)
        assertEquals(5, repeat)
        assertEquals(3, fresh)
        assertTrue(repeat > fresh)
    }

    @Test
    fun `a first evolution is reachable within a session of catching`() {
        // Ten ordinary catches (great throws, half of them new) should already afford a first evolution.
        val session = (0 until 10).sumOf { EvolutionEconomy.forCatch(ThrowAccuracy.GREAT, it % 2 == 0, CatchTier.COMMON) }
        assertTrue("session paid $session", session >= EvolutionEconomy.cost(1))
    }

    @Test
    fun `a battle pays more for a win than a loss, and a loss still pays`() {
        assertTrue(EvolutionEconomy.forBattle(true) > EvolutionEconomy.forBattle(false))
        assertTrue(EvolutionEconomy.forBattle(false) > 0)
    }

    @Test
    fun `evolving needs the Pokémon, a form you do not have yet, and enough shards`() {
        val ok = EvolutionRules.Check.OK
        assertEquals(ok, EvolutionRules.check(balance = 30, cost = 25, fromCaught = true, toCaught = false))
        assertEquals(EvolutionRules.Check.NOT_CAUGHT, EvolutionRules.check(100, 25, fromCaught = false, toCaught = false))
        assertEquals(EvolutionRules.Check.ALREADY_HAVE, EvolutionRules.check(100, 25, fromCaught = true, toCaught = true))
        assertEquals(EvolutionRules.Check.NOT_ENOUGH, EvolutionRules.check(24, 25, fromCaught = true, toCaught = false))
    }

    @Test
    fun `the daily cap trims a reward that would pass it and stops at zero`() {
        assertEquals(10, EvolutionEconomy.withinCap(earnedToday = 0, amount = 10))
        assertEquals(5, EvolutionEconomy.withinCap(earnedToday = EvolutionEconomy.DAILY_CAP - 5, amount = 10))
        assertEquals(0, EvolutionEconomy.withinCap(earnedToday = EvolutionEconomy.DAILY_CAP, amount = 10))
        assertEquals(0, EvolutionEconomy.withinCap(earnedToday = EvolutionEconomy.DAILY_CAP + 50, amount = 10))
    }

    @Test
    fun `a full day of capped earning still pays for a first evolution`() {
        assertTrue(EvolutionEconomy.DAILY_CAP >= EvolutionEconomy.cost(1))
    }

    @Test
    fun `exactly enough shards is enough`() {
        assertEquals(EvolutionRules.Check.OK, EvolutionRules.check(25, 25, true, false))
    }
}
