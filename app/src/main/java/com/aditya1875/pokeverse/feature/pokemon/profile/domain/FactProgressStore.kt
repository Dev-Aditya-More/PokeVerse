package com.aditya1875.pokeverse.feature.pokemon.profile.domain

import android.content.Context
import java.util.Calendar
import java.util.TimeZone

/**
 * Local progress for the Daily Fact card: when it was last revealed, the reveal
 * streak, how the user reacted, and whether today's XP was already paid.
 */
class FactProgressStore(context: Context) {

    private val prefs = context.getSharedPreferences("daily_fact", Context.MODE_PRIVATE)

    /** Local calendar day number (java.time needs API 26; minSdk is 25). */
    private fun today(): Long {
        val now = Calendar.getInstance()
        val offset = TimeZone.getDefault().getOffset(now.timeInMillis)
        return (now.timeInMillis + offset) / 86_400_000L
    }

    val revealedToday: Boolean get() = prefs.getLong(KEY_LAST_REVEAL, -1L) == today()
    val reactedToday: Boolean get() = prefs.getLong(KEY_LAST_REACT, -1L) == today()
    val streak: Int get() {
        val last = prefs.getLong(KEY_LAST_REVEAL, -1L)
        // A streak survives until the end of the day after the last reveal.
        return if (last >= today() - 1) prefs.getInt(KEY_STREAK, 0) else 0
    }
    val learned: Int get() = prefs.getInt(KEY_LEARNED, 0)
    val knew: Int get() = prefs.getInt(KEY_KNEW, 0)

    /** Records today's reveal; returns true if XP for today is still unclaimed (and claims it). */
    fun markRevealed(): Boolean {
        val day = today()
        val last = prefs.getLong(KEY_LAST_REVEAL, -1L)
        val streak = when (last) {
            day -> prefs.getInt(KEY_STREAK, 1)
            day - 1 -> prefs.getInt(KEY_STREAK, 0) + 1
            else -> 1
        }
        val payXp = prefs.getLong(KEY_XP_DAY, -1L) != day
        prefs.edit()
            .putLong(KEY_LAST_REVEAL, day)
            .putInt(KEY_STREAK, streak)
            .apply { if (payXp) putLong(KEY_XP_DAY, day) }
            .apply()
        return payXp
    }

    fun react(newToMe: Boolean) {
        if (reactedToday) return
        prefs.edit()
            .putLong(KEY_LAST_REACT, today())
            .putInt(if (newToMe) KEY_LEARNED else KEY_KNEW, (if (newToMe) learned else knew) + 1)
            .apply()
    }

    private companion object {
        const val KEY_LAST_REVEAL = "last_reveal_day"
        const val KEY_LAST_REACT = "last_react_day"
        const val KEY_STREAK = "streak"
        const val KEY_LEARNED = "learned"
        const val KEY_KNEW = "knew"
        const val KEY_XP_DAY = "xp_day"
    }
}
