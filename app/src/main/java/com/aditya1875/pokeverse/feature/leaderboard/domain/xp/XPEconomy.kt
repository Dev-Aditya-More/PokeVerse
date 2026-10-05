package com.aditya1875.pokeverse.feature.leaderboard.domain.xp

import kotlin.math.roundToInt

/**
 * The rules that turn a game's raw XP into what the player actually earns.
 * Pure Kotlin with no I/O, so every rule here is unit-tested.
 *
 * Two levers keep progression steady without punishing anyone:
 *  - **Daily rate tiers**: the first [FULL_RATE_RAW] raw game XP each day pays in full,
 *    then [HALF_RATE_RAW] at half rate, then a small trickle. Casual players never
 *    notice; marathon sessions stop inflating levels.
 *  - **Rested XP**: time away builds a pool that doubles game XP until it's used up,
 *    so coming back after a break feels rewarding instead of hopeless.
 *
 * Daily login, streak and once-a-day bonuses bypass both — they're the reason to return.
 */
object XPEconomy {
    const val FULL_RATE_RAW = 300
    const val HALF_RATE_RAW = 900
    const val TRICKLE_RATE = 0.2f

    const val RESTED_PER_DAY_AWAY = 100
    const val RESTED_MAX = 300

    data class Award(
        /** XP after the daily rate tiers. */
        val base: Int,
        /** Extra XP from the rested pool (already capped by what's left in it). */
        val restedBonus: Int,
        /** The rate that applied to the last chunk — drives the "reduced" label. */
        val rate: Float
    ) {
        val total: Int get() = base + restedBonus
    }

    /**
     * @param raw this event's raw XP
     * @param rawEarnedToday raw game XP already earned today, before this event
     * @param restedPool rested XP still available
     */
    fun award(raw: Int, rawEarnedToday: Int, restedPool: Int): Award {
        if (raw <= 0) return Award(0, 0, 1f)
        val base = scaled(raw, rawEarnedToday.coerceAtLeast(0))
        val rested = base.coerceAtMost(restedPool.coerceAtLeast(0))
        return Award(base, rested, rateAt(rawEarnedToday + raw - 1))
    }

    /** Rested XP to add when a player returns after [daysAway] days (1 = played yesterday). */
    fun restedGain(daysAway: Int, currentPool: Int): Int {
        if (daysAway < 2) return 0
        val gain = (daysAway - 1) * RESTED_PER_DAY_AWAY
        return (currentPool + gain).coerceAtMost(RESTED_MAX) - currentPool.coerceAtMost(RESTED_MAX)
    }

    fun rateAt(rawEarnedToday: Int): Float = when {
        rawEarnedToday < FULL_RATE_RAW -> 1f
        rawEarnedToday < HALF_RATE_RAW -> 0.5f
        else -> TRICKLE_RATE
    }

    /** Integrates the tiered rate across [raw] XP starting at [start], so a big award straddling a tier is split fairly. */
    private fun scaled(raw: Int, start: Int): Int {
        var earned = 0f
        var position = start
        var remaining = raw
        while (remaining > 0) {
            val tierEnd = when {
                position < FULL_RATE_RAW -> FULL_RATE_RAW
                position < HALF_RATE_RAW -> HALF_RATE_RAW
                else -> Int.MAX_VALUE
            }
            val chunk = minOf(remaining, tierEnd - position)
            earned += chunk * rateAt(position)
            position += chunk
            remaining -= chunk
        }
        // Never round a real effort down to nothing.
        return earned.roundToInt().coerceAtLeast(1)
    }
}
