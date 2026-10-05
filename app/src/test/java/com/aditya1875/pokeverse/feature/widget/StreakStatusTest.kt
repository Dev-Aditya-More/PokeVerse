package com.aditya1875.pokeverse.feature.widget

import com.aditya1875.pokeverse.feature.widget.streak.StreakStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StreakStatusTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }

    @Test
    fun `claimed today is safe`() {
        val status = StreakStatus.from("2026-10-05", 12, at(2026, 10, 5, 9))
        assertEquals(StreakStatus.SafeToday(12), status)
    }

    @Test
    fun `claimed yesterday is at risk with hours to midnight`() {
        val status = StreakStatus.from("2026-10-04", 12, at(2026, 10, 5, 9)) as StreakStatus.AtRisk
        assertEquals(15, status.hoursLeft)
        assertFalse(status.isUrgent)
    }

    @Test
    fun `last hours of the day are urgent and round up`() {
        val status = StreakStatus.from("2026-10-04", 3, at(2026, 10, 5, 23, 10)) as StreakStatus.AtRisk
        assertEquals(1, status.hoursLeft)
        assertTrue(status.isUrgent)
    }

    @Test
    fun `a missed day means no live streak`() {
        assertEquals(StreakStatus.NotStarted, StreakStatus.from("2026-10-02", 12, at(2026, 10, 5, 9)))
    }

    @Test
    fun `brand new player has no streak`() {
        assertEquals(StreakStatus.NotStarted, StreakStatus.from("", 0, at(2026, 10, 5, 9)))
    }

    @Test
    fun `yesterday is resolved across a month boundary`() {
        val status = StreakStatus.from("2026-09-30", 5, at(2026, 10, 1, 8))
        assertTrue(status is StreakStatus.AtRisk)
    }
}
