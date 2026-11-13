package com.aditya1875.pokeverse.feature.pokemon.profile

import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile
import com.aditya1875.pokeverse.feature.pokemon.profile.domain.progressMergedWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileMergeTest {

    private val cloud = UserProfile(
        uid = "u", username = "Misty", isGuest = false, totalXp = 5_000, gamesPlayed = 120,
        bestChaseScore = 900, bestQuizScore = 40, lastDailyXpDate = "2026-11-12", dailyStreak = 9,
        lastWeeklyReset = 200L, weeklyXp = 300, restedXp = 50, feedbackThanksClaimed = true,
    )

    @Test fun `a phone that is behind never lowers the account`() {
        val stale = UserProfile(uid = "u", username = "Misty", isGuest = false, totalXp = 100, bestChaseScore = 10)
        val m = stale.progressMergedWith(cloud)
        assertEquals(5_000, m.totalXp)
        assertEquals(120, m.gamesPlayed)
        assertEquals(900, m.bestChaseScore)
        assertEquals("2026-11-12", m.lastDailyXpDate)
        assertEquals(9, m.dailyStreak)
        assertTrue(m.feedbackThanksClaimed)
        assertEquals(50, m.restedXp) // spendable values come from the copy that is ahead
    }

    @Test fun `a best score set on the lower-XP side survives`() {
        val other = cloud.copy(totalXp = 4_000, bestQuizScore = 75)
        val m = cloud.progressMergedWith(other)
        assertEquals(5_000, m.totalXp)
        assertEquals(75, m.bestQuizScore)
    }

    @Test fun `level follows the merged XP`() {
        val m = UserProfile(totalXp = 0).progressMergedWith(cloud)
        val expected = UserProfile(totalXp = 0).progressMergedWith(UserProfile(totalXp = 5_000)).level
        assertEquals(expected, m.level)
        assertTrue(m.level > 1)
    }

    @Test fun `weekly XP takes the newer week, or the larger tally in the same week`() {
        assertEquals(300, cloud.copy(lastWeeklyReset = 100L, weeklyXp = 999).progressMergedWith(cloud).weeklyXp)
        assertEquals(450, cloud.copy(weeklyXp = 450).progressMergedWith(cloud).weeklyXp)
    }

    @Test fun `a placeholder name takes the account's, a real one is kept`() {
        assertEquals("Misty", UserProfile(username = "Trainer").progressMergedWith(cloud).username)
        assertEquals("Brock", UserProfile(username = "Brock").progressMergedWith(cloud).username)
    }

    @Test fun `a cleared bio stays cleared`() {
        assertEquals("", cloud.copy(bio = "").progressMergedWith(cloud.copy(bio = "old")).bio)
    }
}
