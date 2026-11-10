package com.aditya1875.pokeverse.feature.progression

import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.progression.domain.CoinEconomy
import com.aditya1875.pokeverse.feature.progression.domain.CosmeticKind
import com.aditya1875.pokeverse.feature.progression.domain.Cosmetics
import com.aditya1875.pokeverse.feature.progression.domain.LevelRewards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinEconomyTest {

    @Test
    fun `finishing a game pays a small capped reward`() {
        val first = CoinEconomy.forEvent(XPEvent.GuessComplete(5), completionsToday = 0)
        assertEquals(CoinEconomy.COMPLETION, first.coins)
        assertTrue(first.countsAsCompletion)

        val capped = CoinEconomy.forEvent(XPEvent.GuessComplete(5), completionsToday = CoinEconomy.COMPLETIONS_PER_DAY)
        assertEquals(0, capped.coins)
        assertFalse(capped.countsAsCompletion)
    }

    @Test
    fun `per-answer events never pay coins so long sessions can't farm them`() {
        assertEquals(0, CoinEconomy.forEvent(XPEvent.GuessCorrect(streak = 9), 0).coins)
        assertEquals(0, CoinEconomy.forEvent(XPEvent.QuizAnswer(correct = true), 0).coins)
        assertEquals(0, CoinEconomy.forEvent(XPEvent.RushCorrect, 0).coins)
        assertEquals(0, CoinEconomy.forEvent(XPEvent.CardClashRoundWin(), 0).coins)
    }

    @Test
    fun `community bonuses pay no coins`() {
        assertEquals(0, CoinEconomy.forEvent(XPEvent.ReferralReward(friends = 10), 0).coins)
        assertEquals(0, CoinEconomy.forEvent(XPEvent.FeedbackThanks, 0).coins)
    }

    @Test
    fun `dex daily pays more for a solve than a miss`() {
        assertEquals(CoinEconomy.DEX_DAILY_SOLVED, CoinEconomy.forEvent(XPEvent.DexDaily(xp = 30), 0).coins)
        assertEquals(CoinEconomy.DEX_DAILY_SOLVED, CoinEconomy.forEvent(XPEvent.DexDaily(xp = 15), 0).coins)
        assertEquals(CoinEconomy.DEX_DAILY_TRIED, CoinEconomy.forEvent(XPEvent.DexDaily(xp = 5), 0).coins)
    }

    @Test
    fun `daily ceiling matches the sum of every daily source`() {
        // login 5 + first game 10 + exploration 5 + fact 5 + dex 10 + 5 completions x 4
        assertEquals(55, CoinEconomy.DAILY_MAX)
    }

    @Test
    fun `streak milestones pay once at the listed days only`() {
        assertEquals(15, CoinEconomy.streakMilestone(3))
        assertEquals(40, CoinEconomy.streakMilestone(7))
        assertEquals(500, CoinEconomy.streakMilestone(100))
        assertEquals(300, CoinEconomy.streakMilestone(150))
        assertEquals(0, CoinEconomy.streakMilestone(1))
        assertEquals(0, CoinEconomy.streakMilestone(8))
        assertEquals(0, CoinEconomy.streakMilestone(120))
    }

    @Test
    fun `no missed day needs no freeze`() {
        val guard = CoinEconomy.guardStreak(daysAway = 1, freezes = 2)
        assertTrue(guard.kept)
        assertEquals(0, guard.freezesUsed)
    }

    @Test
    fun `one freeze covers exactly one missed day`() {
        val guard = CoinEconomy.guardStreak(daysAway = 2, freezes = 1)
        assertTrue(guard.kept)
        assertEquals(1, guard.freezesUsed)
    }

    @Test
    fun `freezes that can't cover the gap are not spent`() {
        val guard = CoinEconomy.guardStreak(daysAway = 4, freezes = 2) // 3 days missed
        assertFalse(guard.kept)
        assertEquals(0, guard.freezesUsed)
    }

    @Test
    fun `no freezes means a missed day resets the streak`() {
        assertFalse(CoinEconomy.guardStreak(daysAway = 2, freezes = 0).kept)
    }

    @Test
    fun `a first ever check-in is not a continued streak`() {
        assertFalse(CoinEconomy.guardStreak(daysAway = 0, freezes = 2).kept)
    }
}

