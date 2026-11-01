package com.aditya1875.pokeverse.feature.widget

import com.aditya1875.pokeverse.feature.widget.streak.DayState
import com.aditya1875.pokeverse.feature.widget.streak.DayState.Future
import com.aditya1875.pokeverse.feature.widget.streak.DayState.Missed
import com.aditya1875.pokeverse.feature.widget.streak.DayState.Played
import com.aditya1875.pokeverse.feature.widget.streak.DayState.TodayOpen
import com.aditya1875.pokeverse.feature.widget.streak.WeekStrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class WeekStripTest {

    private val zone = TimeZone.getTimeZone("Asia/Kolkata")

    /** 2026-11-04 is a Wednesday. */
    private fun wed(hour: Int = 20) = Calendar.getInstance(zone).apply {
        clear(); set(2026, Calendar.NOVEMBER, 4, hour, 0)
    }

    private fun states(last: String, streak: Int, locale: Locale = Locale.UK) =
        WeekStrip.build(last, streak, wed(), locale).map { it.state }

    @Test fun `monday-first week with today already played`() {
        // Streak of 5 ending today: Sat..Wed, so Mon-Wed of this week are played.
        assertEquals(
            listOf(Played, Played, Played, Future, Future, Future, Future),
            states("2026-11-04", 5)
        )
    }

    @Test fun `today still open after a streak through yesterday`() {
        assertEquals(
            listOf(Played, Played, TodayOpen, Future, Future, Future, Future),
            states("2026-11-03", 2)
        )
    }

    @Test fun `days before the streak started are missed`() {
        assertEquals(
            listOf(Missed, Played, TodayOpen, Future, Future, Future, Future),
            states("2026-11-03", 1)
        )
    }

    @Test fun `no streak shows only missed days and today open`() {
        assertEquals(
            listOf(Missed, Missed, TodayOpen, Future, Future, Future, Future),
            states("", 0)
        )
    }

    @Test fun `sunday-first locales start the strip on sunday`() {
        val days = WeekStrip.build("2026-11-04", 4, wed(), Locale.US)
        assertEquals(Calendar.SUNDAY, days.first().dayOfWeek)
        // Sun..Wed played (streak of 4 ending today), Thu-Sat ahead.
        assertEquals(listOf(Played, Played, Played, Played, Future, Future, Future), days.map { it.state })
    }

    @Test fun `exactly one day is today`() {
        val days = WeekStrip.build("2026-11-03", 2, wed(hour = 0), Locale.UK)
        assertEquals(1, days.count { it.isToday })
        assertTrue(days.single { it.isToday }.state == DayState.TodayOpen)
    }
}
