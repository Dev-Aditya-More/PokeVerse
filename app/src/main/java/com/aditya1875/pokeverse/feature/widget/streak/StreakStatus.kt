package com.aditya1875.pokeverse.feature.widget.streak

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * What the streak widget should say right now. Pure (the clock is passed in),
 * so the date edge cases are unit-tested rather than discovered at midnight.
 *
 * A streak day is earned by opening the app (the daily XP claim stamps
 * `lastDailyXpDate`), so "kept" means that date is today.
 */
sealed interface StreakStatus {
    /** Today's visit is banked. */
    data class SafeToday(val days: Int) : StreakStatus

    /** Streak alive from yesterday but today isn't claimed yet. */
    data class AtRisk(val days: Int, val hoursLeft: Int) : StreakStatus {
        /** Last few hours of the day — the widget switches to an urgent look. */
        val isUrgent: Boolean get() = hoursLeft <= URGENT_HOURS
    }

    /** No live streak (never started, or a day was missed). */
    data object NotStarted : StreakStatus

    companion object {
        const val URGENT_HOURS = 6

        fun from(lastDailyXpDate: String, dailyStreak: Int, now: Calendar): StreakStatus {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = now.timeZone }
            val today = format.format(now.time)
            val yesterday = format.format((now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }.time)

            return when {
                dailyStreak <= 0 || lastDailyXpDate.isBlank() -> NotStarted
                lastDailyXpDate == today -> SafeToday(dailyStreak)
                lastDailyXpDate == yesterday -> AtRisk(dailyStreak, hoursUntilMidnight(now))
                else -> NotStarted
            }
        }

        private fun hoursUntilMidnight(now: Calendar): Int {
            val midnight = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val minutes = (midnight.timeInMillis - now.timeInMillis) / 60_000
            // Round up: at 23:10 there's still "1h left", not "0h left".
            return ((minutes + 59) / 60).toInt().coerceAtLeast(1)
        }
    }
}