class LevelRewardsTest {

    @Test
    fun `level one has no reward and early levels pay quickly`() {
        assertNull(LevelRewards.forLevel(1))
        assertEquals(15, LevelRewards.forLevel(2)!!.coins)
        assertEquals(25, LevelRewards.forLevel(3)!!.coins)
        assertEquals(35, LevelRewards.forLevel(4)!!.coins)
    }

    @Test
    fun `every fifth level up to 100 is a milestone, and level 20 delivers a frame and a title`() {
        for (level in 5..100 step 5) {
            val reward = LevelRewards.forLevel(level)!!
            assertTrue("level $level should be a milestone", reward.isMilestone)
        }
        // Cosmetics are placed on the milestones the player is promised, including level 20.
        val twenty = LevelRewards.forLevel(20)!!
        assertEquals(listOf("frame_ultra", "title_veteran"), twenty.cosmeticIds)
        assertTrue(twenty.coins >= 100)
    }

    @Test
    fun `milestones pay more than the levels around them`() {
        for (level in listOf(10, 20, 30, 40, 50)) {
            val milestone = LevelRewards.forLevel(level)!!.coins
            assertTrue(milestone > LevelRewards.forLevel(level - 1)!!.coins)
            assertTrue(milestone > LevelRewards.forLevel(level + 1)!!.coins)
        }
    }

    @Test
    fun `catching up pays each level exactly once`() {
        val rewards = LevelRewards.between(from = 0, to = 24)
        assertEquals((2..24).toList(), rewards.map { it.level })
        // Nothing left to claim once you are caught up.
        assertTrue(LevelRewards.between(from = 24, to = 24).isEmpty())
    }

    @Test
    fun `a multi-level jump pays every skipped level`() {
        assertEquals(listOf(8, 9, 10), LevelRewards.between(7, 10).map { it.level })
    }

    @Test
    fun `rewards keep coming past level 100`() {
        assertTrue(LevelRewards.forLevel(137)!!.coins > 0)
        assertTrue(LevelRewards.forLevel(150)!!.isMilestone)
    }

    @Test
    fun `the first twenty-four levels back-pay a meaningful but bounded amount`() {
        val total = LevelRewards.between(0, 24).sumOf { it.coins }
        assertTrue("back-pay was $total", total in 1000..1600)
    }
}

class CosmeticCatalogueTest {

    @Test
    fun `ids are unique`() {
        assertEquals(Cosmetics.all.size, Cosmetics.all.map { it.id }.toSet().size)
    }

    @Test
    fun `every item is either bought or earned at a level, never both or neither`() {
        Cosmetics.all.forEach {
            assertTrue("${it.id} needs a price xor a level", (it.price != null) != (it.levelUnlock != null))
        }
    }

    @Test
    fun `level track and catalogue agree`() {
        Cosmetics.all.filter { it.levelUnlock != null }.forEach { item ->
            assertTrue(
                "${item.id} not granted at level ${item.levelUnlock}",
                item.id in LevelRewards.forLevel(item.levelUnlock!!)!!.cosmeticIds
            )
        }
    }

    @Test
    fun `a veteran owns every level cosmetic they have passed, and nothing above their level`() {
        val at24 = Cosmetics.earnedByLevel(24)
        assertTrue("frame_ultra" in at24 && "backdrop_twilight" in at24 && "title_veteran" in at24)
        assertFalse("title_elite" in at24) // level 25
        assertTrue(Cosmetics.earnedByLevel(1).isEmpty())
    }

    @Test
    fun `only the few chosen themes can be bought with coins`() {
        val themes = Cosmetics.ofKind(CosmeticKind.THEME)
        assertEquals(setOf("SYLVEON", "LUCARIO", "GRENINJA"), themes.map { it.themeName }.toSet())
        assertEquals(setOf("SYLVEON"), Cosmetics.themeUnlockedBy(setOf("theme_sylveon", "frame_flame")))
        assertNotNull(Cosmetics.find("frame_mythic"))
    }

    @Test
    fun `a casual player can afford a first item within a week`() {
        // ~25 coins a day without the daily ceiling, plus the early level rewards.
        val cheapest = Cosmetics.all.mapNotNull { it.price }.min()
        assertTrue(cheapest <= 7 * 25)
    }
}
