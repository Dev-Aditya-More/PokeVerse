package com.aditya1875.pokeverse.feature.game

import com.aditya1875.pokeverse.feature.game.battle.data.SavedTeamsRepository
import com.aditya1875.pokeverse.feature.game.battle.domain.TeamFormat
import com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules
import com.aditya1875.pokeverse.feature.game.battle.domain.TeamSuggester
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineBattleRules
import com.aditya1875.pokeverse.utils.PokemonIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TeamFormatTest {

    // 150 Mewtwo, 151 Mew, 249 Lugia, 250 Ho-Oh are Legendary/Mythical; the rest are ordinary.
    private val legends = listOf(150, 151, 249, 250)
    private val others = listOf(25, 6, 9, 3, 1, 4, 7)

    @Test
    fun `each online format has its own size and legend limit`() {
        assertEquals(3, TeamFormat.QUICK_3.size)
        assertEquals(1, TeamFormat.QUICK_3.maxLegendaries)
        assertEquals(6, TeamFormat.QUICK_6.size)
        assertEquals(2, TeamFormat.QUICK_6.maxLegendaries)
        assertNull("Practice takes any size", TeamFormat.PRACTICE.size)
        assertEquals(TeamRules.MAX_LEGENDARIES, TeamFormat.PRACTICE.maxLegendaries)
        assertEquals(6, TeamFormat.PRACTICE.maxTeam)
    }

    @Test
    fun `a size finds its online format and nothing else does`() {
        assertEquals(TeamFormat.QUICK_3, TeamFormat.quick(3))
        assertEquals(TeamFormat.QUICK_6, TeamFormat.quick(6))
        for (size in listOf(0, 1, 2, 4, 5, 7)) assertNull(TeamFormat.quick(size))
    }

    // ── building a team in each format ──────────────────────────────────────────────

    @Test
    fun `a 3 vs 3 team holds three and one legend`() {
        val f = TeamFormat.QUICK_3
        assertTrue(TeamRules.canAdd(listOf(25, 6), 9, f))
        assertFalse("a fourth is too many", TeamRules.canAdd(listOf(25, 6, 9), 3, f))
        assertTrue(TeamRules.canAdd(listOf(25, 6), 150, f))
        assertFalse("a second legend is refused", TeamRules.canAdd(listOf(150, 25), 151, f))
    }

    @Test
    fun `a 6 vs 6 team holds six and two legends`() {
        val f = TeamFormat.QUICK_6
        assertTrue(TeamRules.canAdd(listOf(25, 6, 9, 3, 1), 4, f))
        assertFalse(TeamRules.canAdd(listOf(25, 6, 9, 3, 1, 4), 7, f))
        assertTrue(TeamRules.canAdd(listOf(150, 25), 151, f))
        assertFalse("a third legend is refused", TeamRules.canAdd(listOf(150, 151, 25), 249, f))
    }

    @Test
    fun `practice keeps the original rules`() {
        val f = TeamFormat.PRACTICE
        assertTrue(TeamRules.canAdd(listOf(150, 151), 249, f))
        assertFalse(TeamRules.canAdd(listOf(150, 151, 249), 250, f))
        assertEquals(TeamRules.trim(legends + others), TeamRules.trim(legends + others, f))
    }

    @Test
    fun `switching to a smaller format trims what is picked`() {
        val sixTeam = listOf(150, 151, 25, 6, 9, 3)
        assertEquals(listOf(150, 25, 6), TeamRules.trim(sixTeam, TeamFormat.QUICK_3))
        assertEquals(sixTeam, TeamRules.trim(sixTeam, TeamFormat.QUICK_6))
    }

    // ── what the online queue accepts ───────────────────────────────────────────────

    @Test
    fun `quick match accepts a legal 3 or 6 and refuses everything else`() {
        assertTrue(OnlineBattleRules.quickTeamOk(listOf(25, 6, 9)))
        assertTrue(OnlineBattleRules.quickTeamOk(listOf(150, 6, 9)))
        assertFalse("two legends in a 3 vs 3", OnlineBattleRules.quickTeamOk(listOf(150, 151, 9)))
        assertTrue(OnlineBattleRules.quickTeamOk(listOf(25, 6, 9, 3, 1, 4)))
        assertTrue("two legends are fine in a 6 vs 6", OnlineBattleRules.quickTeamOk(listOf(150, 151, 6, 9, 3, 1)))
        assertFalse("three legends in a 6 vs 6", OnlineBattleRules.quickTeamOk(listOf(150, 151, 249, 9, 3, 1)))
        for (size in listOf(1, 2, 4, 5)) assertFalse("size $size", OnlineBattleRules.quickTeamOk(others.take(size)))
        assertFalse(OnlineBattleRules.quickTeamOk(emptyList()))
    }

    @Test
    fun `an older client can still bring what the new caps forbid`() {
        // The caps are for your own team; the opponent's team is judged by the looser rule.
        assertTrue(OnlineBattleRules.validTeam(listOf(150, 151, 249), 1025))
        assertTrue(OnlineBattleRules.validTeam(listOf(150, 151, 249, 25, 6, 9), 1025))
    }

    // ── suggestions follow the format ───────────────────────────────────────────────

    @Test
    fun `suggested teams fit the format they were asked for`() {
        val index = (1..60).associateWith { PokemonIndex.Entry("p$it", listOf("fire")) } +
            legends.associateWith { PokemonIndex.Entry("l$it", listOf("psychic")) }
        val roster = index.keys
        repeat(100) { seed ->
            for (f in TeamFormat.entries) {
                val team = TeamSuggester.suggest(roster, index, f.suggestSize, f.suggestLegendaries, Random(seed))
                assertEquals("seed $seed ${f.name}", f.suggestSize, team.size)
                assertTrue(TeamRules.legendaryCount(team) <= f.maxLegendaries)
                // And what is suggested is something the format would accept.
                assertEquals(team, TeamRules.trim(team, f))
            }
        }
    }

    // ── saved teams per format ──────────────────────────────────────────────────────

    @Test
    fun `every format keeps its collection under its own key, and practice keeps the original`() {
        val keys = TeamFormat.entries.map { SavedTeamsRepository.keyOf(it) }
        assertEquals("one key per format", keys.size, keys.toSet().size)
        assertEquals("teams", SavedTeamsRepository.keyOf(TeamFormat.PRACTICE))
        assertNotEquals(SavedTeamsRepository.keyOf(TeamFormat.QUICK_3), SavedTeamsRepository.keyOf(TeamFormat.QUICK_6))
    }

    @Test
    fun `only complete legal teams carry over into an online collection`() {
        assertTrue(SavedTeamsRepository.fitsExactly(listOf(25, 6, 9), TeamFormat.QUICK_3))
        assertTrue(SavedTeamsRepository.fitsExactly(listOf(150, 6, 9), TeamFormat.QUICK_3))
        assertFalse("two legends do not fit a 3 vs 3", SavedTeamsRepository.fitsExactly(listOf(150, 151, 9), TeamFormat.QUICK_3))
        assertFalse("wrong size", SavedTeamsRepository.fitsExactly(listOf(25, 6, 9, 3), TeamFormat.QUICK_3))
        assertTrue(SavedTeamsRepository.fitsExactly(listOf(150, 151, 6, 9, 3, 1), TeamFormat.QUICK_6))
        assertFalse("three legends do not fit a 6 vs 6", SavedTeamsRepository.fitsExactly(listOf(150, 151, 249, 9, 3, 1), TeamFormat.QUICK_6))
        assertFalse("six is not a 3 vs 3", SavedTeamsRepository.fitsExactly(listOf(25, 6, 9, 3, 1, 4), TeamFormat.QUICK_3))
        assertFalse("practice never seeds from itself", SavedTeamsRepository.fitsExactly(listOf(25, 6, 9), TeamFormat.PRACTICE))
    }
}
