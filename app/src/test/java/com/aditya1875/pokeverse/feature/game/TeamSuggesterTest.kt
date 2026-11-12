package com.aditya1875.pokeverse.feature.game

import com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules
import com.aditya1875.pokeverse.feature.game.battle.domain.TeamSuggester
import com.aditya1875.pokeverse.utils.PokemonIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TeamSuggesterTest {

    private fun e(vararg types: String) = PokemonIndex.Entry("x", types.toList())

    // 150 Mewtwo, 151 Mew, 249 Lugia, 250 Ho-Oh are Legendary/Mythical; the rest are ordinary.
    private val index = mapOf(
        4 to e("fire"), 5 to e("fire"), 6 to e("fire", "flying"),
        7 to e("water"), 9 to e("water"),
        1 to e("grass", "poison"), 3 to e("grass", "poison"),
        25 to e("electric"), 26 to e("electric"),
        74 to e("rock", "ground"),
        150 to e("psychic"), 151 to e("psychic"), 249 to e("psychic", "flying"), 250 to e("fire", "flying"),
    )
    private val roster = index.keys.toList()

    private fun typesOf(team: List<Int>) = team.flatMap { index[it]!!.types }

    @Test
    fun `gives exactly the size asked for from the roster, with no repeats`() {
        repeat(100) { seed ->
            val team = TeamSuggester.suggest(roster, index, 3, 1, Random(seed))
            assertEquals(3, team.size)
            assertEquals(team.size, team.toSet().size)
            assertTrue(team.all { it in roster })
        }
    }

    @Test
    fun `never exceeds the legendary cap`() {
        repeat(200) { seed ->
            val team = TeamSuggester.suggest(roster, index, 3, 1, Random(seed))
            assertTrue("seed $seed: $team", TeamRules.legendaryCount(team) <= 1)
            val six = TeamSuggester.suggest(roster, index, 6, 2, Random(seed))
            assertTrue("seed $seed: $six", TeamRules.legendaryCount(six) <= 2)
        }
    }

    @Test
    fun `a cap of zero means no legends at all`() {
        repeat(50) { seed ->
            assertEquals(0, TeamRules.legendaryCount(TeamSuggester.suggest(roster, index, 3, 0, Random(seed))))
        }
    }

    @Test
    fun `prefers a different type for each place when the roster allows`() {
        repeat(100) { seed ->
            val team = TeamSuggester.suggest(roster, index, 3, 1, Random(seed))
            val types = typesOf(team)
            assertEquals("seed $seed: $team has a repeated type", types.size, types.toSet().size)
        }
    }

    @Test
    fun `a roster of one type still fills the team`() {
        val allFire = mapOf(4 to e("fire"), 5 to e("fire"), 6 to e("fire"), 37 to e("fire"))
        val team = TeamSuggester.suggest(allFire.keys, allFire, 3, 1, Random(1))
        assertEquals(3, team.size)
    }

    @Test
    fun `a small roster gives what there is, and an empty one gives nothing`() {
        assertEquals(setOf(25, 4), TeamSuggester.suggest(listOf(25, 4), index, 3, 1, Random(1)).toSet())
        assertEquals(emptyList<Int>(), TeamSuggester.suggest(emptyList(), index, 3, 1, Random(1)))
        assertEquals(emptyList<Int>(), TeamSuggester.suggest(roster, index, 0, 1, Random(1)))
    }

    @Test
    fun `a roster of only legends still gives a legal team`() {
        val team = TeamSuggester.suggest(listOf(150, 151, 249, 250), index, 3, 1, Random(2))
        assertEquals("only one legend is allowed, so only one is taken", 1, team.size)
    }

    @Test
    fun `unknown Pokemon are still usable, they just do not count as a new type`() {
        val team = TeamSuggester.suggest(listOf(9001, 9002, 9003), emptyMap(), 3, 1, Random(3))
        assertEquals(3, team.size)
    }

    @Test
    fun `it can vary - different seeds give different teams`() {
        val teams = (0..40).map { TeamSuggester.suggest(roster, index, 3, 1, Random(it)).toSet() }.toSet()
        assertTrue("expected variety, got ${teams.size} distinct teams", teams.size > 5)
    }
}
