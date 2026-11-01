package com.aditya1875.pokeverse.feature.widget.streak

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** One circle in the widget's "this week" strip. */
enum class DayState {
    /** A past day (or today) the streak covered. */
    Played,

    /** A past day with no visit. */
    Missed,

    /** Today, not visited yet: the circle the player should fill. */
    TodayOpen,

    /** Later this week. */
    Future,
}

data class WeekDay(val state: DayState, val isToday: Boolean, val dayOfWeek: Int)

/**
 * The current calendar week (in the player's locale order, e.g. Mon–Sun or Sun–Sat) with each
 * day's state. Pure: the clock and locale come in, so it's unit-tested.
 *
 * Only the streak's own days are known to be played: the streak is consecutive and ends at
 * [lastDailyXpDate], so day d was played iff it falls in the last [dailyStreak] days up to it.
 */
object WeekStrip {

    fun build(lastDailyXpDate: String, dailyStreak: Int, now: Calendar, locale: Locale = Locale.getDefault()): List<WeekDay> {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = now.timeZone }
        val today = format.format(now.time)

        val playedEnd = lastDailyXpDate.takeIf { it.isNotBlank() && dailyStreak > 0 }
        val playedStart = playedEnd?.let {
            val cal = (now.clone() as Calendar).apply { time = format.parse(it)!! }
            cal.add(Calendar.DAY_OF_YEAR, -(dailyStreak - 1))
            format.format(cal.time)
        }

        val firstDayOfWeek = Calendar.getInstance(now.timeZone, locale).firstDayOfWeek
        val start = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 12) // midday: immune to DST edges while stepping days
            val back = (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
            add(Calendar.DAY_OF_YEAR, -back)
        }

        return (0 until 7).map { offset ->
            val day = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val date = format.format(day.time)
            val played = playedStart != null && date >= playedStart && date <= playedEnd!!
            val state = when {
                date > today -> DayState.Future
                played -> DayState.Played
                date == today -> DayState.TodayOpen
                else -> DayState.Missed
            }
            WeekDay(state, isToday = date == today, dayOfWeek = day.get(Calendar.DAY_OF_WEEK))
        }
    }
}
