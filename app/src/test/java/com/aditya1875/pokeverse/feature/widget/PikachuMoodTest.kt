package com.aditya1875.pokeverse.feature.widget

import com.aditya1875.pokeverse.feature.widget.streak.PikachuLook
import com.aditya1875.pokeverse.feature.widget.streak.PikachuMood
import com.aditya1875.pokeverse.feature.widget.streak.StreakStatus
import com.aditya1875.pokeverse.feature.widget.streak.mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PikachuMoodTest {

    @Test
    fun `mood follows the streak status`() {
        assertEquals(PikachuMood.Happy, StreakStatus.SafeToday(4).mood())
        assertEquals(PikachuMood.Waiting, StreakStatus.AtRisk(4, hoursLeft = 12).mood())
        assertEquals(PikachuMood.Panicking, StreakStatus.AtRisk(4, hoursLeft = 2).mood())
        assertEquals(PikachuMood.Sleeping, StreakStatus.NotStarted.mood())
    }

    @Test
    fun `outfits unlock at their milestones`() {
        assertEquals(PikachuLook.Classic, PikachuLook.forStreak(0))
        assertEquals(PikachuLook.Classic, PikachuLook.forStreak(2))
        assertEquals(PikachuLook.Cap, PikachuLook.forStreak(3))
        assertEquals(PikachuLook.PopStar, PikachuLook.forStreak(13))
        assertEquals(PikachuLook.Libre, PikachuLook.forStreak(14))
        assertEquals(PikachuLook.Belle, PikachuLook.forStreak(365))
    }

    @Test
    fun `next outfit counts down, then runs out`() {
        assertEquals(PikachuLook.PopStar to 2, PikachuLook.next(5))
        assertEquals(PikachuLook.Cap to 3, PikachuLook.next(0))
        assertNull(PikachuLook.next(100))
    }
}
