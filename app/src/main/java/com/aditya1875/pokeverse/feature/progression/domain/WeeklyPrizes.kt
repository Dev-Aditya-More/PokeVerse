package com.aditya1875.pokeverse.feature.progression.domain

/**
 * What the weekly leaderboard pays, for the screens that explain it. The payout itself is made by the
 * `resetWeeklyXp` Cloud Function from functions/weeklyRewards.js, so keep these in step with that file:
 * this object only describes the prizes, it never pays anything.
 */
object WeeklyPrizes {
    const val FIRST = 500
    const val SECOND = 300
    const val THIRD = 200
    const val TOP_10 = 100
    const val PLAYED = 50

    /** Weekly XP needed to count as having played the week. */
    const val MIN_ACTIVE_XP = 50

    /** The tier a finishing [rank] falls in, for choosing the wording of the result popup. */
    fun tierOf(rank: Int): Tier = when {
        rank == 1 -> Tier.FIRST
        rank <= 3 -> Tier.PODIUM
        rank <= 10 -> Tier.TOP_10
        else -> Tier.PLAYED
    }

    enum class Tier { FIRST, PODIUM, TOP_10, PLAYED }
}
