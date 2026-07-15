package com.aditya1875.pokeverse.utils

import java.util.Calendar
import java.util.TimeZone

/**
 * The weekly leaderboard resets every Monday 00:00 IST (Asia/Kolkata) via the
 * resetWeeklyXp Cloud Function. The client must use the exact same boundary:
 * if the local profile still carries last week's weeklyXp past that boundary,
 * syncing it back to Firestore would overwrite the server reset and leak last
 * week's XP into the new week.
 */
object WeeklyReset {
    private val ZONE = TimeZone.getTimeZone("Asia/Kolkata")

    fun startOfCurrentWeekMillis(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(ZONE).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                add(Calendar.DAY_OF_MONTH, -1)
            }
        }
        return cal.timeInMillis
    }

    fun isStale(lastWeeklyReset: Long, now: Long = System.currentTimeMillis()): Boolean =
        lastWeeklyReset < startOfCurrentWeekMillis(now)
}
