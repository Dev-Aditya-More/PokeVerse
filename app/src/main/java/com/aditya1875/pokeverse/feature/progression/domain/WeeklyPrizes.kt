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

    /**
     * Monday 23 Nov 2026, 00:00 IST: the first payout (for the week of 16-22 Nov). Keep in step with
     * FIRST_PAYOUT_WEEK in functions/weeklyRewards.js.
     */
    const val FIRST_PAYOUT_MS = 1_795_372_200_000L

    /** True until the first Monday payout has happened, so the app can say when it starts. */
    fun beforeFirstPayout(nowMs: Long = System.currentTimeMillis()) = nowMs < FIRST_PAYOUT_MS

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
